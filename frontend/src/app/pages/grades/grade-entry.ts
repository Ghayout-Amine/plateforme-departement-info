import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { GradeService } from '../../services/grade.service';
import { AuthService } from '../../services/auth.service';
import { Grade, GradeCreate, GradeType, ReleveNotes } from '../../models/grade.model';

@Component({
  selector: 'app-grade-entry',
  standalone: true,
  imports: [FormsModule, DecimalPipe],
  templateUrl: './grade-entry.html',
  styleUrl: './grade-entry.css'
})
export class GradeEntry implements OnInit {
  private readonly gradeService = inject(GradeService);
  readonly auth = inject(AuthService);

  readonly loading = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly createdGrade = signal<Grade | null>(null);
  readonly myGrades = signal<Grade[]>([]);
  readonly myGradesLoaded = signal(false);

  // PGDI2026-90 : Relevé de notes semestriel
  readonly releve = signal<ReleveNotes | null>(null);
  readonly releveLoading = signal(false);
  readonly releveError = signal('');
  selectedSemester = 'S3';
  readonly semesters = ['S1', 'S2', 'S3', 'S4', 'S5', 'S6'];

  ngOnInit(): void {
    if (this.auth.currentRole() === 'ETUDIANT') {
      this.loadMyGrades();
      this.loadMyReleve();
    }
  }

  readonly types: GradeType[] = [
    'CONTROLE_CONTINU',
    'EXAMEN_FINAL',
    'TP',
    'RATTRAPAGE'
  ];

  form: GradeCreate = {
    studentId: 0,
    moduleId: 0,
    score: 0,
    type: 'CONTROLE_CONTINU',
    coefficient: 1,
    sessionDate: new Date().toISOString().slice(0, 10)
  };

  save(): void {
    this.error.set('');
    this.success.set('');

    if (this.form.studentId <= 0 || this.form.moduleId <= 0) {
      this.error.set('Veuillez saisir un etudiant et un module valides.');
      return;
    }

    if (this.form.score < 0 || this.form.score > 20 ||
        this.form.coefficient <= 0 || this.form.coefficient > 999.99) {
      this.error.set('Note ou coefficient invalide.');
      return;
    }

    this.loading.set(true);
    this.createdGrade.set(null);

    this.gradeService.create(this.form).subscribe({
      next: grade => {
        this.createdGrade.set(grade);
        this.success.set('Note enregistree en brouillon.');
        this.loading.set(false);
      },
      error: err => {
        this.error.set(
          err.status === 403
            ? 'Acces refuse : vous ne gerez pas ce module.'
            : 'Impossible d enregistrer la note.'
        );
        this.loading.set(false);
      }
    });
  }

  publish(): void {
    const grade = this.createdGrade();
    if (!grade || this.loading()) return;

    this.error.set('');
    this.success.set('');
    this.loading.set(true);

    this.gradeService.publish(grade.id).subscribe({
      next: updated => {
        this.createdGrade.set(updated);
        this.success.set('Note publiee avec succes.');
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de publier cette note.');
        this.loading.set(false);
      }
    });
  }

  loadMyGrades(): void {
    if (this.loading()) return;

    this.error.set('');
    this.loading.set(true);

    this.gradeService.getMyGrades().subscribe({
      next: grades => {
        this.myGrades.set(grades);
        this.myGradesLoaded.set(true);
        this.loading.set(false);
      },
      error: err => {
        this.error.set(
          err.status === 401
            ? 'Session expiree. Veuillez vous reconnecter.'
            : err.status === 403
              ? 'Acces refuse.'
              : 'Impossible de charger vos notes.'
        );
        this.loading.set(false);
      }
    });
  }

  loadMyReleve(): void {
    this.releveError.set('');
    this.releve.set(null);
    this.releveLoading.set(true);

    this.gradeService.getMyReleve(this.selectedSemester).subscribe({
      next: data => {
        this.releve.set(data);
        this.releveLoading.set(false);
      },
      error: err => {
        this.releveError.set(
          err?.error?.message || 'Releve indisponible pour ce semestre.'
        );
        this.releveLoading.set(false);
      }
    });
  }

  statusLabel(status: string): string {
    switch (status) {
      case 'VALIDE':
        return 'Validé';
      case 'VALIDE_PAR_COMPENSATION':
        return 'Validé par compensation';
      case 'NON_VALIDE':
        return 'Non validé';
      default:
        return status;
    }
  }

  printReleve(): void {
    window.print();
  }
}