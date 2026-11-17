import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AcademicYear, AcademicYearRequest, Enrollment, EnrollmentRequest } from '../models/enrollment.model';

// PGDI2026-60 (US26) : appels à /api/enrollments et /api/academic-years
@Injectable({
  providedIn: 'root'
})
export class EnrollmentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/enrollments';
  private readonly yearsUrl = '/api/academic-years';

  /** Inscriptions, avec filtres facultatifs (année, filière, groupe). */
  getAll(filters: { academicYearId?: number | null; filiereId?: number | null; groupeId?: number | null }): Observable<Enrollment[]> {
    let params = new HttpParams();
    if (filters.academicYearId) params = params.set('academicYearId', filters.academicYearId);
    if (filters.filiereId) params = params.set('filiereId', filters.filiereId);
    if (filters.groupeId) params = params.set('groupeId', filters.groupeId);
    return this.http.get<Enrollment[]>(this.apiUrl, { params });
  }

  create(data: EnrollmentRequest): Observable<Enrollment> {
    return this.http.post<Enrollment>(this.apiUrl, data);
  }

  update(id: number, data: EnrollmentRequest): Observable<Enrollment> {
    return this.http.put<Enrollment>(`${this.apiUrl}/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getAcademicYears(): Observable<AcademicYear[]> {
    return this.http.get<AcademicYear[]>(this.yearsUrl);
  }

  createAcademicYear(data: AcademicYearRequest): Observable<AcademicYear> {
    return this.http.post<AcademicYear>(this.yearsUrl, data);
  }
}
