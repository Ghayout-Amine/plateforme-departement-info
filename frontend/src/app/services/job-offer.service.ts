import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { JobOffer } from '../models/job-offer.model';

// PGDI2026-93 : appel à GET /api/stages/offres-externes (PGDI2026-86, mis en cache côté serveur)
@Injectable({ providedIn: 'root' })
export class JobOfferService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/stages/offres-externes';

  search(keyword: string, location: string): Observable<JobOffer[]> {
    let params = new HttpParams();
    if (keyword.trim()) params = params.set('keyword', keyword.trim());
    if (location.trim()) params = params.set('location', location.trim());
    return this.http.get<JobOffer[]>(this.apiUrl, { params });
  }
}
