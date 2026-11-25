import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActualiteService } from '../../services/actualite.service';
import { AuthService } from '../../services/auth.service';
import {
  Announcement,
  AnnouncementPriority,
  PRIORITY_LABELS,
  TechArticle
} from '../../models/actualite.model';

// PGDI2026-88 / PGDI2026-89 : page publique /actualites
// - annonces officielles du département (épinglées en tête), gérées par l'admin et le chef de département ;
// - veille technologique externe (Dev.to, Le Monde Informatique).
@Component({
  selector: 'app-actualites',
  standalone: true,
  imports: [FormsModule, DatePipe],
  templateUrl: './actualites.html',
  styleUrl: './actualites.css'
})
export class Actualites implements OnInit {
  private readonly actualiteService = inject(ActualiteService);
  private readonly auth = inject(AuthService);

  readonly priorityLabels = PRIORITY_LABELS;
  readonly priorities = Object.keys(PRIORITY_LABELS) as AnnouncementPriority[];

  readonly canManage = computed(() => {
    const role = this.auth.currentRole();
    return this.auth.isLoggedIn() && (role === 'ADMIN' || role === 'CHEF_DEPARTEMENT');
  });

  // Annonces
  readonly announcements = signal<Announcement[]>([]);
  readonly announcementsLoading = signal(true);
  readonly announcementsError = signal('');
  readonly expanded = signal<Set<number>>(new Set());

  // Formulaire (création ou modification)
  readonly showForm = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly saving = signal(false);
  readonly formError = signal('');
  formTitle = '';
  formContent = '';
  formPriority: AnnouncementPriority = 'NORMALE';
  formPinned = false;

  // Veille
  readonly articles = signal<TechArticle[]>([]);
  readonly unavailableSources = signal<string[]>([]);
  readonly watchLoading = signal(true);
  readonly watchError = signal(false);

  ngOnInit(): void {
    this.loadAnnouncements();
    this.loadTechWatch();
  }

  loadAnnouncements(): void {
    this.announcementsLoading.set(true);
    this.announcementsError.set('');
    this.actualiteService.getAnnouncements().subscribe({
      next: data => {
        this.announcements.set(data);
        this.announcementsLoading.set(false);
      },
      error: err => {
        console.error('Erreur de chargement des annonces:', err);
        this.announcementsError.set('Impossible de charger les annonces du département.');
        this.announcementsLoading.set(false);
      }
    });
  }

  loadTechWatch(): void {
    this.watchLoading.set(true);
    this.watchError.set(false);
    this.actualiteService.getTechWatch(12).subscribe({
      next: data => {
        this.articles.set(data.articles);
        this.unavailableSources.set(data.unavailableSources ?? []);
        this.watchLoading.set(false);
      },
      error: err => {
        console.error('Erreur de chargement de la veille:', err);
        this.watchError.set(true);
        this.watchLoading.set(false);
      }
    });
  }

  // Texte long : on affiche un extrait, « Lire la suite » déplie l'annonce
  isLong(a: Announcement): boolean {
    return a.content.length > 260;
  }

  isExpanded(a: Announcement): boolean {
    return this.expanded().has(a.id);
  }

  toggleExpand(a: Announcement): void {
    this.expanded.update(set => {
      const next = new Set(set);
      next.has(a.id) ? next.delete(a.id) : next.add(a.id);
      return next;
    });
  }

  openCreate(): void {
    this.editingId.set(null);
    this.formTitle = '';
    this.formContent = '';
    this.formPriority = 'NORMALE';
    this.formPinned = false;
    this.formError.set('');
    this.showForm.set(true);
  }

  openEdit(a: Announcement): void {
    this.editingId.set(a.id);
    this.formTitle = a.title;
    this.formContent = a.content;
    this.formPriority = a.priority;
    this.formPinned = a.pinned;
    this.formError.set('');
    this.showForm.set(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.editingId.set(null);
  }

  save(): void {
    this.formError.set('');
    if (!this.formTitle.trim() || !this.formContent.trim()) {
      this.formError.set('Le titre et le contenu sont obligatoires.');
      return;
    }

    const request = {
      title: this.formTitle.trim(),
      content: this.formContent.trim(),
      priority: this.formPriority,
      pinned: this.formPinned
    };
    const id = this.editingId();
    const call$ = id === null
      ? this.actualiteService.createAnnouncement(request)
      : this.actualiteService.updateAnnouncement(id, request);

    this.saving.set(true);
    call$.subscribe({
      next: () => {
        this.saving.set(false);
        this.showForm.set(false);
        this.editingId.set(null);
        this.loadAnnouncements();
      },
      error: err => {
        console.error('Erreur d’enregistrement de l’annonce:', err);
        this.formError.set(this.httpMessage(err, 'La publication a échoué.'));
        this.saving.set(false);
      }
    });
  }

  togglePin(a: Announcement): void {
    this.actualiteService.togglePin(a.id).subscribe({
      next: () => this.loadAnnouncements(),
      error: err => window.alert(this.httpMessage(err, 'L’épinglage a échoué.'))
    });
  }

  remove(a: Announcement): void {
    if (!window.confirm(`Supprimer l’annonce « ${a.title} » ?`)) return;
    this.actualiteService.deleteAnnouncement(a.id).subscribe({
      next: () => this.announcements.update(list => list.filter(x => x.id !== a.id)),
      error: err => window.alert(this.httpMessage(err, 'La suppression a échoué.'))
    });
  }

  // Une image de veille qui ne se charge pas est masquée (la carte garde son bandeau de couleur)
  hideImage(event: Event): void {
    (event.target as HTMLImageElement).style.display = 'none';
  }

  private httpMessage(err: any, fallback: string): string {
    if (err.status === 401) return 'Votre session a expiré. Veuillez vous reconnecter.';
    if (err.status === 403) return 'Vous ne disposez pas des droits nécessaires.';
    if (err.status === 400 && err.error?.errors) return Object.values(err.error.errors).join(' ');
    if (err.error?.message) return err.error.message;
    return fallback;
  }
}
