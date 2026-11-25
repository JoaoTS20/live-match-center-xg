import { Injectable, NgZone, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, retry } from 'rxjs';
import { MatchEventRequest, MatchCommentary } from '../models/match-event.model';

@Injectable({
  providedIn: 'root',
})
export class MatchEventService {
  // Injects Angular's HTTP client (configured in app.config.ts)
  private readonly http = inject(HttpClient);

  private readonly ngZone = inject(NgZone);


  // Relative URL: proxy.conf.json forwards /api/** to http://localhost:8080/api/**
  private readonly eventsUrl = '/api/v1/events';

  sendEvent(event: MatchEventRequest): Observable<unknown> {
    return this.http.post(this.eventsUrl, event);
  }

  streamCommentary(matchId: string): Observable<MatchCommentary> {
    return new Observable<MatchCommentary>((subscriber) => {
      const url = `/api/v1/matches/${encodeURIComponent(matchId)}/stream`;
      const eventSource = new EventSource(url);

      eventSource.onmessage = (event) => {
        this.ngZone.run(() => {
          try {
            const data: MatchCommentary = JSON.parse(event.data);
            subscriber.next(data);
          } catch (err) {
            console.warn('Skipping malformed SSE payload:', err);
          }
        });
      };

      eventSource.onerror = (err) => {
        this.ngZone.run(() => {
          if (eventSource.readyState === EventSource.CLOSED) {
            subscriber.error(err);
          } else {
            // Native EventSource automatically reconnects after transient drops or RollingUpdates
            console.warn('SSE connection interrupted; waiting for automatic reconnect...');
          }
        });
      };      

      // Cleanup: closes the browser SSE connection on unsubscribe
      return () => eventSource.close();
    }).pipe(
      retry({ delay: 3000 })
    );
  }
}
