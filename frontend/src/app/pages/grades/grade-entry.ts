import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { GradeService } from '../../services/grade.service';
import { AuthService } from '../../services/auth.service';
import { Grade, GradeCreate, GradeType } from '../../models/grade.model';

@Component({
  selector: 'app-grade-entry',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './grade-entry.html',
  styleUrl: './grade-entry.css'
})
export class GradeEntry {
  private readonly gradeService = inject(GradeService);
  readonly auth = inject(AuthService);

  readonly loading = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly createdGrade = signal<Grade | null>(null);
  readonly myGrades = signal<Grade[]>([]);
  readonly myGradesLoaded = signal(false);

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
}