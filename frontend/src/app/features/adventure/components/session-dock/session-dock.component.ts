import { KeyValuePipe } from '@angular/common';
import { Component, Input } from '@angular/core';
import { GameSession } from '../../../../core/models/game.models';

@Component({
  selector: 'app-session-dock',
  imports: [KeyValuePipe],
  templateUrl: './session-dock.component.html',
  styleUrl: './session-dock.component.scss',
})
export class SessionDockComponent {
  @Input() session: GameSession | null = null;

  percent(value: number, max: number): number {
    return Math.max(0, Math.min(100, (value / max) * 100));
  }
}
