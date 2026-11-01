import { Component, ElementRef, EventEmitter, HostListener, Input, Output, inject, signal } from '@angular/core';
import { I18nService } from '../../../../core/i18n/i18n.service';

@Component({
  selector: 'app-settings-menu',
  templateUrl: './settings-menu.component.html',
  styleUrl: './settings-menu.component.scss',
})
export class SettingsMenuComponent {
  @Input() images = true;
  @Input() imagesAvailable = false;
  @Output() toggleImages = new EventEmitter<void>();
  @Output() setLanguage = new EventEmitter<'de' | 'en'>();

  protected readonly i18n = inject(I18nService);
  protected readonly open = signal(false);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  @HostListener('document:click', ['$event'])
  closeOnOutsideClick(event: MouseEvent): void {
    if (this.open() && !this.host.nativeElement.contains(event.target as Node)) {
      this.open.set(false);
    }
  }

  @HostListener('document:keydown.escape')
  closeOnEscape(): void {
    this.open.set(false);
  }

  chooseLanguage(language: 'de' | 'en'): void {
    if (language !== this.i18n.language()) {
      this.setLanguage.emit(language);
    }
  }
}
