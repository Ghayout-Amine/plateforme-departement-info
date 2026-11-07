import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';
import { Filiere } from '../models/filiere.model';
import { FILIERES_DEMO } from '../data/filieres-demo';

export interface FilieresResult {
  filieres: Filiere[];
  /** true si les données affichées sont les données de démonstration (API indisponible ou non publique) */
  demo: boolean;
}

@Injectable({ providedIn: 'root' })
export class FiliereService {
  private readonly http = inject(HttpClient);

  /** Liste des filières pour les pages publiques (accueil, filières). */
  getFilieresPubliques(): Observable<FilieresResult> {
    return this.http.get<Filiere[]>('/api/filieres').pipe(
      map((filieres) => ({ filieres, demo: false })),
      catchError(() => of({ filieres: FILIERES_DEMO, demo: true })),
    );
  }
}
