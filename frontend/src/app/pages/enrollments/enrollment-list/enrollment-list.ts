import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { EnrollmentService } from '../../../services/enrollment.service';
import { GroupeService } from '../../../services/groupe.service';
import { StudentService } from '../../../services/student.service';
import {
  AcademicYear,
  Enrollment,
  EnrollmentRequest,
  EnrollmentStatus,
  ENROLLMENT_STATUS_LABELS
} from '../../../models/enrollment.model';
import { Filiere } from '../../../models/filiere.model';
import { Groupe } from '../../../models/groupe.model';
import { Student } from '../../../models/student.model';

// PGDI2026-60 (US26) : inscrire un étudiant dans une filière pour une année,
// puis l'affecter à un groupe de TD/TP (directement depuis le tableau).
@Component({
  selector: 'app-enrollment-list',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './enrollment-list.html',
  styleUrl: './enrollment-list.css'
})
export class EnrollmentList implements OnInit {
  private readonly enrollmentService = inject(EnrollmentService);
  private readonly groupeService = inject(GroupeService);
  private readonly studentService = inject(StudentService);

  readonly statusLabels = ENROLLMENT_STATUS_LABELS;
  readonly statuses = Object.keys(ENROLLMENT_STATUS_LABELS) as EnrollmentStatus[];

  // Données de référence
  readonly years = signal<AcademicYear[]>([]);
  readonly filieres = signal<Filiere[]>([]);
  readonly groupes = signal<Groupe[]>([]);       // tous les groupes (pour le tableau)
  readonly students = signal<Student[]>([]);

  // Liste et filtres
  readonly enrollments = signal<Enrollment[]>([]);
  readonly selectedYearId = signal<number | null>(null);
  readonly filterFiliereId = signal<number | null>(null);
  readonly filterGroupeId = signal<number | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly notice = signal('');

  // Formulaire d'inscription
  formStudentId: number | null = null;
  formFiliereId: number | null = null;
  formGroupeId: number | null = null;
  readonly formError = signal('');
  readonly saving = signal(false);

  // Formulaire « nouvelle année universitaire »
  readonly showYearForm = signal(false);
  yearLabel = '';
  yearStart = '';
  yearEnd = '';
  yearCurrent = true;
  readonly yearError = signal('');

  readonly withoutGroupe = computed(() => this.enrollments().filter(e => !e.groupeId).length);

  // Étudiants pas encore inscrits pour l'année choisie
  readonly availableStudents = computed(() => {
    const enrolledIds = new Set(this.enrollments().map(e => e.studentId));
    return this.students().filter(s => !enrolledIds.has(s.id));
  });

  // Groupes de la filière choisie dans le formulaire
  readonly formGroupes = signal<Groupe[]>([]);

  // Groupes proposés dans le filtre (ceux de la filière filtrée)
  readonly filterGroupes = computed(() => {
    const filiereId = this.filterFiliereId();
    return filiereId ? this.groupes().filter(g => g.filiereId === filiereId) : this.groupes();
  });

  ngOnInit(): void {
    forkJoin({
      years: this.enrollmentService.getAcademicYears(),
      filieres: this.groupeService.getFilieres(),
      groupes: this.groupeService.getAll(),
      students: this.studentService.getPage('', 0, 500)
    }).subscribe({
      next: ({ years, filieres, groupes, students }) => {
        this.years.set(years);
        this.filieres.set(filieres);
        this.groupes.set(groupes);
        this.students.set(students.content);

        const current = years.find(y => y.current) ?? years[0];
        this.selectedYearId.set(current?.id ?? null);
        this.showYearForm.set(years.length === 0);
        this.loadEnrollments();
      },
      error: err => {
        console.error('Erreur de chargement des données:', err);
        this.error.set(this.httpMessage(err, 'Impossible de charger les données. Vérifiez le serveur.'));
        this.loading.set(false);
      }
    });
  }

  loadEnrollments(): void {
    if (!this.selectedYearId()) {
      this.enrollments.set([]);
      this.loading.set(false);
      return;
    }
    this.loading.set(true);
    this.error.set('');

    this.enrollmentService.getAll({
      academicYearId: this.selectedYearId(),
      filiereId: this.filterFiliereId(),
      groupeId: this.filterGroupeId()
    }).subscribe({
      next: data => {
        this.enrollments.set(data);
        this.loading.set(false);
      },
      error: err => {
        console.error('Erreur de chargement des inscriptions:', err);
        this.error.set(this.httpMessage(err, 'Impossible de charger les inscriptions.'));
        this.loading.set(false);
      }
    });
  }

  onYearChange(yearId: number | null): void {
    this.selectedYearId.set(yearId);
    this.loadEnrollments();
  }

  onFilterFiliereChange(filiereId: number | null): void {
    this.filterFiliereId.set(filiereId);
    this.filterGroupeId.set(null);
    this.loadEnrollments();
  }

  onFilterGroupeChange(groupeId: number | null): void {
    this.filterGroupeId.set(groupeId);
    this.loadEnrollments();
  }

