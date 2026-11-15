import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { StatsService } from '../../services/stats.service';
import { Stats } from '../../models/stats.model';

interface Actualite {
  date: string;
  titre: string;
  texte: string;
}

@Component({
  selector: 'app-home',
  imports: [RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home {
  private readonly statsService = inject(StatsService);

  // Chiffres réels lus dans la base via GET /api/stats ; null tant que la réponse n'est pas arrivée
  readonly stats = signal<Stats | null>(null);
  readonly statsError = signal(false);

  readonly actualites: Actualite[] = [
    {
      date: '19 oct. 2026',
      titre: 'Lancement de la plateforme PGDI',
      texte: 'Début du Sprint 1 : authentification, filières et modules.',
    },
    {
      date: '28 oct. 2026',
      titre: 'Connexion sécurisée disponible',
      texte: 'Les utilisateurs peuvent maintenant se connecter avec leur compte.',
    },
    {
      date: '2 nov. 2026',
      titre: 'Documentation de l’API en ligne',
      texte: 'L’API est documentée avec Swagger / OpenAPI.',
    },
  ];

  constructor() {
    this.statsService.getStats().subscribe({
      next: (data) => this.stats.set(data),
      error: () => this.statsError.set(true),
    });
  }
}
