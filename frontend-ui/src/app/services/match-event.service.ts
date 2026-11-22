import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MatchEventRequest } from '../models/match-event.model';

@Injectable({
  providedIn: 'root',
})
export class MatchEventService {
  // Injects Angular's HTTP client (configured in app.config.ts)
  private readonly http = inject(HttpClient);

  // Relative URL: proxy.conf.json forwards /api/** to http://localhost:8080/api/**
  private readonly eventsUrl = '/api/v1/events';

  sendEvent(event: MatchEventRequest): Observable<unknown> {
    return this.http.post(this.eventsUrl, event);
  }
}
