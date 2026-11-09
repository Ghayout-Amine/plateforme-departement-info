import { Component, computed, inject, signal } from '@angular/core';
import { FiliereService } from '../../services/filiere.service';
import { Filiere } from '../../models/filiere.model';

@Component({
  selector: 'app-filieres',
  templateUrl: './filieres.html',
  styleUrl: './filieres.css',
})
export class Filieres {
  private readonly filiereService = inject(FiliereService);

  readonly loading = signal(true);
  readonly demo = signal(false);
  readonly filieres = signal<Filiere[]>([]);
  readonly niveauChoisi = signal<string>('TOUS');

  readonly niveaux = computed(() => [
    'TOUS',
    ...Array.from(new Set(this.filieres().map((f) => f.niveau))),
  ]);

  readonly filieresAffichees = computed(() =>
    this.niveauChoisi() === 'TOUS'
      ? this.filieres()
      : this.filieres().filter((f) => f.niveau === this.niveauChoisi()),
  );

  constructor() {
    this.filiereService.getFilieresPubliques().subscribe((res) => {
      this.filieres.set(res.filieres);
      this.demo.set(res.demo);
      this.loading.set(false);
    });
  }

  choisir(niveau: string): void {
    this.niveauChoisi.set(niveau);
  }
}