  // Quand on choisit une filière dans le formulaire, on charge ses groupes
  onFormFiliereChange(filiereId: number | null): void {
    this.formFiliereId = filiereId;
    this.formGroupeId = null;
    this.formGroupes.set(filiereId ? this.groupes().filter(g => g.filiereId === filiereId) : []);
  }

  enroll(): void {
    this.formError.set('');
    this.notice.set('');

    if (!this.formStudentId || !this.formFiliereId || !this.selectedYearId()) {
      this.formError.set('Choisissez un étudiant, une filière et une année universitaire.');
      return;
    }

    this.saving.set(true);
    this.enrollmentService.create({
      studentId: this.formStudentId,
      filiereId: this.formFiliereId,
      academicYearId: this.selectedYearId()!,
      groupeId: this.formGroupeId
    }).subscribe({
      next: created => {
        this.saving.set(false);
        this.notice.set(`${created.studentFirstName} ${created.studentLastName} est inscrit(e) en ${created.filiereCode}`
          + (created.groupeName ? `, groupe ${created.groupeName}.` : ' (groupe à affecter).'));
        this.formStudentId = null;
        this.formGroupeId = null;
        this.loadEnrollments();
      },
      error: err => {
        console.error('Erreur d’inscription:', err);
        this.formError.set(this.httpMessage(err, 'L’inscription a échoué.'));
        this.saving.set(false);
      }
    });
  }

  groupesOf(enrollment: Enrollment): Groupe[] {
    return this.groupes().filter(g => g.filiereId === enrollment.filiereId);
  }

  // Changement de groupe ou de statut directement depuis le tableau
  changeGroupe(enrollment: Enrollment, groupeId: number | null): void {
    this.saveRow(enrollment, { groupeId }, `${enrollment.studentFirstName} ${enrollment.studentLastName} a changé de groupe.`);
  }

  changeStatus(enrollment: Enrollment, status: EnrollmentStatus): void {
    this.saveRow(enrollment, { status }, `Statut mis à jour : ${this.statusLabels[status]}.`);
  }

  private saveRow(enrollment: Enrollment, change: Partial<EnrollmentRequest>, successMessage: string): void {
    this.notice.set('');
    this.error.set('');
    const request: EnrollmentRequest = {
      studentId: enrollment.studentId,
      filiereId: enrollment.filiereId,
      academicYearId: enrollment.academicYearId,
      groupeId: enrollment.groupeId ?? null,
      status: enrollment.status,
      ...change
    };

    this.enrollmentService.update(enrollment.id, request).subscribe({
      next: updated => {
        this.enrollments.update(list => list.map(e => (e.id === updated.id ? updated : e)));
        this.notice.set(successMessage);
      },
      error: err => {
        console.error('Erreur de mise à jour:', err);
        // On recharge pour remettre la ligne dans son état réel (ex. groupe complet refusé)
        this.loadEnrollments();
        this.error.set(this.httpMessage(err, 'La modification a échoué.'));
      }
    });
  }

  deleteEnrollment(enrollment: Enrollment): void {
    const confirmed = window.confirm(
      `Supprimer l’inscription de ${enrollment.studentFirstName} ${enrollment.studentLastName} pour ${enrollment.academicYearLabel} ?`
    );
    if (!confirmed) return;

    this.enrollmentService.delete(enrollment.id).subscribe({
      next: () => this.enrollments.update(list => list.filter(e => e.id !== enrollment.id)),
      error: err => {
        console.error('Erreur de suppression:', err);
        window.alert(this.httpMessage(err, 'La suppression a échoué.'));
      }
    });
  }

  createYear(): void {
    this.yearError.set('');
    if (!this.yearLabel.trim() || !this.yearStart || !this.yearEnd) {
      this.yearError.set('Remplissez le libellé et les deux dates.');
      return;
    }

    this.enrollmentService.createAcademicYear({
      label: this.yearLabel.trim(),
      startDate: this.yearStart,
      endDate: this.yearEnd,
      current: this.yearCurrent
    }).subscribe({
      next: year => {
        this.enrollmentService.getAcademicYears().subscribe(years => this.years.set(years));
        this.showYearForm.set(false);
        this.yearLabel = '';
        this.yearStart = '';
        this.yearEnd = '';
        this.onYearChange(year.id);
      },
      error: err => {
        console.error('Erreur de création de l’année:', err);
        this.yearError.set(this.httpMessage(err, 'La création de l’année a échoué.'));
      }
    });
  }

  initials(e: Enrollment): string {
    return `${e.studentFirstName?.charAt(0) ?? ''}${e.studentLastName?.charAt(0) ?? ''}`.toUpperCase();
  }

  // Le backend renvoie un message clair pour 400 / 404 / 409
  private httpMessage(err: any, fallback: string): string {
    if (err.status === 401) return 'Votre session a expiré. Veuillez vous reconnecter.';
    if (err.status === 403) return 'Vous ne disposez pas des droits nécessaires.';
    if ([400, 404, 409].includes(err.status) && err.error?.message && err.error.message !== 'Erreur de validation') {
      return err.error.message;
    }
    if (err.status === 400 && err.error?.errors) {
      return Object.values(err.error.errors).join(' ');
    }
    if (err.status === 400) return 'Certaines informations sont invalides.';
    return fallback;
  }
}
