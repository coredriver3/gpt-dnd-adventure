import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnDestroy,
  Output,
  ViewChild,
  signal,
} from '@angular/core';
import { DiceRoll } from '../../../../core/models/game.models';
import { Mat3, Vec3, apply, buildD20, cssMatrix, dot, multiply, normalize, rotation } from './d20-geometry';

const EDGE = 96;
const FACE_WIDTH = EDGE * 1.02; // Slightly oversized so no gaps show between faces.
const FACE_HEIGHT = (FACE_WIDTH * Math.sqrt(3)) / 2;
const ROLL_MS = 2000;
const HOLD_MS = 1600;
const LIGHT: Vec3 = normalize([-0.4, -0.6, 0.7]);
const FACES = buildD20(FACE_WIDTH, FACE_HEIGHT, EDGE);

@Component({
  selector: 'app-dice-roll',
  templateUrl: './dice-roll.component.html',
  styleUrl: './dice-roll.component.scss',
})
export class DiceRollComponent implements AfterViewInit, OnDestroy {
  @Input({ required: true }) roll!: DiceRoll;
  @Output() finished = new EventEmitter<void>();

  @ViewChild('die') private die!: ElementRef<HTMLElement>;
  @ViewChild('bouncer') private bouncer!: ElementRef<HTMLElement>;

  protected readonly faces = FACES;
  protected readonly faceWidth = FACE_WIDTH;
  protected readonly faceHeight = FACE_HEIGHT;
  protected readonly landed = signal(false);

  private frame = 0;
  private holdTimer: ReturnType<typeof setTimeout> | undefined;
  private target!: Mat3;
  private done = false;

  protected get total(): number {
    return this.roll.natural + this.roll.modifier;
  }

  protected get signedModifier(): string {
    return this.roll.modifier > 0 ? `+${this.roll.modifier}` : `${this.roll.modifier}`;
  }

  ngAfterViewInit(): void {
    this.target = FACES.find((face) => face.value === this.roll.natural)!.landing;
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      this.land();
      return;
    }
    const axis = normalize([Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5]);
    const turns = (5 + Math.random() * 2) * 2 * Math.PI;
    const start = performance.now();
    const step = (now: number) => {
      const t = Math.min(1, (now - start) / ROLL_MS);
      const remaining = Math.pow(1 - t, 3);
      this.render(multiply(rotation(axis, turns * remaining), this.target));
      const height = 240 * Math.pow(1 - t, 2) * Math.abs(Math.cos(t * Math.PI * 3.5));
      this.bouncer.nativeElement.style.transform = `translateY(${-height}px)`;
      if (t < 1) {
        this.frame = requestAnimationFrame(step);
      } else {
        this.land();
      }
    };
    this.frame = requestAnimationFrame(step);
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.frame);
    clearTimeout(this.holdTimer);
  }

  /** Skips the animation while rolling, continues to the story once landed. */
  @HostListener('click')
  @HostListener('document:keydown.escape')
  @HostListener('document:keydown.enter')
  advance(): void {
    if (this.landed()) {
      this.finish();
    } else if (this.target) {
      this.land();
    }
  }

  private land(): void {
    cancelAnimationFrame(this.frame);
    this.render(this.target);
    this.bouncer.nativeElement.style.transform = 'none';
    this.landed.set(true);
    this.holdTimer = setTimeout(() => this.finish(), HOLD_MS);
  }

  private finish(): void {
    if (!this.done) {
      this.done = true;
      clearTimeout(this.holdTimer);
      this.finished.emit();
    }
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
