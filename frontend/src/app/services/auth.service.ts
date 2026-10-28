import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
}

const TOKEN_KEY = 'pgdi_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly apiUrl = '/api/auth';

  private readonly tokenSignal = signal<string | null>(this.readToken());
  readonly isLoggedIn = () => this.tokenSignal() !== null;

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${this.apiUrl}/login`, credentials)
      .pipe(tap((res) => this.saveToken(res.token)));
  }

  logout(): void {
    try {
      localStorage.removeItem(TOKEN_KEY);
    } catch {}
    this.tokenSignal.set(null);
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

  private readToken(): string | null {
    try {
      return localStorage.getItem(TOKEN_KEY);
    } catch {
      return null;
    }
  }
}
