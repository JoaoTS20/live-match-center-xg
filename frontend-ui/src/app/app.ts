import { Component, signal, inject } from '@angular/core';
import { MatchEventRequest, PitchCoordinates } from './models/match-event.model';
import { MatchEventService } from './services/match-event.service';
import { PitchComponent } from './components/pitch/pitch';
import { EventControlsComponent } from './components/event-controls/event-controls';

@Component({
  imports: [PitchComponent, EventControlsComponent], // <app-pitch> & <app-event-controls> available
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  private readonly eventService = inject(MatchEventService);
  
  coordinates = signal<PitchCoordinates>({ x: 91.5, y: 52.0 });
  sending = signal(false);
  statusMessage = signal('');

  onCoordinatesChange(coords: PitchCoordinates): void {
    this.coordinates.set(coords);
  }

  onEventSubmit(payload: MatchEventRequest): void {
    this.sending.set(true);
    this.statusMessage.set('');

    // Calling .subscribe() triggers the lazy HTTP POST Observable
    this.eventService.sendEvent(payload).subscribe({
      next: () => {
        this.sending.set(false);
        this.statusMessage.set(
          `Sent ${payload.eventType} by ${payload.playerName} at (${payload.x}, ${payload.y})`
        );
      },
      error: (err) => {
        this.sending.set(false);
        this.statusMessage.set(
          `Error ${err.status}: ${err.error?.detail ?? 'Could not reach match-service'}`
        );
      },
    });
  }
}
