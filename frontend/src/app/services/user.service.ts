import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  UserResponse, UserCreate, UserUpdate,
  UpdateProfile, ChangePassword, UserPage, UserRole
} from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/users';

  // ─── ADMIN : Lister tous les utilisateurs ─────────────────────────────────
  getAll(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(this.apiUrl);
  }

  // ─── ADMIN : Lister avec filtres + pagination ──────────────────────────────
  search(role?: UserRole, enabled?: boolean, search?: string, page = 0, size = 10): Observable<UserPage> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (role) params = params.set('role', role);
    if (enabled !== undefined) params = params.set('enabled', enabled);
    if (search) params = params.set('search', search);
    return this.http.get<UserPage>(`${this.apiUrl}/search`, { params });
  }

  // ─── ADMIN : Récupérer un utilisateur par ID ───────────────────────────────
  getById(id: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/${id}`);
  }

  // ─── ADMIN : Créer un utilisateur ─────────────────────────────────────────
  create(data: UserCreate): Observable<UserResponse> {
    return this.http.post<UserResponse>(this.apiUrl, data);
  }

  // ─── ADMIN : Mettre à jour un utilisateur ─────────────────────────────────
  update(id: number, data: UserUpdate): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.apiUrl}/${id}`, data);
  }

  // ─── ADMIN : Activer / Désactiver un compte ────────────────────────────────
  toggleStatus(id: number): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.apiUrl}/${id}/toggle-status`, {});
  }

  // ─── ADMIN : Changer le rôle d'un utilisateur ─────────────────────────────
  changeRole(id: number, role: UserRole): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.apiUrl}/${id}/change-role?role=${role}`, {});
  }

  // ─── ADMIN : Supprimer un utilisateur ─────────────────────────────────────
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // ─── PROFIL : Mon profil ───────────────────────────────────────────────────
  getMyProfile(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/profile`);
  }

  // ─── PROFIL : Mettre à jour mon profil ────────────────────────────────────
  updateMyProfile(data: UpdateProfile): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.apiUrl}/profile`, data);
  }

  // ─── PROFIL : Changer mon mot de passe ────────────────────────────────────
  changePassword(data: ChangePassword): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/change-password`, data);
  }
}
