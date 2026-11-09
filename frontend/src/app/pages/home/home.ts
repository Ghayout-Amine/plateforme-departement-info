import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FiliereService } from '../../services/filiere.service';

interface Actualite {
  date: string;
  titre: string;
  texte: string;
}

interface ChiffreCle {
  valeur: string;
  libelle: string;
}

@Component({
  selector: 'app-home',
  imports: [RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home {
  private readonly filiereService = inject(FiliereService);

  readonly nbFilieres = signal<number | null>(null);

  // Valeurs de démonstration : à remplacer par des données du backend quand un endpoint de statistiques existera.
  readonly chiffres: ChiffreCle[] = [
    { valeur: '350', libelle: 'Étudiants' },
    { valeur: '24', libelle: 'Enseignants' },
    { valeur: '60', libelle: 'Modules' },
  ];

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
    this.filiereService.getFilieresPubliques().subscribe((res) => this.nbFilieres.set(res.filieres.length));
  }
}
