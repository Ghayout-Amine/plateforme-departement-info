import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Internship,
  InternshipRequest,
  InternshipStatus,
  InternshipValidation,
} from '../models/internship.model';

// PGDI2026-80 (US66/US21) : appels à /api/internships
@Injectable({ providedIn: 'root' })
export class InternshipService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/internships';

  /** Étudiant : déclare son stage avec la convention PDF (envoi multipart : data + file). */
  declare(data: InternshipRequest, file: File): Observable<Internship> {
    const body = new FormData();
    body.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    body.append('file', file);
    return this.http.post<Internship>(`${this.apiUrl}/me`, body);
  }

  /** Étudiant : ses propres stages. */
  getMine(): Observable<Internship[]> {
    return this.http.get<Internship[]>(`${this.apiUrl}/me`);
  }

  /** Département : tous les stages, filtre de statut facultatif. */
  getAll(status?: InternshipStatus | ''): Observable<Internship[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<Internship[]>(this.apiUrl, { params });
  }

  /** Département : valide ou refuse la convention (motif obligatoire en cas de refus). */
  validate(id: number, decision: InternshipValidation): Observable<Internship> {
    return this.http.put<Internship>(`${this.apiUrl}/${id}/validation`, decision);
  }

  /** Télécharge la convention (PDF). Passe par HttpClient pour envoyer le jeton JWT. */
  getAgreement(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/agreement`, { responseType: 'blob' });
  }
}
