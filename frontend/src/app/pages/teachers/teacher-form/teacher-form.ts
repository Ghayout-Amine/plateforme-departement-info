import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { UserService } from '../../../services/user.service';
import { TeacherService } from '../../../services/teacher.service';
import { TeacherPhotoService } from '../../../services/teacher-photo.service';
import { UserResponse } from '../../../models/user-create.model';
import { UserCreate } from '../../../models/user-create.model';
import { TeacherRequest } from '../../../models/teacher.model';

@Component({
  selector: 'app-teacher-form',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './teacher-form.html',
  styleUrl: './teacher-form.css'
})
export class TeacherForm {
  private readonly userService = inject(UserService);
  private readonly teacherService = inject(TeacherService);
  private readonly teacherPhotoService = inject(TeacherPhotoService);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly availableUsers = signal<UserResponse[]>([]);
  readonly photoPreviewUrl = signal<string | null>(null);
  selectedPhotoFile: File | null = null;

  firstName = '';
  lastName = '';
  email = '';
  password = '';
  phone = '';

  employeeNumber = '';
  academicTitle = '';
  specialty = '';
  office = '';
  hireDate = '';
  bio = '';

  ngOnInit(): void {
    this.userService.getAll().subscribe({
      next: users => {
        this.availableUsers.set(
          users.filter(user => user.role === 'ENSEIGNANT')
        );
      },
      error: err => {
        console.error('Erreur de chargement des utilisateurs:', err);
      }
    });
  }

  onPhotoFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    if (!['image/jpeg', 'image/png'].includes(file.type)) {
      this.error.set('Format image non supporté. Veuillez choisir un fichier JPEG ou PNG.');
      input.value = '';
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      this.error.set('La photo dépasse 5 Mo. Veuillez choisir une image plus légère (max 5 Mo).');
      this.clearPhoto();
      input.value = '';
      return;
    }

    this.selectedPhotoFile = file;
    this.photoPreviewUrl.set(URL.createObjectURL(file));
  }

  clearPhoto(): void {
    this.selectedPhotoFile = null;
    const url = this.photoPreviewUrl();
    if (url) URL.revokeObjectURL(url);
    this.photoPreviewUrl.set(null);
  }

  submitForm(): void {
    this.error.set('');
    this.success.set('');

    if (!this.firstName.trim() || !this.lastName.trim() ||
        !this.email.trim() || !this.password.trim() ||
        !this.employeeNumber.trim()) {
      this.error.set('Veuillez remplir tous les champs obligatoires.');
      return;
    }

    if (this.selectedPhotoFile && this.selectedPhotoFile.size > 5 * 1024 * 1024) {
      this.error.set('La photo sélectionnée dépasse la limite de 5 Mo. Veuillez en choisir une plus légère.');
      return;
    }

    this.loading.set(true);

    const userData: UserCreate = {
      firstName: this.firstName.trim(),
      lastName: this.lastName.trim(),
      email: this.email.trim(),
      password: this.password,
      phone: this.phone.trim(),
      role: 'ENSEIGNANT'
    };

    this.userService.create(userData).subscribe({
      next: user => {
        const teacherData: TeacherRequest = {
          employeeNumber: this.employeeNumber.trim(),
          academicTitle: this.academicTitle.trim(),
          specialty: this.specialty.trim(),
          office: this.office.trim(),
          hireDate: this.hireDate || undefined,
          bio: this.bio.trim(),
          userId: user.id
        };

        this.teacherService.create(teacherData).subscribe({
          next: (createdTeacher) => {
            if (this.selectedPhotoFile) {
              this.teacherPhotoService.uploadPhoto(createdTeacher.id, this.selectedPhotoFile).subscribe({
                next: () => {
                  this.success.set('Enseignant et photo ajoutés avec succès.');
                  this.loading.set(false);
                  this.router.navigate(['/teachers']);
                },
                error: () => {
                  this.success.set('Enseignant créé avec succès.');
                  this.loading.set(false);
                  this.router.navigate(['/teachers']);
                }
              });
            } else {
              this.success.set('Enseignant ajouté avec succès.');
              this.loading.set(false);
              this.router.navigate(['/teachers']);
            }
          },
          error: err => {
            console.error('Erreur de création du profil:', err);
            this.error.set(
              'Le compte utilisateur a été créé, mais la création du profil enseignant a échoué. Contactez l’administrateur.'
            );
            this.loading.set(false);
          }
        });
      },
      error: err => {
        console.error('Erreur de création du compte:', err);
        this.error.set(
          err.status === 409
            ? 'Cette adresse email existe déjà.'
            : err.status === 403
              ? 'Vous ne disposez pas des droits administrateur.'
              : 'Erreur lors de la création du compte utilisateur.'
        );
        this.loading.set(false);
      }
    });
  }
}
