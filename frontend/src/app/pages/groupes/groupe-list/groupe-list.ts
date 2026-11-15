import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { GroupeService } from '../../../services/groupe.service';
import { Groupe } from '../../../models/groupe.model';
import { Filiere } from '../../../models/filiere.model';

// PGDI2026-44 (US22) : groupes d'étudiants, filtrés par filière côté serveur (GET /api/groupes?filiereId=)
@Component({
  selector: 'app-groupe-list',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './groupe-list.html',
  styleUrl: './groupe-list.css'
})
export class GroupeList implements OnInit {
  private readonly groupeService = inject(GroupeService);

  readonly groupes = signal<Groupe[]>([]);
  readonly filieres = signal<Filiere[]>([]);
  readonly selectedFiliereId = signal<number | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly searchTerm = signal('');

  readonly filteredGroupes = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    if (!term) return this.groupes();

    return this.groupes().filter(groupe =>
      [groupe.name, groupe.filiereCode, groupe.filiereNom].some(value => value?.toLowerCase().includes(term))
    );
  });

  readonly filieresCount = computed(() => new Set(this.groupes().map(groupe => groupe.filiereId)).size);

  readonly totalCapacity = computed(() =>
    this.groupes().reduce((sum, groupe) => sum + (groupe.capacity ?? 0), 0)
  );

  ngOnInit(): void {
    this.groupeService.getFilieres().subscribe({
      next: data => this.filieres.set(data),
      error: err => console.error('Erreur de chargement des filières:', err)
    });
    this.loadGroupes();
  }

  onFiliereChange(filiereId: number | null): void {
    this.selectedFiliereId.set(filiereId);
    this.loadGroupes();
  }

  loadGroupes(): void {
    this.loading.set(true);
    this.error.set('');

    this.groupeService.getAll(this.selectedFiliereId()).subscribe({
      next: data => {
        this.groupes.set(data);
        this.loading.set(false);
      },
      error: err => {
        console.error('Erreur de chargement des groupes:', err);
        this.error.set(
          err.status === 401
            ? 'Votre session a expiré. Veuillez vous reconnecter.'
            : err.status === 403
              ? 'Vous ne disposez pas des droits nécessaires.'
              : 'Impossible de charger les groupes. Vérifiez le serveur.'
        );
        this.loading.set(false);
      }
    });
  }

  deleteGroupe(groupe: Groupe): void {
    const confirmed = window.confirm(`Voulez-vous vraiment supprimer le groupe « ${groupe.name} » (${groupe.filiereCode}) ?`);
    if (!confirmed) return;

    this.groupeService.delete(groupe.id).subscribe({
      next: () => this.groupes.update(list => list.filter(item => item.id !== groupe.id)),
      error: err => {
        console.error('Erreur de suppression:', err);
        window.alert(
          err.status === 403
            ? 'Seuls l’administrateur et le chef de département peuvent supprimer un groupe.'
            : 'La suppression a échoué.'
        );
      }
    });
  }
}
