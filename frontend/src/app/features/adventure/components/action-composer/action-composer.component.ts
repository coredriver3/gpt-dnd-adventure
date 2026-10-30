import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-action-composer',
  imports: [FormsModule],
  templateUrl: './action-composer.component.html',
  styleUrl: './action-composer.component.scss',
})
export class ActionComposerComponent {
  @Input() disabled = false;
  @Input() busy = false;
  @Input() language: 'de' | 'en' = 'de';
  @Input() suggestions: string[] = [];
  @Output() sendAction = new EventEmitter<string>();

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

  placeholder(): string {
    if (this.busy) {
      return this.language === 'de' ? 'Der Erzähler denkt nach ...' : 'The narrator is thinking ...';
    }
    return this.language === 'de'
      ? 'Was tust du? Oder [inventory], [status], [roll d20+2] ...'
      : 'What do you do? Or [inventory], [status], [roll d20+2] ...';
  }
}
