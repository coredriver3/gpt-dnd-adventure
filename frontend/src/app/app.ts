import { Component, signal } from '@angular/core';
import { AdventurePageComponent } from './features/adventure/pages/adventure-page/adventure-page.component';

@Component({
  selector: 'app-root',
  imports: [AdventurePageComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly title = signal('GPT Dice Adventure');
}
