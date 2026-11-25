import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Announcement, AnnouncementRequest, TechWatchResponse } from '../models/actualite.model';

// PGDI2026-88 / PGDI2026-89 : appels à /api/actualites
@Injectable({
  providedIn: 'root'
})
export class ActualiteService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/actualites';

  getAnnouncements(): Observable<Announcement[]> {
    return this.http.get<Announcement[]>(`${this.apiUrl}/departement`);
  }

  createAnnouncement(data: AnnouncementRequest): Observable<Announcement> {
    return this.http.post<Announcement>(`${this.apiUrl}/departement`, data);
  }

  updateAnnouncement(id: number, data: AnnouncementRequest): Observable<Announcement> {
    return this.http.put<Announcement>(`${this.apiUrl}/departement/${id}`, data);
  }

  togglePin(id: number): Observable<Announcement> {
    return this.http.patch<Announcement>(`${this.apiUrl}/departement/${id}/epingler`, {});
  }

  deleteAnnouncement(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/departement/${id}`);
  }

  getTechWatch(limit = 12): Observable<TechWatchResponse> {
    return this.http.get<TechWatchResponse>(`${this.apiUrl}/veille-externe`, {
      params: new HttpParams().set('limit', limit)
    });
  }
}
