import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Grade,
  GradeCreate,
  GradeAverage
} from '../models/grade.model';

@Injectable({ providedIn: 'root' })
export class GradeService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/grades';

  create(data: GradeCreate): Observable<Grade> {
    return this.http.post<Grade>(this.apiUrl, data);
  }

  publish(id: number): Observable<Grade> {
    return this.http.patch<Grade>(
      `${this.apiUrl}/${id}/publish`, {}
    );
  }

  getMyGrades(): Observable<Grade[]> {
    return this.http.get<Grade[]>(`${this.apiUrl}/me`);
  }

  getAverage(
    studentId: number,
    moduleId: number,
    academicYearId: number
  ): Observable<GradeAverage> {
    const params = new HttpParams()
      .set('studentId', studentId)
      .set('moduleId', moduleId)
      .set('academicYearId', academicYearId);

    return this.http.get<GradeAverage>(
      `${this.apiUrl}/average`, { params }
    );
  }
}