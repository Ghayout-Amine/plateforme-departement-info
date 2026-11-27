import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Absence,
  AbsenceCreate,
  AbsenceJustification
} from '../models/absence.model';

@Injectable({ providedIn: 'root' })
export class AbsenceService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/absences';

  create(data: AbsenceCreate): Observable<Absence> {
    return this.http.post<Absence>(this.apiUrl, data);
  }

  justify(
    id: number,
    data: AbsenceJustification
  ): Observable<Absence> {
    return this.http.patch<Absence>(
      `${this.apiUrl}/${id}/justify`, data
    );
  }

  getMyAbsences(): Observable<Absence[]> {
    return this.http.get<Absence[]>(`${this.apiUrl}/me`);
  }

  uploadJustificatif(id: number, file: File): Observable<Absence> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Absence>(`${this.apiUrl}/${id}/justificatif`, formData);
  }

  getJustificatif(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/justificatif`, { responseType: 'blob' });
  }
}