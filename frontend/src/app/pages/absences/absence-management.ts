import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AbsenceService } from '../../services/absence.service';
import { AuthService } from '../../services/auth.service';
import { Absence, AbsenceCreate } from '../../models/absence.model';

@Component({
  selector: 'app-absence-management',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './absence-management.html',
  styleUrl: './absence-management.css'
})
export class AbsenceManagement {
  private readonly service = inject(AbsenceService);
  readonly auth = inject(AuthService);

  readonly loading = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly lastAbsence = signal<Absence | null>(null);
  readonly myAbsences = signal<Absence[]>([]);
  readonly myAbsencesLoaded = signal(false);

  form: AbsenceCreate = {
    studentId: 0,
    moduleId: 0,
    date: new Date().toLocaleDateString('en-CA'),
    sessionTime: '',
    justified: false,
    motif: null
  };

  justificationId = 0;
  justificationMotif = '';

  private resetMessages(): void {
    this.error.set('');
    this.success.set('');
  }

  private showError(status: number, operation: string): void {
    this.error.set(
      status === 401
        ? 'Session expiree. Veuillez vous reconnecter.'
        : status === 403
          ? 'Acces refuse pour cette operation.'
          : status === 409
            ? 'Cette absence existe deja ou est deja justifiee.'
            : `Impossible de ${operation}.`
    );
  }

  save(): void {
    this.resetMessages();

    if (this.loading()) return;

    const data: AbsenceCreate = {
      ...this.form,
      sessionTime: this.form.sessionTime.trim(),
      motif: this.form.motif?.trim() || null
    };

    if (!Number.isInteger(data.studentId) || data.studentId <= 0 ||
        !Number.isInteger(data.moduleId) || data.moduleId <= 0 ||
        !data.date || !data.sessionTime ||
        data.sessionTime.length > 50 ||
        (data.justified && !data.motif) ||
        (data.motif?.length ?? 0) > 500) {
      this.error.set('Veuillez verifier les informations de la seance.');
      return;
    }

    this.loading.set(true);
    this.service.create(data).subscribe({
      next: absence => {
        this.lastAbsence.set(absence);
        this.success.set('Absence enregistree avec succes.');
        this.loading.set(false);
      },
      error: err => {
        this.showError(err.status, 'enregistrer cette absence');
        this.loading.set(false);
      }
    });
  }

  justify(): void {
    this.resetMessages();

    if (this.loading()) return;

    if (!Number.isInteger(this.justificationId) ||
        this.justificationId <= 0 ||
        !this.justificationMotif.trim() ||
        this.justificationMotif.length > 500) {
      this.error.set('Identifiant ou motif de justification invalide.');
      return;
    }

    this.loading.set(true);
    this.service.justify(this.justificationId, {
      motif: this.justificationMotif.trim()
    }).subscribe({
      next: absence => {
        this.lastAbsence.set(absence);
        this.success.set('Absence justifiee avec succes.');
        this.loading.set(false);
      },
      error: err => {
        this.showError(err.status, 'justifier cette absence');
        this.loading.set(false);
      }
    });
  }

  loadMyAbsences(): void {
    this.resetMessages();

    if (this.loading()) return;

    this.loading.set(true);
    this.service.getMyAbsences().subscribe({
      next: absences => {
        this.myAbsences.set(absences);
        this.myAbsencesLoaded.set(true);
        this.loading.set(false);
      },
      error: err => {
        this.showError(err.status, 'charger vos absences');
        this.loading.set(false);
      }
    });
  }
}