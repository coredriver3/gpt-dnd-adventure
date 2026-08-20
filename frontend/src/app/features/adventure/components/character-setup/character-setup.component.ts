import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
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

  protected model: CreateSessionRequest = {
    name: 'Mara',
    concept: 'runaway cartographer',
    setting: 'dark-fantasy',
  };

  submit(): void {
    this.createSession.emit({
      name: this.model.name.trim() || 'Mara',
      concept: this.model.concept.trim() || 'traveller',
      setting: this.model.setting,
    });
  }
}
