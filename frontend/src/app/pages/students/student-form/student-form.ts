import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { StudentService } from '../../../services/student.service';
import { Gender, StudentCreate, StudentUpdate } from '../../../models/student.model';

// Un seul composant pour l'ajout (/students/new) et la modification (/students/:id/edit)
@Component({
  selector: 'app-student-form',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './student-form.html',
  styleUrl: './student-form.css'
})
export class StudentForm implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly studentService = inject(StudentService);

  readonly isEdit = signal(false);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');

  private studentId: number | null = null;

  // Compte utilisateur
  firstName = '';
  lastName = '';
  email = '';
  password = '';
  phone = '';
  enabled = true;

  // Informations étudiant
  studentNumber = '';
  cne = '';
  birthDate = '';
  birthPlace = '';
  gender: Gender | '' = '';
  address = '';
  city = '';

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) return;

    const id = Number(idParam);
    if (Number.isNaN(id)) {
      this.error.set('Identifiant invalide.');
      return;
    }

    this.isEdit.set(true);
    this.studentId = id;
    this.loading.set(true);

    this.studentService.getById(id).subscribe({
      next: student => {
        this.firstName = student.firstName;
        this.lastName = student.lastName;
        this.email = student.email;
        this.phone = student.phone ?? '';
        this.enabled = student.enabled;
        this.studentNumber = student.studentNumber;
        this.cne = student.cne ?? '';
        this.birthDate = student.birthDate ?? '';
        this.birthPlace = student.birthPlace ?? '';
        this.gender = student.gender ?? '';
        this.address = student.address ?? '';
        this.city = student.city ?? '';
        this.loading.set(false);
      },
      error: err => {
        console.error('Erreur de chargement:', err);
        this.error.set(
          err.status === 404
            ? 'Cet étudiant n’existe pas.'
            : 'Impossible de charger les données de l’étudiant.'
        );
        this.loading.set(false);
      }
    });
  }

  submitForm(): void {
    this.error.set('');

    if (!this.firstName.trim() || !this.lastName.trim() || !this.studentNumber.trim()) {
      this.error.set('Veuillez remplir tous les champs obligatoires.');
      return;
    }

    if (!this.isEdit()) {
      if (!this.email.trim() || !this.password) {
        this.error.set('Veuillez remplir tous les champs obligatoires.');
        return;
      }
      if (this.password.length < 8) {
        this.error.set('Le mot de passe doit contenir au moins 8 caractères.');
        return;
      }
    }

    this.saving.set(true);

    const common = {
      firstName: this.firstName.trim(),
      lastName: this.lastName.trim(),
      phone: this.phone.trim() || undefined,
      studentNumber: this.studentNumber.trim(),
      cne: this.cne.trim() || undefined,
      birthDate: this.birthDate || undefined,
      birthPlace: this.birthPlace.trim() || undefined,
      gender: this.gender || undefined,
      address: this.address.trim() || undefined,
      city: this.city.trim() || undefined
    };

    const request$ = this.isEdit()
      ? this.studentService.update(this.studentId!, { ...common, enabled: this.enabled } as StudentUpdate)
      : this.studentService.create({ ...common, email: this.email.trim(), password: this.password } as StudentCreate);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.router.navigate(['/students']);
      },
      error: err => {
        console.error('Erreur d’enregistrement:', err);
        this.error.set(this.errorMessage(err));
        this.saving.set(false);
      }
    });
  }

  // Le backend renvoie déjà un message clair pour les doublons (409)
  private errorMessage(err: any): string {
    if (err.status === 409) {
      return err.error?.message ?? 'Cet email, ce numéro étudiant ou ce CNE est déjà utilisé.';
    }
    if (err.status === 400) {
      return 'Certaines informations sont invalides. Vérifiez le formulaire.';
    }
    if (err.status === 401) {
      return 'Votre session a expiré. Veuillez vous reconnecter.';
    }
    if (err.status === 403) {
      return 'Vous ne disposez pas des droits nécessaires.';
    }
    return 'L’enregistrement a échoué.';
  }
}
