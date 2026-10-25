import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';

const CLE_TOKEN = 'pgdi_token';

/**
 * Stockage minimal du token JWT.
 * À fusionner avec le service d'authentification de la story « connexion » quand il existera
 * (garder la même clé de stockage, ou faire lire ce service par l'intercepteur).
 */
@Injectable({ providedIn: 'root' })
export class AuthToken {
  private http = inject(HttpClient);

  get(): string | null {
    try {
      return localStorage.getItem(CLE_TOKEN);
    } catch {
      return null;
    }
  }

  set(token: string): void {
    try {
      localStorage.setItem(CLE_TOKEN, token);
    } catch {
      /* stockage indisponible */
    }
  }

  clear(): void {
    try {
      localStorage.removeItem(CLE_TOKEN);
    } catch {
      /* stockage indisponible */
    }
  }

  /** En développement uniquement : se connecte avec l'utilisateur de test s'il n'y a pas de token. */
  async connexionDevSiBesoin(): Promise<void> {
    const identifiants = environment.devLogin;
    if (!identifiants || this.get()) return;
    try {
      const rep = await firstValueFrom(
        this.http.post<{ token: string; tokenType: string }>(
          `${environment.apiUrl}/auth/login`,
          identifiants,
        ),
      );
      this.set(rep.token);
    } catch {
      console.warn('[dev] Connexion automatique impossible : le backend est-il démarré ?');
    }
  }
}
