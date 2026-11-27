import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { InternshipService } from '../../../services/internship.service';
import {
  INTERNSHIP_STATUS_LABELS,
  INTERNSHIP_TYPE_LABELS,
  Internship,
  InternshipRequest,
} from '../../../models/internship.model';
import { COUNTRIES, getCountryName } from '../../../models/country.model';

// PGDI2026-80 (US66/US21) : l'étudiant déclare son stage/PFE avec la convention
// et suit la décision du département.
@Component({
  selector: 'app-mes-stages',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './mes-stages.html',
  styleUrl: './mes-stages.css',
})
export class MesStages implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly internshipService = inject(InternshipService);

  readonly statusLabels = INTERNSHIP_STATUS_LABELS;
  readonly typeLabels = INTERNSHIP_TYPE_LABELS;
  readonly countries = COUNTRIES;
  readonly getCountryName = getCountryName;

  readonly internships = signal<Internship[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly fileError = signal('');
  private file: File | null = null;

  readonly form = this.fb.nonNullable.group({
    internshipType: ['PFE', Validators.required],
    subject: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(255)]],
    companyName: ['', [Validators.required, Validators.maxLength(200)]],
    companyCity: [''],
    companyCountry: ['MA', [Validators.required, Validators.pattern(/^[A-Za-z]{2}$/)]],
    startDate: ['', Validators.required],
    endDate: ['', Validators.required],
    companyTutorName: [''],
    companyTutorEmail: ['', Validators.email],
    companyTutorPhone: [''],
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.internshipService.getMine().subscribe({
      next: (list) => {
        this.internships.set(list);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(this.messageFrom(err, 'Impossible de charger vos stages.'));
      },
    });
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const chosen = input.files?.[0] ?? null;
    this.fileError.set('');
    this.file = null;
    if (!chosen) return;
    // Contrôle côté navigateur ; le serveur revérifie (vrai PDF, 5 Mo maximum)
    if (chosen.type !== 'application/pdf' && !chosen.name.toLowerCase().endsWith('.pdf')) {
      this.fileError.set('La convention doit être un fichier PDF.');
      input.value = '';
      return;
    }
    if (chosen.size > 5 * 1024 * 1024) {
      this.fileError.set('La convention dépasse 5 Mo.');
      input.value = '';
      return;
    }
    this.file = chosen;
  }

  submit(): void {
    this.error.set('');
    this.success.set('');
    if (!this.file) {
      this.fileError.set('Joignez la convention au format PDF.');
    }
    if (this.form.invalid || !this.file) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    if (raw.endDate <= raw.startDate) {
      this.error.set('La date de fin doit être après la date de début.');
      return;
    }
    const data: InternshipRequest = {
      ...raw,
      internshipType: raw.internshipType as InternshipRequest['internshipType'],
    };
    this.saving.set(true);
    this.internshipService.declare(data, this.file).subscribe({
      next: () => {
        this.saving.set(false);
        this.success.set('Votre stage est déclaré. Le département va examiner la convention.');
        this.form.reset({ internshipType: 'PFE', companyCountry: 'MA' });
        this.file = null;
        this.load();
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(this.messageFrom(err, 'Déclaration impossible pour le moment.'));
      },
    });
  }

  openAgreement(item: Internship): void {
    this.internshipService.getAgreement(item.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank');
        setTimeout(() => URL.revokeObjectURL(url), 60_000);
      },
      error: (err) => this.error.set(this.messageFrom(err, 'Convention indisponible.')),
    });
  }

  private messageFrom(err: { error?: { message?: string } }, fallback: string): string {
    return err?.error?.message ?? fallback;
  }
}
