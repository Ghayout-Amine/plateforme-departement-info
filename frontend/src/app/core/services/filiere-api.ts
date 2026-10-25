import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Filiere, FiliereRequest } from '../models/filiere.model';

@Injectable({ providedIn: 'root' })
export class FiliereApi {
  private http = inject(HttpClient);
  private url = `${environment.apiUrl}/filieres`;

  getAll(): Observable<Filiere[]> {
    return this.http.get<Filiere[]>(this.url);
  }

  getById(id: number): Observable<Filiere> {
    return this.http.get<Filiere>(`${this.url}/${id}`);
  }

  create(filiere: FiliereRequest): Observable<Filiere> {
    return this.http.post<Filiere>(this.url, filiere);
  }

  update(id: number, filiere: FiliereRequest): Observable<Filiere> {
    return this.http.put<Filiere>(`${this.url}/${id}`, filiere);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
