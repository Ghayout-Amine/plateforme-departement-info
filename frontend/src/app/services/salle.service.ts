import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RoomType, Salle, SalleRequest } from '../models/salle.model';

// PGDI2026-44 (US21) : appels à l'API /api/salles (le jeton JWT est ajouté par l'intercepteur)
@Injectable({
  providedIn: 'root'
})
export class SalleService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/salles';

  getAll(filters: { type?: RoomType; active?: boolean } = {}): Observable<Salle[]> {
    let params = new HttpParams();
    if (filters.type) params = params.set('type', filters.type);
    if (filters.active !== undefined) params = params.set('active', String(filters.active));
    return this.http.get<Salle[]>(this.apiUrl, { params });
  }

  getById(id: number): Observable<Salle> {
    return this.http.get<Salle>(`${this.apiUrl}/${id}`);
  }

  create(data: SalleRequest): Observable<Salle> {
    return this.http.post<Salle>(this.apiUrl, data);
  }

  update(id: number, data: SalleRequest): Observable<Salle> {
    return this.http.put<Salle>(`${this.apiUrl}/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
