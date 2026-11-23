import { Component, input } from '@angular/core';
import { MatchCommentary } from '../../models/match-event.model';

@Component({
  selector: 'app-commentary-feed',
  templateUrl: './commentary-feed.html',
  styleUrl: './commentary-feed.css',
})
export class CommentaryFeedComponent {
  items = input.required<MatchCommentary[]>();
}
