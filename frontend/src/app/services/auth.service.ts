import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { LoginRequest, LoginResponse, UserRole } from '../models/auth.model';

const TOKEN_KEY = 'pgdi_token';
const USER_KEY = 'pgdi_user';

export type { LoginRequest, LoginResponse, UserRole };

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly apiUrl = '/api/auth';

  private readonly tokenSignal = signal<string | null>(this.readToken());
  private readonly currentUserSignal = signal<LoginResponse | null>(this.readUser());

  readonly isLoggedIn = () => this.tokenSignal() !== null;
  readonly currentUser = () => this.currentUserSignal();
  readonly currentRole = () => this.currentUserSignal()?.role ?? null;

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${this.apiUrl}/login`, credentials)
      .pipe(
        tap((res) => {
          this.saveToken(res.token);
          this.saveUser(res);
        })
      );
  }

  logout(): void {
    try {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
    } catch {}
    this.tokenSignal.set(null);
    this.currentUserSignal.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return this.tokenSignal();
  }

  private saveToken(token: string): void {
    try {
      localStorage.setItem(TOKEN_KEY, token);
    } catch {}
    this.tokenSignal.set(token);
  }

  private saveUser(user: LoginResponse): void {
    try {
      localStorage.setItem(USER_KEY, JSON.stringify(user));
    } catch {}
    this.currentUserSignal.set(user);
  }

  private readToken(): string | null {
    try {
      return localStorage.getItem(TOKEN_KEY);
    } catch {
      return null;
    }
  }

  private readUser(): LoginResponse | null {
    try {
      const data = localStorage.getItem(USER_KEY);
      return data ? JSON.parse(data) : null;
    } catch {
      return null;
    }
  }
}
