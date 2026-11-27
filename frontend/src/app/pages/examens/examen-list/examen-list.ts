import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { ExamenService } from '../../../services/examen.service';
import { GroupeService } from '../../../services/groupe.service';
import { SalleService } from '../../../services/salle.service';
import { EXAM_TYPE_LABELS, Examen } from '../../../models/examen.model';
import { Groupe } from '../../../models/groupe.model';
import { Salle } from '../../../models/salle.model';
import { fromIso, hhmm, longDate, today } from '../../../utils/dates';

interface ExamDay {
  date: string;
  label: string;
  examens: Examen[];
}

// PGDI2026-75 (US49) : calendrier des examens, regroupés par jour, filtrables par groupe et par salle
@Component({
  selector: 'app-examen-list',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './examen-list.html',
  styleUrl: './examen-list.css'
})
export class ExamenList implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly examenService = inject(ExamenService);
  private readonly groupeService = inject(GroupeService);
  private readonly salleService = inject(SalleService);

  readonly typeLabels = EXAM_TYPE_LABELS;
  readonly hhmm = hhmm;

  readonly canManage = computed(() => ['ADMIN', 'CHEF_DEPARTEMENT'].includes(this.auth.currentRole() ?? ''));
  readonly hasPersonalView = computed(() => ['ETUDIANT', 'ENSEIGNANT'].includes(this.auth.currentRole() ?? ''));

  readonly groupes = signal<Groupe[]>([]);
  readonly salles = signal<Salle[]>([]);
  readonly examens = signal<Examen[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');

  readonly mine = signal(false);
  readonly upcomingOnly = signal(true);
  readonly groupeId = signal<number | null>(null);
  readonly salleId = signal<number | null>(null);

  readonly days = computed<ExamDay[]>(() => {
    const byDate = new Map<string, Examen[]>();
    this.examens().forEach(e => byDate.set(e.examDate, [...(byDate.get(e.examDate) ?? []), e]));
    return [...byDate.entries()].map(([date, examens]) => ({ date, label: longDate(date), examens }));
  });

  readonly nextExam = computed(() => this.examens().find(e => e.examDate >= today()) ?? null);

  readonly daysUntilNext = computed(() => {
    const next = this.nextExam();
    if (!next) return null;
    return Math.round((fromIso(next.examDate).getTime() - fromIso(today()).getTime()) / 86400000);
  });

  readonly salleCount = computed(() => new Set(this.examens().flatMap(e => e.salleIds)).size);

  ngOnInit(): void {
    this.mine.set(this.hasPersonalView());
    this.groupeService.getAll().subscribe({ next: data => this.groupes.set(data) });
    this.salleService.getAll().subscribe({ next: data => this.salles.set(data) });
    this.load();
  }

  setMine(value: boolean): void {
    this.mine.set(value);
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    const from = this.upcomingOnly() ? today() : undefined;
    const request$ = this.mine()
      ? this.examenService.getMine(from)
      : this.examenService.getAll({
          from,
          groupeId: this.groupeId() ?? undefined,
          salleId: this.salleId() ?? undefined
        });

    request$.subscribe({
      next: data => {
        this.examens.set(data);
        this.loading.set(false);
      },
      error: err => {
        console.error('Erreur de chargement des examens:', err);
        this.error.set(err.status === 401
          ? 'Votre session a expiré. Veuillez vous reconnecter.'
          : 'Impossible de charger le calendrier des examens. Vérifiez le serveur.');
        this.loading.set(false);
      }
    });
  }

  deleteExamen(examen: Examen): void {
    if (!window.confirm(`Supprimer l’examen « ${examen.title} » du ${longDate(examen.examDate)} ?`)) return;
    this.examenService.delete(examen.id).subscribe({
      next: () => this.examens.update(list => list.filter(e => e.id !== examen.id)),
      error: err => window.alert(err.error?.message ?? 'La suppression a échoué.')
    });
  }
}
