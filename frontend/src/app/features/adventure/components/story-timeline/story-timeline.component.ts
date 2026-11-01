import {
  AfterViewInit,
  Component,
  ElementRef,
  Injector,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ViewChild,
  afterNextRender,
  inject,
  signal,
} from '@angular/core';
import { I18nService } from '../../../../core/i18n/i18n.service';
import { StoryNode } from '../../../../core/models/game.models';
import { toParagraphs } from './story-text';

interface MapEntry {
  node: StoryNode;
  /** Scene and story nodes are stops on the path; commands and rejections are just notes beside it. */
  stop: boolean;
  number: number;
  /** Where the card sits in the free horizontal space, 0 to 1. */
  x: number;
  /** Where the path enters the card along its top edge, 0 to 1. */
  entry: number;
  /** Where the path leaves the card along its bottom edge, 0 to 1. */
  exit: number;
  paragraphs: string[];
}

interface Segment {
  id: string;
  d: string;
}

const DRAW_MS = 900;
const GOLDEN = 0.618034;

/** The story as a hand-drawn map: curved paths lead to freely placed stops, and past stops fold up. */
@Component({
  selector: 'app-story-timeline',
  templateUrl: './story-timeline.component.html',
  styleUrl: './story-timeline.component.scss',
})
export class StoryTimelineComponent implements OnChanges, AfterViewInit, OnDestroy {
  @Input() nodes: StoryNode[] = [];
  @Input() images = true;

  @ViewChild('map') private map!: ElementRef<HTMLElement>;

  protected readonly i18n = inject(I18nService);
  protected readonly segments = signal<Segment[]>([]);
  /** The stop the path is still being drawn toward; its card stays hidden until then. */
  protected readonly pendingId = signal<string | null>(null);
  protected readonly drawingId = signal<string | null>(null);
  /** Past stops the player unfolded again. */
  protected readonly unfolded = signal<ReadonlySet<string>>(new Set());
  protected entries: MapEntry[] = [];
  protected newestId: string | null = null;
  protected currentId: string | null = null;

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);
  private observer: ResizeObserver | undefined;
  private revealTimer: ReturnType<typeof setTimeout> | undefined;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['nodes']) {
      const previous: StoryNode[] = changes['nodes'].previousValue ?? [];
      this.entries = toEntries(this.nodes);
      this.currentId = [...this.entries].reverse().find((entry) => entry.stop)?.node.id ?? null;
      this.stepForward(previous);
      afterNextRender(
        () => {
          this.measure();
          this.scrollToEnd();
        },
        { injector: this.injector },
      );
    }
  }

  ngAfterViewInit(): void {
    this.observer = new ResizeObserver(() => this.measure());
    this.observer.observe(this.map.nativeElement);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
    clearTimeout(this.revealTimer);
  }

  protected isOpen(entry: MapEntry): boolean {
    return entry.node.id === this.currentId || this.unfolded().has(entry.node.id);
  }

  protected toggle(entry: MapEntry): void {
    const next = new Set(this.unfolded());
    if (!next.delete(entry.node.id)) {
      next.add(entry.node.id);
    }
    this.unfolded.set(next);
  }

  /** Animates only a stop added in this game; loading a saved game shows the finished map at once. */
  private stepForward(previous: StoryNode[]): void {
    clearTimeout(this.revealTimer);
    this.pendingId.set(null);
    this.drawingId.set(null);
    this.newestId = null;
    const sameGame = previous.length > 0 && previous[0].id === this.nodes[0]?.id;
    if (!sameGame) {
      this.unfolded.set(new Set());
    }
    const added = this.entries.at(-1);
    if ((previous.length > 0 && !sameGame) || !added || this.nodes.length !== previous.length + 1) {
      return;
    }
    this.newestId = added.node.id;
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (added.stop && added.number > 1 && !reducedMotion) {
      this.pendingId.set(added.node.id);
      this.drawingId.set(added.node.id);
      this.revealTimer = setTimeout(() => {
        this.pendingId.set(null);
        this.scrollToEnd();
      }, DRAW_MS);
    }
  }

  private measure(): void {
    const root = this.map.nativeElement;
    const origin = root.getBoundingClientRect();
    const stops = Array.from(root.querySelectorAll<HTMLElement>('.stop')).map((element) => {
      const pin = element.querySelector('.pin')!.getBoundingClientRect();
      const card = element.querySelector('.card')!.getBoundingClientRect();
      return {
        id: element.dataset['id']!,
        x: pin.left + pin.width / 2 - origin.left,
        y: pin.top + pin.height / 2 - origin.top,
        exitX: card.left + card.width * Number(element.dataset['exit']) - origin.left,
        bottom: card.bottom - origin.top,
      };
    });
    const segments: Segment[] = [];
    stops.forEach((stop, index) => {
      if (index > 0) {
        const from = stops[index - 1];
        segments.push({ id: stop.id, d: curve(from.exitX, from.bottom, stop.x, stop.y, index, origin.width) });
      }
    });
    this.segments.set(segments);
  }

  private scrollToEnd(): void {
    const element = this.host.nativeElement;
    element.scrollTo({ top: element.scrollHeight, behavior: 'smooth' });
    // On narrow screens the page scrolls instead of this element.
    if (element.scrollHeight <= element.clientHeight && window.innerWidth <= 860) {
      this.map.nativeElement.lastElementChild?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }
}

/** One smooth arc that bows out to a different side and depth each time, so no two connections look alike. */
function curve(x0: number, y0: number, x1: number, y1: number, index: number, width: number): string {
  const reach = (y1 - y0) * 0.8;
  const bow = (fraction(index * GOLDEN) - 0.5) * Math.min(220, width * 0.3);
  const clampX = (x: number) => Math.max(10, Math.min(width - 10, x));
  return `M ${x0} ${y0} C ${clampX(x0 + bow)} ${y0 + reach}, ${clampX(x1 + bow)} ${y1 - reach}, ${x1} ${y1}`;
}

function fraction(value: number): number {
  return value - Math.floor(value);
}

function toEntries(nodes: StoryNode[]): MapEntry[] {
  let stops = 0;
  return nodes.map((node) => {
    const stop = node.type === 'scene' || node.type === 'story point';
    if (stop) {
      stops++;
    }
    return {
      node,
      stop,
      number: stops,
      x: fraction(stops * GOLDEN + 0.15),
      entry: 0.2 + fraction(stops * 0.37) * 0.6,
      exit: 0.2 + fraction(stops * 0.73 + 0.4) * 0.6,
      paragraphs: toParagraphs(node.text),
    };
  });
}
