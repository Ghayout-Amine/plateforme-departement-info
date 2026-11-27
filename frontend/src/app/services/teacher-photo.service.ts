import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TeacherPhotoUploadResponse {
  avatarUrl: string;
}

@Injectable({
  providedIn: 'root'
})
export class TeacherPhotoService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/teachers';

  uploadPhoto(
    teacherId: number,
    file: File
  ): Observable<TeacherPhotoUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<TeacherPhotoUploadResponse>(
      `${this.apiUrl}/${teacherId}/photo`,
      formData
    );
  }

  getPhoto(teacherId: number): Observable<Blob> {
    return this.http.get(
      `${this.apiUrl}/${teacherId}/photo`,
      { responseType: 'blob' }
    );
  }
}