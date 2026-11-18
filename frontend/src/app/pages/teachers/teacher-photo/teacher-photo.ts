import { Component, inject, input, OnInit, OnDestroy, signal } from '@angular/core';
import { TeacherPhotoService } from '../../../services/teacher-photo.service';

@Component({
  selector: 'app-teacher-photo',
  standalone: true,
  templateUrl: './teacher-photo.html',
  styleUrl: './teacher-photo.css'
})
export class TeacherPhoto implements OnInit, OnDestroy {
  private readonly photoService = inject(TeacherPhotoService);

  readonly teacherId = input.required<number>();
  readonly initials = input.required<string>();

  readonly photoUrl = signal<string | null>(null);
  readonly previewUrl = signal<string | null>(null);
  readonly selectedFile = signal<File | null>(null);
  readonly uploading = signal(false);
  readonly error = signal('');
  readonly success = signal('');

  ngOnInit(): void {
    this.loadPhoto();
  }

  private loadPhoto(): void {
    this.photoService.getPhoto(this.teacherId()).subscribe({
      next: blob => {
        this.releasePhotoUrl();
        this.photoUrl.set(URL.createObjectURL(blob));
      },
      error: err => {
        if (err.status !== 404) {
          this.error.set('Impossible de charger la photo.');
        }
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    this.error.set('');
    this.success.set('');
    this.selectedFile.set(null);
    this.releasePreviewUrl();

    if (!file) return;

    if (!['image/jpeg', 'image/png'].includes(file.type)) {
      this.error.set('Veuillez choisir une image JPEG ou PNG.');
      input.value = '';
      return;
    }

    if (file.size === 0 || file.size > 2 * 1024 * 1024) {
      this.error.set('La photo doit faire entre 1 octet et 2 Mo.');
      input.value = '';
      return;
    }

    this.selectedFile.set(file);
    this.previewUrl.set(URL.createObjectURL(file));
  }

  upload(): void {
    const file = this.selectedFile();

    if (!file || this.uploading()) return;

    this.uploading.set(true);
    this.error.set('');
    this.success.set('');

    this.photoService.uploadPhoto(this.teacherId(), file).subscribe({
      next: () => {
        this.selectedFile.set(null);
        this.releasePreviewUrl();
        this.success.set('Photo mise a jour avec succes.');
        this.uploading.set(false);
        this.loadPhoto();
      },
      error: err => {
        this.error.set(
          err.status === 403
            ? 'Vous n avez pas le droit de modifier cette photo.'
            : err.status === 413
              ? 'La photo depasse la taille autorisee.'
              : 'Impossible de televerser la photo.'
        );
        this.uploading.set(false);
      }
    });
  }

  private releasePhotoUrl(): void {
    const url = this.photoUrl();
    if (url) URL.revokeObjectURL(url);
    this.photoUrl.set(null);
  }

  private releasePreviewUrl(): void {
    const url = this.previewUrl();
    if (url) URL.revokeObjectURL(url);
    this.previewUrl.set(null);
  }

  ngOnDestroy(): void {
    this.releasePhotoUrl();
    this.releasePreviewUrl();
  }
}