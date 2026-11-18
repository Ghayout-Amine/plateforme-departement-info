import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../../services/user.service';
import { UserResponse, UpdateProfile, ChangePassword } from '../../../models/user.model';

@Component({
  selector: 'app-user-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-profile.html',
  styleUrl: './user-profile.css'
})
export class UserProfile implements OnInit {
  private readonly userService = inject(UserService);

  profile = signal<UserResponse | null>(null);
  loading = signal(true);
  successMsg = signal('');
  errorMsg = signal('');

  // Onglets
  activeTab: 'profile' | 'security' = 'profile';

  // Formulaire profil
  profileForm: UpdateProfile = { firstName: '', lastName: '', phone: '', avatarUrl: '' };

  // Formulaire mot de passe
  passwordForm: ChangePassword = { oldPassword: '', newPassword: '' };
  confirmPassword = '';
  passwordError = '';

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.userService.getMyProfile().subscribe({
      next: (data) => {
        this.profile.set(data);
        this.profileForm = {
          firstName: data.firstName,
          lastName: data.lastName,
          phone: data.phone || '',
          avatarUrl: data.avatarUrl || ''
        };
        this.loading.set(false);
      },
      error: () => { this.errorMsg.set('Erreur lors du chargement du profil.'); this.loading.set(false); }
    });
  }

  saveProfile(): void {
    this.errorMsg.set('');
    this.userService.updateMyProfile(this.profileForm).subscribe({
      next: (updated) => {
        this.profile.set(updated);
        this.successMsg.set('Profil mis à jour avec succès !');
        setTimeout(() => this.successMsg.set(''), 3000);
      },
      error: () => this.errorMsg.set('Erreur lors de la mise à jour du profil.')
    });
  }

  onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.errorMsg.set('');
    this.successMsg.set('');

    if (!['image/jpeg', 'image/png'].includes(file.type)) {
      this.errorMsg.set('Veuillez choisir une image JPEG ou PNG.');
      input.value = '';
      return;
    }

    if (file.size === 0 || file.size > 5 * 1024 * 1024) {
      this.errorMsg.set('La photo doit faire au maximum 5 Mo.');
      input.value = '';
      return;
    }

    this.userService.uploadProfilePhoto(file).subscribe({
      next: () => {
        this.successMsg.set('Photo de profil mise à jour avec succès !');
        this.loadProfile();
        input.value = '';
        setTimeout(() => this.successMsg.set(''), 4000);
      },
      error: (err) => {
        this.errorMsg.set(
          err.status === 413
            ? 'La photo dépasse la taille autorisée (2 Mo).'
            : 'Impossible de téléverser la photo.'
        );
        input.value = '';
      }
    });
  }

  savePassword(): void {
    this.passwordError = '';
    this.errorMsg.set('');

    if (this.passwordForm.newPassword !== this.confirmPassword) {
      this.passwordError = 'Les mots de passe ne correspondent pas.';
      return;
    }
    if (this.passwordForm.newPassword.length < 6) {
      this.passwordError = 'Le nouveau mot de passe doit contenir au moins 6 caractères.';
      return;
    }

    this.userService.changePassword(this.passwordForm).subscribe({
      next: () => {
        this.successMsg.set('Mot de passe modifié avec succès !');
        this.passwordForm = { oldPassword: '', newPassword: '' };
        this.confirmPassword = '';
        setTimeout(() => this.successMsg.set(''), 3000);
      },
      error: (err) => this.errorMsg.set(err?.error?.message || 'Erreur lors du changement de mot de passe.')
    });
  }

  getRoleLabel(role: string): string {
    const map: Record<string, string> = {
      ADMIN: '🔑 Administrateur',
      CHEF_DEPARTEMENT: '🏛️ Chef de Département',
      ENSEIGNANT: '👨‍🏫 Enseignant',
      ETUDIANT: '🎓 Étudiant'
    };
    return map[role] || role;
  }

  getInitials(): string {
    const p = this.profile();
    if (!p) return '?';
    return `${p.firstName[0]}${p.lastName[0]}`.toUpperCase();
  }
}
