import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../../services/user.service';
import { UserResponse, UserRole } from '../../../models/user.model';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-list.html',
  styleUrl: './user-list.css'
})
export class UserList implements OnInit {
  private readonly userService = inject(UserService);

  users = signal<UserResponse[]>([]);
  loading = signal(false);
  errorMsg = signal('');
  successMsg = signal('');

  // Filtres
  searchQuery = '';
  filterRole: UserRole | '' = '';
  filterEnabled: string = '';

  // Création d'utilisateur
  showCreateModal = false;
  newUser = { email: '', password: '', firstName: '', lastName: '', phone: '', role: 'ENSEIGNANT' as UserRole };
  createError = '';

  roles: UserRole[] = ['ADMIN', 'CHEF_DEPARTEMENT', 'ENSEIGNANT', 'ETUDIANT'];

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading.set(true);
    this.userService.getAll().subscribe({
      next: (data) => { this.users.set(data); this.loading.set(false); },
      error: () => { this.errorMsg.set('Erreur lors du chargement des utilisateurs.'); this.loading.set(false); }
    });
  }

  get filteredUsers(): UserResponse[] {
    return this.users().filter(u => {
      const matchSearch = !this.searchQuery ||
        u.firstName.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        u.lastName.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        u.email.toLowerCase().includes(this.searchQuery.toLowerCase());
      const matchRole = !this.filterRole || u.role === this.filterRole;
      const matchEnabled = this.filterEnabled === '' ||
        (this.filterEnabled === 'true' && u.enabled) ||
        (this.filterEnabled === 'false' && !u.enabled);
      return matchSearch && matchRole && matchEnabled;
    });
  }

  toggleStatus(user: UserResponse): void {
    this.userService.toggleStatus(user.id).subscribe({
      next: (updated) => {
        this.users.update(list => list.map(u => u.id === updated.id ? updated : u));
        this.successMsg.set(`Compte ${updated.enabled ? 'activé' : 'désactivé'} avec succès.`);
        setTimeout(() => this.successMsg.set(''), 3000);
      },
      error: () => this.errorMsg.set('Erreur lors de la mise à jour du statut.')
    });
  }

  changeRole(user: UserResponse, event: Event): void {
    const role = (event.target as HTMLSelectElement).value as UserRole;
    this.userService.changeRole(user.id, role).subscribe({
      next: (updated) => {
        this.users.update(list => list.map(u => u.id === updated.id ? updated : u));
        this.successMsg.set('Rôle mis à jour avec succès.');
        setTimeout(() => this.successMsg.set(''), 3000);
      },
      error: () => this.errorMsg.set('Erreur lors du changement de rôle.')
    });
  }

  deleteUser(user: UserResponse): void {
    if (!confirm(`Supprimer ${user.firstName} ${user.lastName} ?`)) return;
    this.userService.delete(user.id).subscribe({
      next: () => {
        this.users.update(list => list.filter(u => u.id !== user.id));
        this.successMsg.set('Utilisateur supprimé avec succès.');
        setTimeout(() => this.successMsg.set(''), 3000);
      },
      error: () => this.errorMsg.set('Erreur lors de la suppression.')
    });
  }

  submitCreate(): void {
    this.createError = '';
    this.userService.create({ ...this.newUser } as any).subscribe({
      next: (created) => {
        this.users.update(list => [created, ...list]);
        this.showCreateModal = false;
        this.newUser = { email: '', password: '', firstName: '', lastName: '', phone: '', role: 'ENSEIGNANT' };
        this.successMsg.set('Utilisateur créé avec succès.');
        setTimeout(() => this.successMsg.set(''), 3000);
      },
      error: (err) => {
        this.createError = err?.error?.message || 'Erreur lors de la création.';
      }
    });
  }

  getRoleBadge(role: UserRole): string {
    const map: Record<UserRole, string> = {
      ADMIN: 'badge-admin',
      CHEF_DEPARTEMENT: 'badge-chef',
      ENSEIGNANT: 'badge-enseignant',
      ETUDIANT: 'badge-etudiant'
    };
    return map[role] || 'badge-default';
  }

  getRoleLabel(role: UserRole): string {
    const map: Record<UserRole, string> = {
      ADMIN: 'Admin',
      CHEF_DEPARTEMENT: 'Chef Dép.',
      ENSEIGNANT: 'Enseignant',
      ETUDIANT: 'Étudiant'
    };
    return map[role] || role;
  }
}
