import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Student, StudentCreate, StudentPage, StudentUpdate } from '../models/student.model';

@Injectable({
  providedIn: 'root'
})
export class StudentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/students';

  getPage(search = '', page = 0, size = 10): Observable<StudentPage> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', 'studentNumber,asc');

    if (search.trim()) {
      params = params.set('search', search.trim());
    }

    return this.http.get<StudentPage>(this.apiUrl, { params });
  }

  getById(id: number): Observable<Student> {
    return this.http.get<Student>(`${this.apiUrl}/${id}`);
  }

  create(data: StudentCreate): Observable<Student> {
    return this.http.post<Student>(this.apiUrl, data);
  }

  update(id: number, data: StudentUpdate): Observable<Student> {
    return this.http.put<Student>(`${this.apiUrl}/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
