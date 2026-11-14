import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { SalleService } from '../../../services/salle.service';
import { ROOM_TYPE_LABELS, ROOM_TYPES, RoomType, Salle } from '../../../models/salle.model';

// PGDI2026-44 (US21) : liste des salles avec recherche, filtre par type et suppression
@Component({
  selector: 'app-salle-list',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './salle-list.html',
  styleUrl: './salle-list.css'
})
export class SalleList implements OnInit {
  private readonly salleService = inject(SalleService);

  readonly roomTypes = ROOM_TYPES;
  readonly typeLabels = ROOM_TYPE_LABELS;

  readonly salles = signal<Salle[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly searchTerm = signal('');
  readonly typeFilter = signal<RoomType | ''>('');

  readonly filteredSalles = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const type = this.typeFilter();

    return this.salles().filter(salle =>
      (!type || salle.roomType === type) &&
      (!term || [salle.code, salle.name, salle.building]
        .some(value => value?.toLowerCase().includes(term)))
    );
  });

  readonly activeCount = computed(() => this.salles().filter(salle => salle.isActive).length);

  readonly totalCapacity = computed(() =>
    this.salles()
      .filter(salle => salle.isActive)
      .reduce((sum, salle) => sum + salle.capacity, 0)
  );

  ngOnInit(): void {
    this.loadSalles();
  }

  loadSalles(): void {
    this.loading.set(true);
    this.error.set('');

    this.salleService.getAll().subscribe({
      next: data => {
        this.salles.set(data);
        this.loading.set(false);
      },
      error: err => {
        console.error('Erreur de chargement des salles:', err);
        this.error.set(
          err.status === 401
            ? 'Votre session a expiré. Veuillez vous reconnecter.'
            : err.status === 403
              ? 'Vous ne disposez pas des droits nécessaires.'
              : 'Impossible de charger les salles. Vérifiez le serveur.'
        );
        this.loading.set(false);
      }
    });
  }

  deleteSalle(salle: Salle): void {
    const confirmed = window.confirm(`Voulez-vous vraiment supprimer la salle ${salle.code} (${salle.name}) ?`);
    if (!confirmed) return;

    this.salleService.delete(salle.id).subscribe({
      next: () => this.salles.update(list => list.filter(item => item.id !== salle.id)),
      error: err => {
        console.error('Erreur de suppression:', err);
        window.alert(
          err.status === 403
            ? 'Seuls l’administrateur et le chef de département peuvent supprimer une salle.'
            : err.status === 409
              ? 'Cette salle est encore utilisée : elle ne peut pas être supprimée.'
              : 'La suppression a échoué.'
        );
      }
    });
  }
}
