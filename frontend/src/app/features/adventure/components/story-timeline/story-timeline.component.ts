import { Component, Input } from '@angular/core';
import { StoryNode } from '../../../../core/models/game.models';

@Component({
  selector: 'app-story-timeline',
  templateUrl: './story-timeline.component.html',
  styleUrl: './story-timeline.component.scss',
})
export class StoryTimelineComponent {
  @Input() nodes: StoryNode[] = [];
  @Input() images = true;
}
