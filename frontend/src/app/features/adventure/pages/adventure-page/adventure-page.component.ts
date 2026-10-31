import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { GameApiService } from '../../../../core/api/game-api.service';
import { CreateSessionRequest, DiceRoll, GameSession, SessionSummary, Setting } from '../../../../core/models/game.models';
import { ActionComposerComponent } from '../../components/action-composer/action-composer.component';
import { CharacterSetupComponent } from '../../components/character-setup/character-setup.component';
import { DiceRollComponent } from '../../components/dice-roll/dice-roll.component';
import { SessionDockComponent } from '../../components/session-dock/session-dock.component';
import { SessionListComponent } from '../../components/session-list/session-list.component';
import { SettingsMenuComponent } from '../../components/settings-menu/settings-menu.component';
import { StoryTimelineComponent } from '../../components/story-timeline/story-timeline.component';

type View = 'play' | 'setup' | 'sessions';

const LAST_SESSION_KEY = 'gpt-dnd.lastSession';

@Component({
  selector: 'app-adventure-page',
  imports: [
    ActionComposerComponent,
    CharacterSetupComponent,
    DiceRollComponent,
    SessionDockComponent,
    SessionListComponent,
    SettingsMenuComponent,
    StoryTimelineComponent,
  ],
  templateUrl: './adventure-page.component.html',
  styleUrl: './adventure-page.component.scss',
})
export class AdventurePageComponent implements OnInit {
  private readonly api = inject(GameApiService);

  protected readonly settings = signal<Setting[]>([]);
  protected readonly session = signal<GameSession | null>(null);
  protected readonly sessions = signal<SessionSummary[]>([]);
  protected readonly sessionsLoading = signal(false);
  protected readonly view = signal<View>('setup');
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly dice = signal<DiceRoll | null>(null);
  private afterDice: (() => void) | null = null;
  private readonly setupLanguage = signal<'de' | 'en'>('de');
  protected readonly language = computed(() => this.session()?.language ?? this.setupLanguage());

  ngOnInit(): void {
    this.api.settings().subscribe({
      next: (settings) => this.settings.set(settings),
      error: (error: HttpErrorResponse) => {
        const detail = error.status ? ` (HTTP ${error.status})` : '';
        this.error.set(
          `API request failed${detail}. Run "npm run dev:frontend" and open http://localhost:4200/.`,
        );
      },
    });
    this.resume();
  }

  /** Continues the last played game, otherwise shows saved games or the setup when there are none. */
  private resume(): void {
    const lastId = readLastSession();
    if (!lastId) {
      this.showSessions(true);
      return;
    }
    this.api.getSession(lastId).subscribe({
      next: (session) => this.play(session),
      error: () => {
        writeLastSession(null);
        this.showSessions(true);
      },
    });
  }

  showSessions(fallbackToSetup = false): void {
    this.view.set('sessions');
    this.sessionsLoading.set(true);
    this.api.listSessions().subscribe({
      next: (sessions) => {
        this.sessions.set(sessions);
        this.sessionsLoading.set(false);
        if (fallbackToSetup && sessions.length === 0) {
          this.view.set('setup');
        }
      },
      error: (error: HttpErrorResponse) => {
        this.sessionsLoading.set(false);
        this.error.set(this.message(error, 'Could not load saved games.'));
      },
    });
  }

  openSession(id: string): void {
    if (id === this.session()?.id) {
      this.view.set('play');
      return;
    }
    this.busy.set(true);
    this.api.getSession(id).subscribe({
      next: (session) => {
        this.play(session);
        this.busy.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(this.message(error, 'Could not load game.'));
        this.busy.set(false);
      },
    });
  }

  deleteSession(id: string): void {
    this.api.deleteSession(id).subscribe({
      next: () => {
        this.sessions.update((sessions) => sessions.filter((session) => session.id !== id));
        if (this.session()?.id === id) {
          this.session.set(null);
        }
        if (readLastSession() === id) {
          writeLastSession(null);
        }
      },
      error: (error: HttpErrorResponse) => this.error.set(this.message(error, 'Could not delete game.')),
    });
  }

  newGame(): void {
    this.view.set('setup');
    this.session.set(null);
    this.error.set(null);
  }

  create(request: CreateSessionRequest): void {
    this.busy.set(true);
    this.api.createSession({ ...request, language: this.language() }).subscribe({
      next: (session) => {
        this.revealRoll(session.nodes.at(-1)?.roll ?? null, () => {
          this.play(session);
          this.busy.set(false);
        });
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(this.message(error, 'Could not create session.'));
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
    this.error.set(null);
    this.api.sendAction(active.id, text).subscribe({
      next: (session) => {
        const added = session.nodes.length > active.nodes.length;
        this.revealRoll(added ? (session.nodes.at(-1)?.roll ?? null) : null, () => {
          this.session.set(session);
          this.busy.set(false);
        });
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(this.message(error, 'Action failed.'));
        this.busy.set(false);
      },
    });
  }

  toggleImages(): void {
    const active = this.session();
    if (!active) {
      return;
    }
    this.api.updatePreferences(active.id, { images: !active.images }).subscribe({
      next: (session) => this.session.set(session),
      error: (error: HttpErrorResponse) => this.error.set(this.message(error, 'Could not change images.')),
    });
  }

  setLanguage(language: 'de' | 'en'): void {
    const active = this.session();
    if (!active) {
      this.setupLanguage.set(language);
      return;
    }
    this.api.updatePreferences(active.id, { language }).subscribe({
      next: (session) => this.session.set(session),
      error: (error: HttpErrorResponse) => this.error.set(this.message(error, 'Could not change language.')),
    });
  }

  finishDice(): void {
    const next = this.afterDice;
    this.afterDice = null;
    this.dice.set(null);
    next?.();
  }

  /** Holds the result back until the die has landed, so the story cannot spoil the roll. */
  private revealRoll(roll: DiceRoll | null, next: () => void): void {
    if (!roll) {
      next();
      return;
    }
    this.afterDice = next;
    this.dice.set(roll);
  }

  private play(session: GameSession): void {
    this.session.set(session);
    this.view.set('play');
    this.error.set(null);
    writeLastSession(session.id);
  }

  private message(error: HttpErrorResponse, fallback: string): string {
    const detail = (error.error as { error?: string } | null)?.error;
    return detail ? `${fallback} ${detail}` : fallback;
  }
}

function readLastSession(): string | null {
  try {
    return localStorage.getItem(LAST_SESSION_KEY);
  } catch {
    return null;
  }
}

function writeLastSession(id: string | null): void {
  try {
    if (id) {
      localStorage.setItem(LAST_SESSION_KEY, id);
    } else {
      localStorage.removeItem(LAST_SESSION_KEY);
    }
  } catch {
    // Storage unavailable (private mode); resuming is a convenience only.
  }
}
