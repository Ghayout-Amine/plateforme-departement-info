import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Teacher, TeacherRequest } from '../models/teacher.model';

@Injectable({
  providedIn: 'root'
})
export class TeacherService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/teachers';

  getAll(): Observable<Teacher[]> {
    return this.http.get<Teacher[]>(this.apiUrl);
  }

  getById(id: number): Observable<Teacher> {
    return this.http.get<Teacher>(`${this.apiUrl}/${id}`);
  }

  create(data: TeacherRequest): Observable<Teacher> {
    return this.http.post<Teacher>(this.apiUrl, data);
  }

  update(id: number, data: TeacherRequest): Observable<Teacher> {
    return this.http.put<Teacher>(`${this.apiUrl}/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
