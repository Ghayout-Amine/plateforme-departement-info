import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Examen, ExamenRequest } from '../models/examen.model';

// PGDI2026-75 (US49) : appels à l'API /api/examens
@Injectable({
  providedIn: 'root'
})
export class ExamenService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/examens';

  getAll(filters: { from?: string; to?: string; groupeId?: number; salleId?: number; moduleId?: number } = {}): Observable<Examen[]> {
    let params = new HttpParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') params = params.set(key, value);
    });
    return this.http.get<Examen[]>(this.apiUrl, { params });
  }

  // Mes examens (étudiant : son groupe, enseignant : ses modules)
  getMine(from?: string): Observable<Examen[]> {
    return this.http.get<Examen[]>(`${this.apiUrl}/me`, { params: from ? { from } : {} });
  }

  getById(id: number): Observable<Examen> {
    return this.http.get<Examen>(`${this.apiUrl}/${id}`);
  }

  create(data: ExamenRequest): Observable<Examen> {
    return this.http.post<Examen>(this.apiUrl, data);
  }

  update(id: number, data: ExamenRequest): Observable<Examen> {
    return this.http.put<Examen>(`${this.apiUrl}/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
