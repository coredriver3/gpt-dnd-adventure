import { DOCUMENT, Injectable, effect, inject, signal } from '@angular/core';
import { Language, TRANSLATIONS, TranslationKey, translateServerText } from './translations';

const STORAGE_KEY = 'gpt-dnd.language';

@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly document = inject(DOCUMENT);

  readonly language = signal<Language>(initialLanguage());

  constructor() {
    effect(() => {
      const language = this.language();
      this.document.documentElement.lang = language;
      try {
        localStorage.setItem(STORAGE_KEY, language);
      } catch {
        // Storage unavailable (private mode); the language just resets on reload.
      }
    });
  }

  t(key: TranslationKey): string {
    return TRANSLATIONS[this.language()][key];
  }

  server(text: string): string {
    return translateServerText(text, this.language());
  }
}

function initialLanguage(): Language {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored === 'de' || stored === 'en') {
      return stored;
    }
  } catch {
    // Fall through to the browser language.
  }
  return navigator.language.toLowerCase().startsWith('de') ? 'de' : 'en';
}
