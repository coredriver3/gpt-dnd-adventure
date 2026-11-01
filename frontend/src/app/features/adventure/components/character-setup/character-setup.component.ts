import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../../../core/i18n/i18n.service';
import { CreateSessionRequest, Setting } from '../../../../core/models/game.models';

@Component({
  selector: 'app-character-setup',
  imports: [FormsModule],
  templateUrl: './character-setup.component.html',
  styleUrl: './character-setup.component.scss',
})
export class CharacterSetupComponent {
  @Input({ required: true }) settings: Setting[] = [];
  @Input() busy = false;
  @Output() createSession = new EventEmitter<CreateSessionRequest>();

  protected readonly i18n = inject(I18nService);
  protected model: CreateSessionRequest = {
    name: '',
    concept: '',
    setting: '',
  };

  protected get complete(): boolean {
    return !!(this.model.name.trim() && this.model.concept.trim() && this.model.setting.trim());
  }

  submit(): void {
    if (this.busy || !this.complete) {
      return;
    }
    this.createSession.emit({
      name: this.model.name.trim(),
      concept: this.model.concept.trim(),
      setting: this.model.setting.trim(),
    });
  }
}
