import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Stats } from '../models/stats.model';

@Injectable({ providedIn: 'root' })
export class StatsService {
  private readonly http = inject(HttpClient);

  /** Chiffres réels du département (endpoint public, pas besoin d'être connecté). */
  getStats(): Observable<Stats> {
    return this.http.get<Stats>('/api/stats');
  }
}
