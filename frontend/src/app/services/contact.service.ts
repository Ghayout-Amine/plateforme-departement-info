import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, delay, of } from 'rxjs';
import { ContactMessage } from '../models/contact.model';

// Passer à true quand le endpoint backend POST /api/contact sera disponible (sous-tâche [Backend]).
// Tant que c'est false, l'envoi est simulé côté navigateur (aucune donnée n'est envoyée).
const CONTACT_API_ENABLED = false;

@Injectable({ providedIn: 'root' })
export class ContactService {
  private readonly http = inject(HttpClient);

  envoyer(message: ContactMessage): Observable<unknown> {
    if (!CONTACT_API_ENABLED) {
      return of({ simulated: true }).pipe(delay(600));
    }
    return this.http.post('/api/contact', message);
  }
}
