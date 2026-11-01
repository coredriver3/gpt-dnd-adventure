import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../../../core/i18n/i18n.service';

@Component({
  selector: 'app-action-composer',
  imports: [FormsModule],
  templateUrl: './action-composer.component.html',
  styleUrl: './action-composer.component.scss',
})
export class ActionComposerComponent {
  @Input() disabled = false;
  @Input() busy = false;
  @Input() suggestions: string[] = [];
  @Output() sendAction = new EventEmitter<string>();

  protected readonly i18n = inject(I18nService);
  protected text = '';

  submit(): void {
    const value = this.text.trim();
    if (!value) {
      return;
    }
    this.sendAction.emit(value);
    this.text = '';
  }

  choose(suggestion: string): void {
    this.sendAction.emit(suggestion);
  }
}
