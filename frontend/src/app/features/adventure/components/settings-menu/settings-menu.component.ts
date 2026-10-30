import { Component, ElementRef, EventEmitter, HostListener, Input, Output, inject, signal } from '@angular/core';

@Component({
  selector: 'app-settings-menu',
  templateUrl: './settings-menu.component.html',
  styleUrl: './settings-menu.component.scss',
})
export class SettingsMenuComponent {
  @Input() images = true;
  @Input() imagesAvailable = false;
  @Input() language: 'de' | 'en' = 'de';
  @Output() toggleImages = new EventEmitter<void>();
  @Output() setLanguage = new EventEmitter<'de' | 'en'>();

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
    if (language !== this.language) {
      this.setLanguage.emit(language);
    }
  }
}
