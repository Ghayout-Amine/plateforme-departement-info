import { Component, inject } from '@angular/core';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-dashboard',
  template: `
    <section style="padding: 2rem;">
      <h2>Tableau de bord</h2>
      @if (auth.currentUser(); as user) {
        <p>Bienvenue <strong>{{ user.firstName }} {{ user.lastName }}</strong> !</p>
        <p>Rôle actif : <span style="background: #e3f2fd; padding: 0.2rem 0.6rem; border-radius: 4px; font-weight: bold; color: #1565c0;">{{ user.role }}</span></p>
        <p>Email : {{ user.email }}</p>
      } @else {
        <p>Vous êtes connecté.</p>
      }
    </section>
  `,
})
export class Dashboard {
  readonly auth = inject(AuthService);
}
