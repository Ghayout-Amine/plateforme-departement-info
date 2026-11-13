import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { StudentService } from '../../../services/student.service';
import { Student } from '../../../models/student.model';

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './student-list.html',
  styleUrl: './student-list.css'
})
export class StudentList implements OnInit {
  private readonly studentService = inject(StudentService);
  private readonly search$ = new Subject<string>();

  readonly students = signal<Student[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly searchTerm = signal('');

  // Pagination (gérée par le backend)
  readonly page = signal(0);
  readonly size = 10;
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);

  ngOnInit(): void {
    // On attend que l'utilisateur arrête de taper avant d'appeler l'API
    this.search$
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => {
        this.page.set(0);
        this.loadStudents();
      });

    this.loadStudents();
  }

  onSearch(term: string): void {
    this.searchTerm.set(term);
    this.search$.next(term);
  }

  loadStudents(): void {
    this.loading.set(true);
    this.error.set('');

    this.studentService.getPage(this.searchTerm(), this.page(), this.size).subscribe({
      next: data => {
        this.students.set(data.content);
        this.totalElements.set(data.page.totalElements);
        this.totalPages.set(data.page.totalPages);
        this.loading.set(false);
      },
      error: err => {
        console.error('Erreur de chargement des étudiants:', err);
        this.error.set(
          err.status === 401
            ? 'Votre session a expiré. Veuillez vous reconnecter.'
            : err.status === 403
              ? 'Vous ne disposez pas des droits nécessaires.'
              : 'Impossible de charger les étudiants. Vérifiez le serveur.'
        );
        this.loading.set(false);
      }
    });
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.page.set(page);
    this.loadStudents();
  }

  deleteStudent(student: Student): void {
    const confirmed = window.confirm(
      `Voulez-vous vraiment supprimer ${student.firstName} ${student.lastName} ? Son compte utilisateur sera aussi supprimé.`
    );

    if (!confirmed) return;

    this.studentService.delete(student.id).subscribe({
      next: () => {
        // Si on supprime le dernier étudiant de la page, on revient à la page précédente
        if (this.students().length === 1 && this.page() > 0) {
          this.page.update(p => p - 1);
        }
        this.loadStudents();
      },
      error: err => {
        console.error('Erreur de suppression:', err);
        window.alert('La suppression a échoué.');
      }
    });
  }

  getInitials(student: Student): string {
    return `${student.firstName?.charAt(0) ?? ''}${student.lastName?.charAt(0) ?? ''}`.toUpperCase();
  }

  genderLabel(student: Student): string {
    return student.gender === 'FEMALE' ? 'Femme' : student.gender === 'MALE' ? 'Homme' : '—';
  }
}
