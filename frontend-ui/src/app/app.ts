import { Component, signal, inject, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { MatchEventRequest, PitchCoordinates, MatchCommentary } from './models/match-event.model';
import { MatchEventService } from './services/match-event.service';
import { VoiceService } from './services/voice.service';
import { PitchComponent } from './components/pitch/pitch';
import { EventControlsComponent } from './components/event-controls/event-controls';
import { CommentaryFeedComponent } from './components/commentary-feed/commentary-feed';

@Component({
  imports: [PitchComponent, EventControlsComponent, CommentaryFeedComponent], // <app-pitch> & <app-event-controls> <app-commentary-feed> available
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  private readonly eventService = inject(MatchEventService);
  protected readonly voiceService = inject(VoiceService);

  coordinates = signal<PitchCoordinates>({ x: 91.5, y: 52.0 });
  sending = signal(false);
  statusMessage = signal('');
  commentaryFeed = signal<MatchCommentary[]>([]);

  private activeMatchId = 'sporting-vs-braga-2026';
  private streamSub?: Subscription;

  ngOnInit(): void {
    this.connectToStream(this.activeMatchId);
  }

  ngOnDestroy(): void {
    this.streamSub?.unsubscribe();
  }

  onCoordinatesChange(coords: PitchCoordinates): void {
    this.coordinates.set(coords);
  }

  onEventSubmit(payload: MatchEventRequest): void {

    // If the user changed Match ID in the form, switch the SSE stream to that match
    if (payload.matchId !== this.activeMatchId) {
      this.activeMatchId = payload.matchId;
      this.commentaryFeed.set([]);
      this.connectToStream(this.activeMatchId);
    }

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

  testVoice(): void {
    this.voiceService.speak('Live match center voice narrator is online and ready.');
  }

  private connectToStream(matchId: string): void {
    this.streamSub?.unsubscribe();
    this.streamSub = this.eventService.streamCommentary(matchId).subscribe((item) => {
      // Prepend newest commentary to the top and read it aloud
      this.commentaryFeed.update((list) => [item, ...list]);
      this.voiceService.speak(item.commentary);
    });
  }

}
