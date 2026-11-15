import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { SalleService } from '../../../services/salle.service';
import { ROOM_TYPE_LABELS, ROOM_TYPES, RoomType, SalleRequest } from '../../../models/salle.model';

// PGDI2026-44 (US21) : un seul composant pour l'ajout (/salles/new) et la modification (/salles/:id/edit)
@Component({
  selector: 'app-salle-form',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './salle-form.html',
  styleUrl: './salle-form.css'
})
export class SalleForm implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly salleService = inject(SalleService);

  readonly roomTypes = ROOM_TYPES;
  readonly typeLabels = ROOM_TYPE_LABELS;

  readonly isEdit = signal(false);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');

  private salleId: number | null = null;

  code = '';
  name = '';
  building = '';
  capacity: number | null = null;
  roomType: RoomType | '' = '';
  isActive = true;

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) return;

    const id = Number(idParam);
    if (Number.isNaN(id)) {
      this.error.set('Identifiant invalide.');
      return;
    }

    this.isEdit.set(true);
    this.salleId = id;
    this.loading.set(true);

    this.salleService.getById(id).subscribe({
      next: salle => {
        this.code = salle.code;
        this.name = salle.name;
        this.building = salle.building ?? '';
        this.capacity = salle.capacity;
        this.roomType = salle.roomType;
        this.isActive = salle.isActive;
        this.loading.set(false);
      },
      error: err => {
        console.error('Erreur de chargement:', err);
        this.error.set(err.status === 404 ? 'Cette salle n’existe pas.' : 'Impossible de charger la salle.');
        this.loading.set(false);
      }
    });
  }

  submitForm(): void {
    this.error.set('');

    if (!this.code.trim() || !this.name.trim() || !this.roomType || !this.capacity) {
      this.error.set('Veuillez remplir tous les champs obligatoires.');
      return;
    }
    if (this.capacity < 1) {
      this.error.set('La capacité doit être au moins de 1 place.');
      return;
    }

    const payload: SalleRequest = {
      code: this.code.trim().toUpperCase(),
      name: this.name.trim(),
      building: this.building.trim() || undefined,
      capacity: Number(this.capacity),
      roomType: this.roomType,
      isActive: this.isActive
    };

    this.saving.set(true);

    const request$ = this.isEdit()
      ? this.salleService.update(this.salleId!, payload)
      : this.salleService.create(payload);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.router.navigate(['/salles']);
      },
      error: err => {
        console.error('Erreur d’enregistrement:', err);
        this.error.set(this.errorMessage(err));
        this.saving.set(false);
      }
    });
  }

  // Le backend renvoie un message clair pour les doublons (409) et les champs invalides (400)
  private errorMessage(err: any): string {
    if (err.status === 409) {
      return err.error?.message ?? 'Une salle avec ce code existe déjà.';
    }
    if (err.status === 400) {
      const fieldErrors = err.error?.errors ? Object.values(err.error.errors).join(' ') : '';
      return fieldErrors || 'Certaines informations sont invalides. Vérifiez le formulaire.';
    }
    if (err.status === 401) {
      return 'Votre session a expiré. Veuillez vous reconnecter.';
    }
    if (err.status === 403) {
      return 'Seuls l’administrateur et le chef de département peuvent modifier les salles.';
    }
    return 'L’enregistrement a échoué.';
  }
}
