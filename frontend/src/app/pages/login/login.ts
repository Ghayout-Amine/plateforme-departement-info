import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { UserRole } from '../../models/auth.model';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly error = signal('');
  readonly loading = signal(false);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.error.set('');
    this.loading.set(true);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.redirectUser(res.role);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(
          err.status === 401
            ? 'Email ou mot de passe incorrect.'
            : 'Serveur injoignable. Vérifiez que le backend est démarré.',
        );
      },
    });
  }

  private redirectUser(role?: UserRole): void {
    // Redirection selon le rôle (extensible quand les modules auront leurs pages dédiées)
    switch (role) {
      case 'ADMIN':
      case 'CHEF_DEPARTEMENT':
      case 'ENSEIGNANT':
      case 'ETUDIANT':
      default:
        this.router.navigate(['/dashboard']);
        break;
    }
  }
}
