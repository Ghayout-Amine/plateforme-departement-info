import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TeacherService } from '../../../services/teacher.service';
import { Teacher, TeacherRequest } from '../../../models/teacher.model';

@Component({
  selector: 'app-teacher-edit',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './teacher-edit.html',
  styleUrl: './teacher-edit.css'
})
export class TeacherEdit implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly teacherService = inject(TeacherService);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal('');

  private teacher: Teacher | null = null;

  employeeNumber = '';
  academicTitle = '';
  specialty = '';
  office = '';
  hireDate = '';
  bio = '';

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));

    if (!id || Number.isNaN(id)) {
      this.error.set('Identifiant invalide.');
      this.loading.set(false);
      return;
    }

    this.teacherService.getById(id).subscribe({
      next: teacher => {
        this.teacher = teacher;
        this.employeeNumber = teacher.employeeNumber;
        this.academicTitle = teacher.academicTitle ?? '';
        this.specialty = teacher.specialty ?? '';
        this.office = teacher.office ?? '';
        this.hireDate = teacher.hireDate ?? '';
        this.bio = teacher.bio ?? '';
        this.loading.set(false);
      },
      error: err => {
        console.error('Erreur de chargement:', err);
        this.error.set('Impossible de charger les données de l’enseignant.');
        this.loading.set(false);
      }
    });
  }

  submitForm(): void {
    if (!this.teacher) return;

    if (!this.employeeNumber.trim()) {
      this.error.set('Le matricule est obligatoire.');
      return;
    }

    this.error.set('');
    this.saving.set(true);

    const data: TeacherRequest = {
      employeeNumber: this.employeeNumber.trim(),
      academicTitle: this.academicTitle.trim(),
      specialty: this.specialty.trim(),
      office: this.office.trim(),
      hireDate: this.hireDate || undefined,
      bio: this.bio.trim(),
      userId: this.teacher.userId
    };

    this.teacherService.update(this.teacher.id, data).subscribe({
      next: () => {
        this.saving.set(false);
        this.router.navigate(['/teachers', this.teacher!.id]);
      },
      error: err => {
        console.error('Erreur de modification:', err);
        this.error.set(
          err.status === 409
            ? 'Ce matricule est déjà utilisé par un autre enseignant.'
            : err.status === 403
              ? 'Vous ne disposez pas des droits nécessaires.'
              : 'La modification a échoué.'
        );
        this.saving.set(false);
      }
    });
  }
}
