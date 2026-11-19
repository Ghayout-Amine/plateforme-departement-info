import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Planning, PlanningRequest } from '../models/planning.model';
import { ModuleCours } from '../models/module.model';

// PGDI2026-75 (US36) : appels à l'API /api/plannings (le jeton JWT est ajouté par l'intercepteur)
@Injectable({
  providedIn: 'root'
})
export class PlanningService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/plannings';

  // Séances de la semaine qui contient "semaine" (AAAA-MM-JJ), filtrées par groupe, enseignant ou salle
  getWeek(semaine: string, filters: { groupeId?: number; teacherId?: number; salleId?: number } = {}): Observable<Planning[]> {
    let params = new HttpParams().set('semaine', semaine);
    if (filters.groupeId) params = params.set('groupeId', filters.groupeId);
    if (filters.teacherId) params = params.set('teacherId', filters.teacherId);
    if (filters.salleId) params = params.set('salleId', filters.salleId);
    return this.http.get<Planning[]>(this.apiUrl, { params });
  }

  // Mon emploi du temps (étudiant ou enseignant connecté)
  getMyWeek(semaine: string): Observable<Planning[]> {
    return this.http.get<Planning[]>(`${this.apiUrl}/me`, { params: { semaine } });
  }

  getById(id: number): Observable<Planning> {
    return this.http.get<Planning>(`${this.apiUrl}/${id}`);
  }

  create(data: PlanningRequest): Observable<Planning> {
    return this.http.post<Planning>(this.apiUrl, data);
  }

  // Même séance chaque semaine pendant data.weeks semaines (tout ou rien côté serveur)
  createSerie(data: PlanningRequest): Observable<Planning[]> {
    return this.http.post<Planning[]>(`${this.apiUrl}/serie`, data);
  }

  update(id: number, data: PlanningRequest): Observable<Planning> {
    return this.http.put<Planning>(`${this.apiUrl}/${id}`, data);
  }

  cancel(id: number): Observable<Planning> {
    return this.http.patch<Planning>(`${this.apiUrl}/${id}/annuler`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Modules (pour les listes déroulantes des formulaires de séance et d'examen)
  getModules(): Observable<ModuleCours[]> {
    return this.http.get<ModuleCours[]>('/api/modules');
  }
}
