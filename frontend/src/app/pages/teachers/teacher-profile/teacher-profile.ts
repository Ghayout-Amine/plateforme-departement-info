import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TeacherService } from '../../../services/teacher.service';
import { Teacher } from '../../../models/teacher.model';

@Component({
  selector: 'app-teacher-profile',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './teacher-profile.html',
  styleUrl: './teacher-profile.css'
})
export class TeacherProfile implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly teacherService = inject(TeacherService);

  readonly teacher = signal<Teacher | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));

    if (!id || Number.isNaN(id)) {
      this.error.set('Identifiant de l’enseignant invalide.');
      this.loading.set(false);
      return;
    }

    this.teacherService.getById(id).subscribe({
      next: data => {
        this.teacher.set(data);
        this.loading.set(false);
      },
      error: err => {
        console.error('Erreur de chargement:', err);
        this.error.set('Impossible de charger le profil de cet enseignant.');
        this.loading.set(false);
      }
    });
  }

  getInitials(teacher: Teacher): string {
    return `${teacher.firstName?.charAt(0) ?? ''}${teacher.lastName?.charAt(0) ?? ''}`.toUpperCase();
  }
}
