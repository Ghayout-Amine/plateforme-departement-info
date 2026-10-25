import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ModuleCours, ModuleCoursRequest } from '../models/module-cours.model';

@Injectable({ providedIn: 'root' })
export class ModuleCoursApi {
  private http = inject(HttpClient);
  private url = `${environment.apiUrl}/modules`;

  getAll(): Observable<ModuleCours[]> {
    return this.http.get<ModuleCours[]>(this.url);
  }

  getByFiliere(filiereId: number): Observable<ModuleCours[]> {
    return this.http.get<ModuleCours[]>(`${this.url}/filiere/${filiereId}`);
  }

  create(module: ModuleCoursRequest): Observable<ModuleCours> {
    return this.http.post<ModuleCours>(this.url, module);
  }

  update(id: number, module: ModuleCoursRequest): Observable<ModuleCours> {
    return this.http.put<ModuleCours>(`${this.url}/${id}`, module);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
