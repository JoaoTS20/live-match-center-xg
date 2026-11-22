import { Injectable, NgZone, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
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
          const data: MatchCommentary = JSON.parse(event.data);
          subscriber.next(data);
        });
      };

      eventSource.onerror = (error) => {
        // EventSource automatically reconnects on temporary drops,
        // so we log instead of terminating the Observable with subscriber.error()
        console.warn('SSE stream waiting for backend / reconnecting...', error);
      };

      // Cleanup: closes the browser SSE connection on unsubscribe
      return () => eventSource.close();
    });
  }
}
