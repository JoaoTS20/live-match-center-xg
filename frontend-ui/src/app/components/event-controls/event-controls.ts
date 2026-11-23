import { Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  MatchEventRequest,
  MatchEventType,
  PitchCoordinates,
} from '../../models/match-event.model';

@Component({
  selector: 'app-event-controls',
  imports: [FormsModule],         // Enables [(ngModel)] and (ngSubmit) in the HTML
  templateUrl: './event-controls.html',
  styleUrl: './event-controls.css',
})
export class EventControlsComponent {
  coordinates = input.required<PitchCoordinates>();
  sending = input(false);

  eventSubmit = output<MatchEventRequest>();

  // Default values so you can test immediately without typing every time
  matchId = 'sporting-vs-braga-2026';
  team = 'Sporting';
  playerName = 'Trincao';
  eventType: MatchEventType = 'SHOT';
  minute = 72;

  onSubmit(): void {
    this.eventSubmit.emit({
      matchId: this.matchId,
      team: this.team,
      playerName: this.playerName,
      eventType: this.eventType,
      minute: this.minute,
      x: this.coordinates().x,
      y: this.coordinates().y,
    });
  }
}