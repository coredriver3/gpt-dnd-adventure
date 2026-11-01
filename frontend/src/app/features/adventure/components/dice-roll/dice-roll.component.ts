import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { I18nService } from '../../../../core/i18n/i18n.service';
import { DiceRoll } from '../../../../core/models/game.models';
import { Mat3, Vec3, apply, buildD20, cssMatrix, dot, multiply, normalize, rotation } from './d20-geometry';

const EDGE = 96;
const FACE_WIDTH = EDGE * 1.02; // Slightly oversized so no gaps show between faces.
const FACE_HEIGHT = (FACE_WIDTH * Math.sqrt(3)) / 2;
const ROLL_MS = 2000;
const HOLD_MS = 1000;
const LIGHT: Vec3 = normalize([-0.4, -0.6, 0.7]);
const FACES = buildD20(FACE_WIDTH, FACE_HEIGHT, EDGE);

/** Permanent dice window: shows the latest roll, and tumbles the die whenever a new roll comes in. */
@Component({
  selector: 'app-dice-roll',
  templateUrl: './dice-roll.component.html',
  styleUrl: './dice-roll.component.scss',
})
export class DiceRollComponent implements OnChanges, AfterViewInit, OnDestroy {
  @Input() roll: DiceRoll | null = null;
  @Input() rolling = false;
  @Output() finished = new EventEmitter<void>();

  @ViewChild('die') private die!: ElementRef<HTMLElement>;
  @ViewChild('bouncer') private bouncer!: ElementRef<HTMLElement>;

  protected readonly i18n = inject(I18nService);
  protected readonly faces = FACES;
  protected readonly faceWidth = FACE_WIDTH;
  protected readonly faceHeight = FACE_HEIGHT;
  protected readonly landed = signal(false);

  private frame = 0;
  private holdTimer: ReturnType<typeof setTimeout> | undefined;
  private viewReady = false;
  private animating = false;

  protected get total(): number {
    return this.roll ? this.roll.natural + this.roll.modifier : 0;
  }

  protected get signedModifier(): string {
    const modifier = this.roll?.modifier ?? 0;
    return modifier > 0 ? `+${modifier}` : `${modifier}`;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (this.viewReady && changes['roll']) {
      this.show();
    }
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.show();
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.frame);
    clearTimeout(this.holdTimer);
  }

  /** A click skips the tumbling. */
  @HostListener('click')
  skip(): void {
    if (this.animating) {
      this.land();
    }
  }

  private show(): void {
    cancelAnimationFrame(this.frame);
    clearTimeout(this.holdTimer);
    this.animating = false;
    if (this.rolling && this.roll) {
      this.tumble();
    } else {
      this.render(this.target());
      this.bouncer.nativeElement.style.transform = 'none';
      this.landed.set(!!this.roll);
    }
  }

  private tumble(): void {
    this.animating = true;
    this.landed.set(false);
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      this.land();
      return;
    }
    const target = this.target();
    const axis = normalize([Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5]);
    const turns = (5 + Math.random() * 2) * 2 * Math.PI;
    const start = performance.now();
    const step = (now: number) => {
      const t = Math.min(1, (now - start) / ROLL_MS);
      this.render(multiply(rotation(axis, turns * Math.pow(1 - t, 3)), target));
      const height = 46 * Math.pow(1 - t, 2) * Math.abs(Math.cos(t * Math.PI * 3.5));
      this.bouncer.nativeElement.style.transform = `translateY(${-height}px)`;
      if (t < 1) {
        this.frame = requestAnimationFrame(step);
      } else {
        this.land();
      }
    };
    this.frame = requestAnimationFrame(step);
  }

  private land(): void {
    cancelAnimationFrame(this.frame);
    this.animating = false;
    this.render(this.target());
    this.bouncer.nativeElement.style.transform = 'none';
    this.landed.set(true);
    this.holdTimer = setTimeout(() => this.finished.emit(), HOLD_MS);
  }

  private target(): Mat3 {
    const value = this.roll?.natural ?? 20;
    return FACES.find((face) => face.value === value)!.landing;
  }

  private render(orientation: Mat3): void {
    this.die.nativeElement.style.transform = cssMatrix(orientation);
    const elements = this.die.nativeElement.children;
    FACES.forEach((face, index) => {
      const light = Math.max(0, dot(apply(orientation, face.normal), LIGHT));
      (elements[index] as HTMLElement).style.backgroundColor = `hsl(150 12% ${12 + light * 30}%)`;
    });
  }
}
