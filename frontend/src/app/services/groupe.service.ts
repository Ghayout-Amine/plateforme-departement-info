import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Groupe, GroupeRequest } from '../models/groupe.model';
import { Filiere } from '../models/filiere.model';

// PGDI2026-44 (US22) : appels à l'API /api/groupes (le jeton JWT est ajouté par l'intercepteur)
@Injectable({
  providedIn: 'root'
})
export class GroupeService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/groupes';

  /** Tous les groupes, ou seulement ceux d'une filière si filiereId est fourni. */
  getAll(filiereId?: number | null): Observable<Groupe[]> {
    let params = new HttpParams();
    if (filiereId) params = params.set('filiereId', String(filiereId));
    return this.http.get<Groupe[]>(this.apiUrl, { params });
  }

  getById(id: number): Observable<Groupe> {
    return this.http.get<Groupe>(`${this.apiUrl}/${id}`);
  }

  create(data: GroupeRequest): Observable<Groupe> {
    return this.http.post<Groupe>(this.apiUrl, data);
  }

  update(id: number, data: GroupeRequest): Observable<Groupe> {
    return this.http.put<Groupe>(`${this.apiUrl}/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /** Filières pour la liste déroulante (utilisateur connecté). */
  getFilieres(): Observable<Filiere[]> {
    return this.http.get<Filiere[]>('/api/filieres');
  }
}
