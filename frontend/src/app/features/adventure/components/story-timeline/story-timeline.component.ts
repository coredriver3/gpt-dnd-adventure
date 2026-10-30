import { Component, ElementRef, Input, OnChanges, SimpleChanges, afterNextRender, inject, Injector } from '@angular/core';
import { StoryNode } from '../../../../core/models/game.models';

@Component({
  selector: 'app-story-timeline',
  templateUrl: './story-timeline.component.html',
  styleUrl: './story-timeline.component.scss',
})
export class StoryTimelineComponent implements OnChanges {
  @Input() nodes: StoryNode[] = [];
  @Input() images = true;

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['nodes']) {
      // Keep the newest story point in view once it has been rendered.
      afterNextRender(() => this.host.nativeElement.scrollTo({ top: this.host.nativeElement.scrollHeight, behavior: 'smooth' }), {
        injector: this.injector,
      });
    }
  }
}
