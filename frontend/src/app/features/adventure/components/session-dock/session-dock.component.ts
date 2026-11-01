import { Component, Input, inject } from '@angular/core';
import { I18nService } from '../../../../core/i18n/i18n.service';
import { GameSession } from '../../../../core/models/game.models';

@Component({
  selector: 'app-session-dock',
  templateUrl: './session-dock.component.html',
  styleUrl: './session-dock.component.scss',
})
export class SessionDockComponent {
  @Input() session: GameSession | null = null;

  protected readonly i18n = inject(I18nService);

  percent(value: number, max: number): number {
    return Math.max(0, Math.min(100, (value / max) * 100));
  }
}
