import { DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
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

  percent(value: number, max: number): number {
    return Math.max(0, Math.min(100, (value / max) * 100));
  }
}
