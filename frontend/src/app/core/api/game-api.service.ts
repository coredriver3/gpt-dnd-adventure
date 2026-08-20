import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateSessionRequest, GameSession, Setting } from '../models/game.models';

@Injectable({ providedIn: 'root' })
export class GameApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api';

  settings(): Observable<Setting[]> {
    return this.http.get<Setting[]>(`${this.baseUrl}/settings`);
  }

  createSession(request: CreateSessionRequest): Observable<GameSession> {
    return this.http.post<GameSession>(`${this.baseUrl}/sessions`, request);
  }

  sendAction(sessionId: string, text: string): Observable<GameSession> {
    return this.http.post<GameSession>(`${this.baseUrl}/sessions/${sessionId}/actions`, { text });
  }

  updatePreferences(
    sessionId: string,
    preferences: Partial<Pick<GameSession, 'language' | 'images'>>,
  ): Observable<GameSession> {
    return this.http.patch<GameSession>(`${this.baseUrl}/sessions/${sessionId}/preferences`, preferences);
  }
}
