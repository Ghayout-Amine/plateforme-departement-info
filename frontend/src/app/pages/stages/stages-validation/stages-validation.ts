import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { InternshipService } from '../../../services/internship.service';
import {
  INTERNSHIP_STATUS_LABELS,
  INTERNSHIP_TYPE_LABELS,
  Internship,
  InternshipStatus,
} from '../../../models/internship.model';
import { TeacherService } from '../../../services/teacher.service';
import { Teacher } from '../../../models/teacher.model';
import { getCountryName } from '../../../models/country.model';

// PGDI2026-80 (US66/US21) : le département examine les conventions,
// les valide ou les refuse (motif obligatoire en cas de refus).
@Component({
  selector: 'app-stages-validation',
  imports: [FormsModule, DatePipe],
  templateUrl: './stages-validation.html',
  styleUrl: './stages-validation.css',
})
export class StagesValidation implements OnInit {
  private readonly internshipService = inject(InternshipService);
  private readonly teacherService = inject(TeacherService);

  readonly statusLabels = INTERNSHIP_STATUS_LABELS;
  readonly typeLabels = INTERNSHIP_TYPE_LABELS;
  readonly statuses = Object.keys(INTERNSHIP_STATUS_LABELS) as InternshipStatus[];
  readonly getCountryName = getCountryName;

  readonly internships = signal<Internship[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly notice = signal('');

  // PGDI2026-87 : enseignants actifs proposés comme encadrants, choix par dossier
  readonly teachers = signal<Teacher[]>([]);
  readonly selectedTeacher: Record<number, number | null> = {};

  statusFilter: InternshipStatus | '' = 'PENDING_VALIDATION';

  // Refus en cours de saisie : un seul dossier à la fois
  readonly rejectingId = signal<number | null>(null);
  rejectReason = '';
  readonly rejectError = signal('');

  ngOnInit(): void {
    this.load();
    this.teacherService.getAll(true).subscribe({
      next: (list) => this.teachers.set(list),
      error: () => this.error.set('Liste des enseignants indisponible.'),
    });
  }

  assignSupervisor(item: Internship): void {
    const teacherId = this.selectedTeacher[item.id];
    if (!teacherId) {
      this.error.set('Choisissez un enseignant encadrant.');
      return;
    }
    this.notice.set('');
    this.error.set('');
    this.internshipService.assignSupervisor(item.id, Number(teacherId)).subscribe({
      next: (updated) => {
        this.notice.set(`Encadrant affecté : ${updated.supervisorName}. Le stage est en cours.`);
        this.load();
      },
      error: (err) => this.error.set(err?.error?.message ?? 'Affectation impossible pour le moment.'),
    });
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.internshipService.getAll(this.statusFilter).subscribe({
      next: (list) => {
        this.internships.set(list);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Impossible de charger les stages (accès réservé au département).');
      },
    });
  }

  approve(item: Internship): void {
    this.decide(item, { approved: true });
  }

  startReject(item: Internship): void {
    this.rejectingId.set(item.id);
    this.rejectReason = '';
    this.rejectError.set('');
  }

  cancelReject(): void {
    this.rejectingId.set(null);
  }

  confirmReject(item: Internship): void {
    if (!this.rejectReason.trim()) {
      this.rejectError.set('Indiquez le motif du refus.');
      return;
    }
    this.decide(item, { approved: false, comment: this.rejectReason.trim() });
  }

  openAgreement(item: Internship): void {
    this.internshipService.getAgreement(item.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank');
        setTimeout(() => URL.revokeObjectURL(url), 60_000);
      },
      error: (err) => this.error.set(err?.error?.message ?? 'Convention indisponible.'),
    });
  }

  private decide(item: Internship, decision: { approved: boolean; comment?: string }): void {
    this.notice.set('');
    this.error.set('');
    this.internshipService.validate(item.id, decision).subscribe({
      next: () => {
        this.rejectingId.set(null);
        this.notice.set(decision.approved ? 'Convention validée.' : 'Convention refusée.');
        this.load();
      },
      error: (err) => this.error.set(err?.error?.message ?? 'Décision impossible pour le moment.'),
    });
  }
}
