import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TeacherService } from '../../../services/teacher.service';
import { Teacher } from '../../../models/teacher.model';

@Component({
  selector: 'app-teacher-list',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './teacher-list.html',
  styleUrl: './teacher-list.css'
})
export class TeacherList implements OnInit {
  private readonly teacherService = inject(TeacherService);

  readonly teachers = signal<Teacher[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly searchTerm = signal('');

  readonly filteredTeachers = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();

    if (!term) return this.teachers();

    return this.teachers().filter(teacher =>
      [
        teacher.firstName,
        teacher.lastName,
        teacher.employeeNumber,
        teacher.specialty,
        teacher.userEmail
      ].some(value => value?.toLowerCase().includes(term))
    );
  });

  readonly specialtiesCount = computed(() =>
    new Set(
      this.teachers()
        .map(teacher => teacher.specialty?.trim())
        .filter(Boolean)
    ).size
  );

  ngOnInit(): void {
    this.loadTeachers();
  }

  loadTeachers(): void {
    this.loading.set(true);
    this.error.set('');

    this.teacherService.getAll().subscribe({
      next: data => {
        this.teachers.set(data);
        this.loading.set(false);
      },
      error: err => {
        console.error('Erreur de chargement des enseignants:', err);
        this.error.set(
          err.status === 401
            ? 'Votre session a expiré. Veuillez vous reconnecter.'
            : err.status === 403
              ? 'Vous ne disposez pas des droits nécessaires.'
              : 'Impossible de charger les enseignants. Vérifiez le serveur.'
        );
        this.loading.set(false);
      }
    });
  }

  deleteTeacher(teacher: Teacher): void {
    const confirmed = window.confirm(
      `Voulez-vous vraiment supprimer ${teacher.firstName} ${teacher.lastName} ?`
    );

    if (!confirmed) return;

    this.teacherService.delete(teacher.id).subscribe({
      next: () => {
        this.teachers.update(list =>
          list.filter(item => item.id !== teacher.id)
        );
      },
      error: err => {
        console.error('Erreur de suppression:', err);
        window.alert('La suppression a échoué.');
      }
    });
  }

  getInitials(teacher: Teacher): string {
    return `${teacher.firstName?.charAt(0) ?? ''}${teacher.lastName?.charAt(0) ?? ''}`.toUpperCase();
  }
}
