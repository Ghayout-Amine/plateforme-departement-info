import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { GroupeService } from '../../../services/groupe.service';
import { GroupeRequest } from '../../../models/groupe.model';
import { Filiere } from '../../../models/filiere.model';

// PGDI2026-44 (US22) : ajout (/groupes/new) et modification (/groupes/:id/edit) d'un groupe
@Component({
  selector: 'app-groupe-form',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './groupe-form.html',
  styleUrl: './groupe-form.css'
})
export class GroupeForm implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly groupeService = inject(GroupeService);

  readonly filieres = signal<Filiere[]>([]);
  readonly isEdit = signal(false);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');

  private groupeId: number | null = null;

  name = '';
  capacity: number | null = null;
  filiereId: number | null = null;

  ngOnInit(): void {
    this.groupeService.getFilieres().subscribe({
      next: data => this.filieres.set(data),
      error: err => {
        console.error('Erreur de chargement des filières:', err);
        this.error.set('Impossible de charger les filières.');
      }
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) return;

    const id = Number(idParam);
    if (Number.isNaN(id)) {
      this.error.set('Identifiant invalide.');
      return;
    }

    this.isEdit.set(true);
    this.groupeId = id;
    this.loading.set(true);

    this.groupeService.getById(id).subscribe({
      next: groupe => {
        this.name = groupe.name;
        this.capacity = groupe.capacity ?? null;
        this.filiereId = groupe.filiereId;
        this.loading.set(false);
      },
      error: err => {
        console.error('Erreur de chargement:', err);
        this.error.set(err.status === 404 ? 'Ce groupe n’existe pas.' : 'Impossible de charger le groupe.');
        this.loading.set(false);
      }
    });
  }

  submitForm(): void {
    this.error.set('');

    if (!this.name.trim() || !this.filiereId) {
      this.error.set('Veuillez renseigner le nom du groupe et sa filière.');
      return;
    }
    if (this.capacity !== null && this.capacity < 1) {
      this.error.set('L’effectif maximal doit être au moins de 1.');
      return;
    }

    const payload: GroupeRequest = {
      name: this.name.trim(),
      capacity: this.capacity ? Number(this.capacity) : undefined,
      filiereId: Number(this.filiereId)
    };

    this.saving.set(true);

    const request$ = this.isEdit()
      ? this.groupeService.update(this.groupeId!, payload)
      : this.groupeService.create(payload);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.router.navigate(['/groupes']);
      },
      error: err => {
        console.error('Erreur d’enregistrement:', err);
        this.error.set(this.errorMessage(err));
        this.saving.set(false);
      }
    });
  }

  private errorMessage(err: any): string {
    if (err.status === 409) {
      return err.error?.message ?? 'Un groupe portant ce nom existe déjà dans cette filière.';
    }
    if (err.status === 404) {
      return err.error?.message ?? 'La filière choisie n’existe plus.';
    }
    if (err.status === 400) {
      const fieldErrors = err.error?.errors ? Object.values(err.error.errors).join(' ') : '';
      return fieldErrors || 'Certaines informations sont invalides. Vérifiez le formulaire.';
    }
    if (err.status === 401) {
      return 'Votre session a expiré. Veuillez vous reconnecter.';
    }
    if (err.status === 403) {
      return 'Seuls l’administrateur et le chef de département peuvent modifier les groupes.';
    }
    return 'L’enregistrement a échoué.';
  }
}
