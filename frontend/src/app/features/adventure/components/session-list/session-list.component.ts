import { DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { I18nService } from '../../../../core/i18n/i18n.service';
import { SessionSummary } from '../../../../core/models/game.models';

@Component({
  selector: 'app-session-list',
  imports: [DatePipe],
  templateUrl: './session-list.component.html',
  styleUrl: './session-list.component.scss',
})
export class SessionListComponent {
  @Input() sessions: SessionSummary[] = [];
  @Input() activeId: string | null = null;
  @Input() loading = false;
  @Output() openSession = new EventEmitter<string>();
  @Output() newGame = new EventEmitter<void>();
  @Output() closeList = new EventEmitter<void>();
  @Output() deleteSession = new EventEmitter<string>();

  protected readonly i18n = inject(I18nService);
  protected readonly confirmingId = signal<string | null>(null);

  percent(value: number, max: number): number {
    return Math.max(0, Math.min(100, (value / max) * 100));
  }
}
