import { KeyValuePipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { GameSession } from '../../../../core/models/game.models';

@Component({
  selector: 'app-session-dock',
  imports: [KeyValuePipe],
  templateUrl: './session-dock.component.html',
  styleUrl: './session-dock.component.scss',
})
export class SessionDockComponent {
  @Input() session: GameSession | null = null;
  @Output() toggleImages = new EventEmitter<void>();
  @Output() toggleLanguage = new EventEmitter<void>();

  protected readonly fallbackResources: Record<string, number> = {
    coin: 12,
    rations: 3,
    torch: 2,
  };

  percent(value: number, max: number): number {
    return Math.max(0, Math.min(100, (value / max) * 100));
  }
}
