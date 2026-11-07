import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ContactService } from '../../services/contact.service';

@Component({
  selector: 'app-contact',
  imports: [ReactiveFormsModule],
  templateUrl: './contact.html',
  styleUrl: './contact.css',
})
export class Contact {
  private readonly fb = inject(FormBuilder);
  private readonly contactService = inject(ContactService);

  readonly loading = signal(false);
  readonly succes = signal(false);
  readonly error = signal('');

  readonly form = this.fb.nonNullable.group({
    nom: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    sujet: ['', [Validators.required, Validators.minLength(3)]],
    message: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(1000)]],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.error.set('');
    this.succes.set(false);
    this.loading.set(true);
    this.contactService.envoyer(this.form.getRawValue()).subscribe({
      next: () => {
        this.loading.set(false);
        this.succes.set(true);
        this.form.reset();
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Envoi impossible pour le moment. Veuillez réessayer plus tard.');
      },
    });
  }
}
