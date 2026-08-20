import { Component, OnInit, inject, signal } from '@angular/core';
import { GameApiService } from '../../../../core/api/game-api.service';
import { CreateSessionRequest, GameSession, Setting } from '../../../../core/models/game.models';
import { ActionComposerComponent } from '../../components/action-composer/action-composer.component';
import { CharacterSetupComponent } from '../../components/character-setup/character-setup.component';
import { SessionDockComponent } from '../../components/session-dock/session-dock.component';
import { StoryTimelineComponent } from '../../components/story-timeline/story-timeline.component';

@Component({
  selector: 'app-adventure-page',
  imports: [ActionComposerComponent, CharacterSetupComponent, SessionDockComponent, StoryTimelineComponent],
  templateUrl: './adventure-page.component.html',
  styleUrl: './adventure-page.component.scss',
})
export class AdventurePageComponent implements OnInit {
  private readonly api = inject(GameApiService);

  protected readonly settings = signal<Setting[]>([]);
  protected readonly session = signal<GameSession | null>(null);
  protected readonly setupOpen = signal(true);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.api.settings().subscribe({
      next: (settings) => this.settings.set(settings),
      error: () => this.error.set('Backend not reachable. Start Spring Boot on port 8080.'),
    });
  }

  newGame(): void {
    this.setupOpen.set(true);
    this.session.set(null);
    this.error.set(null);
  }

  create(request: CreateSessionRequest): void {
    this.busy.set(true);
    this.api.createSession(request).subscribe({
      next: (session) => {
        this.session.set(session);
        this.setupOpen.set(false);
        this.error.set(null);
        this.busy.set(false);
      },
      error: () => {
        this.error.set('Could not create session.');
        this.busy.set(false);
      },
    });
  }

  sendAction(text: string): void {
    const active = this.session();
    if (!active) {
      return;
    }
    this.busy.set(true);
    this.api.sendAction(active.id, text).subscribe({
      next: (session) => {
        this.session.set(session);
        this.busy.set(false);
      },
      error: () => {
        this.error.set('Action failed.');
        this.busy.set(false);
      },
    });
  }

  toggleImages(): void {
    const active = this.session();
    if (!active) {
      return;
    }
    this.api.updatePreferences(active.id, { images: !active.images }).subscribe((session) => this.session.set(session));
  }

  toggleLanguage(): void {
    const active = this.session();
    if (!active) {
      return;
    }
    this.api
      .updatePreferences(active.id, { language: active.language === 'de' ? 'en' : 'de' })
      .subscribe((session) => this.session.set(session));
  }
}
