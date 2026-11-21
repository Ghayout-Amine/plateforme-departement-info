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

  readonly statusLabels = INTERNSHIP_STATUS_LABELS;
  readonly typeLabels = INTERNSHIP_TYPE_LABELS;
  readonly statuses = Object.keys(INTERNSHIP_STATUS_LABELS) as InternshipStatus[];

  readonly internships = signal<Internship[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly notice = signal('');

  statusFilter: InternshipStatus | '' = 'PENDING_VALIDATION';

  // Refus en cours de saisie : un seul dossier à la fois
  readonly rejectingId = signal<number | null>(null);
  rejectReason = '';
  readonly rejectError = signal('');

  ngOnInit(): void {
    this.load();
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
