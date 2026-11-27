import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { PlanningService } from '../../services/planning.service';
import { GroupeService } from '../../services/groupe.service';
import { SalleService } from '../../services/salle.service';
import { TeacherService } from '../../services/teacher.service';
import { Planning, PlanningView, SESSION_TYPE_LABELS } from '../../models/planning.model';
import { Groupe } from '../../models/groupe.model';
import { Salle } from '../../models/salle.model';
import { Teacher } from '../../models/teacher.model';
import { addDays, fromIso, hhmm, minutes, mondayOf, shortDate, today } from '../../utils/dates';

// Grille de 08:00 à 19:00, 1 minute = 1 pixel
const DAY_START = 8 * 60;
const DAY_END = 19 * 60;
const DAY_NAMES = ['Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi'];

interface PlacedSession {
  session: Planning;
  top: number;
  height: number;
  left: number;   // en %
  width: number;  // en %
}

interface DayColumn {
  label: string;
  date: string;
  isToday: boolean;
  sessions: PlacedSession[];
}

// PGDI2026-75 (US36) : emploi du temps hebdomadaire (par groupe, enseignant, salle, ou "mon emploi du temps")
@Component({
  selector: 'app-emploi-du-temps',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './emploi-du-temps.html',
  styleUrl: './emploi-du-temps.css'
})
export class EmploiDuTemps implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly planningService = inject(PlanningService);
  private readonly groupeService = inject(GroupeService);
  private readonly salleService = inject(SalleService);
  private readonly teacherService = inject(TeacherService);

  readonly typeLabels = SESSION_TYPE_LABELS;
  readonly hours = Array.from({ length: (DAY_END - DAY_START) / 60 + 1 }, (_, i) => 8 + i);
  readonly gridHeight = DAY_END - DAY_START;

  readonly canManage = computed(() => ['ADMIN', 'CHEF_DEPARTEMENT'].includes(this.auth.currentRole() ?? ''));
  readonly hasPersonalView = computed(() => ['ETUDIANT', 'ENSEIGNANT'].includes(this.auth.currentRole() ?? ''));

  readonly view = signal<PlanningView>('groupe');
  readonly selectedId = signal<number | null>(null);
  readonly weekStart = signal(mondayOf(today()));

  readonly groupes = signal<Groupe[]>([]);
  readonly salles = signal<Salle[]>([]);
  readonly teachers = signal<Teacher[]>([]);

  readonly sessions = signal<Planning[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly selected = signal<Planning | null>(null);

  readonly weekLabel = computed(() => {
    const start = this.weekStart();
    return `Semaine du ${shortDate(start)} au ${shortDate(addDays(start, 5))} ${fromIso(start).getFullYear()}`;
  });

  readonly options = computed(() => {
    switch (this.view()) {
      case 'groupe':
        return this.groupes().map(g => ({ id: g.id, label: `${g.name} (${g.filiereCode})` }));
      case 'salle':
        return this.salles().map(s => ({ id: s.id, label: `${s.code} — ${s.name}` }));
      case 'teacher':
        return this.teachers().map(t => ({ id: t.id, label: `${t.firstName} ${t.lastName}` }));
      default:
        return [];
    }
  });

  readonly days = computed<DayColumn[]>(() => {
    const start = this.weekStart();
    const now = today();
    return DAY_NAMES.map((label, index) => {
      const date = addDays(start, index);
      return {
        label,
        date,
        isToday: date === now,
        sessions: this.place(this.sessions().filter(s => s.sessionDate === date))
      };
    });
  });

  readonly activeSessions = computed(() => this.sessions().filter(s => s.status === 'PLANNED'));
  readonly cancelledCount = computed(() => this.sessions().length - this.activeSessions().length);
  readonly totalHours = computed(() =>
    this.activeSessions().reduce((sum, s) => sum + (minutes(s.endTime) - minutes(s.startTime)) / 60, 0)
  );

  printSchedule(): void {
    window.print();
  }

  ngOnInit(): void {
    const query = this.route.snapshot.queryParamMap;
    const semaine = query.get('semaine');
    if (semaine) this.weekStart.set(mondayOf(semaine));

    const vue = query.get('vue') as PlanningView | null;
    const id = Number(query.get('id'));
    if (vue && ['me', 'groupe', 'teacher', 'salle'].includes(vue)) {
      this.view.set(vue);
      this.selectedId.set(id || null);
    } else {
      this.view.set(this.hasPersonalView() ? 'me' : 'groupe');
    }

    this.groupeService.getAll().subscribe({ next: data => { this.groupes.set(data); this.selectFirstIfNeeded(); } });
    this.salleService.getAll().subscribe({ next: data => { this.salles.set(data); this.selectFirstIfNeeded(); } });
    if (this.canManage()) {
      this.teacherService.getAll().subscribe({ next: data => { this.teachers.set(data); this.selectFirstIfNeeded(); } });
    }
    this.load();
  }

  // Quand la liste affichée arrive (groupes, salles ou enseignants), on sélectionne son premier élément
  private selectFirstIfNeeded(): void {
    const first = this.options()[0];
    if (this.view() !== 'me' && !this.selectedId() && first) {
      this.selectedId.set(first.id);
      this.load();
    }
  }

  changeView(view: PlanningView): void {
    this.view.set(view);
    const first = this.options()[0];
    this.selectedId.set(view === 'me' ? null : first?.id ?? null);
    this.load();
  }

  changeSelection(id: number): void {
    this.selectedId.set(Number(id));
    this.load();
  }

  moveWeek(weeks: number): void {
    this.weekStart.set(weeks === 0 ? mondayOf(today()) : addDays(this.weekStart(), 7 * weeks));
    this.load();
  }

  load(): void {
    const view = this.view();
    const id = this.selectedId();
    this.selected.set(null);
    this.error.set('');

    if (view !== 'me' && !id) {
      this.sessions.set([]);
      return;
    }

    const request$ = view === 'me'
      ? this.planningService.getMyWeek(this.weekStart())
      : this.planningService.getWeek(this.weekStart(), {
          groupeId: view === 'groupe' ? id! : undefined,
          teacherId: view === 'teacher' ? id! : undefined,
          salleId: view === 'salle' ? id! : undefined
        });

    this.loading.set(true);
    request$.subscribe({
      next: data => {
        this.sessions.set(data);
        this.loading.set(false);
        this.syncUrl();
      },
      error: err => {
        console.error('Erreur de chargement de l’emploi du temps:', err);
        this.sessions.set([]);
        this.error.set(err.status === 401
          ? 'Votre session a expiré. Veuillez vous reconnecter.'
          : err.error?.message ?? 'Impossible de charger l’emploi du temps. Vérifiez le serveur.');
        this.loading.set(false);
      }
    });
  }

  // Clic sur une case vide : nouvelle séance pré-remplie (date, heure, ressource affichée)
  newAt(day: DayColumn, event: MouseEvent): void {
    if (!this.canManage()) return;
    const minute = DAY_START + Math.floor((event.offsetY) / 30) * 30;
    const start = `${String(Math.floor(minute / 60)).padStart(2, '0')}:${String(minute % 60).padStart(2, '0')}`;
    const queryParams: Record<string, string | number> = { date: day.date, debut: start };
    const id = this.selectedId();
    if (id && this.view() !== 'me') queryParams[this.view()] = id;
    this.router.navigate(['/emploi-du-temps/seances/new'], { queryParams });
  }

  select(session: Planning, event: MouseEvent): void {
    event.stopPropagation();
    this.selected.set(this.selected()?.id === session.id ? null : session);
  }

  cancelSession(session: Planning): void {
    if (!window.confirm(`Annuler la séance ${session.moduleCode} du ${shortDate(session.sessionDate)} ? Le créneau sera libéré.`)) return;
    this.planningService.cancel(session.id).subscribe({
      next: updated => {
        this.sessions.update(list => list.map(s => (s.id === updated.id ? updated : s)));
        this.selected.set(updated);
      },
      error: err => window.alert(err.error?.message ?? 'L’annulation a échoué.')
    });
  }

  deleteSession(session: Planning): void {
    if (!window.confirm(`Supprimer définitivement la séance ${session.moduleCode} du ${shortDate(session.sessionDate)} ?`)) return;
    this.planningService.delete(session.id).subscribe({
      next: () => {
        this.sessions.update(list => list.filter(s => s.id !== session.id));
        this.selected.set(null);
      },
      error: err => window.alert(err.error?.message ?? 'La suppression a échoué.')
    });
  }

  hhmm = hhmm;

  day(session: Planning): string {
    return fromIso(session.sessionDate).toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' });
  }

  /**
   * Position de chaque séance dans sa colonne. Deux séances qui se chevauchent
   * (par ex. une séance annulée et sa remplaçante) sont affichées côte à côte.
   */
  private place(daySessions: Planning[]): PlacedSession[] {
    const sorted = [...daySessions].sort((a, b) => minutes(a.startTime) - minutes(b.startTime));
    const laneEnds: number[] = [];
    const lanes = sorted.map(s => {
      const start = minutes(s.startTime);
      let lane = laneEnds.findIndex(end => end <= start);
      if (lane === -1) {
        lane = laneEnds.length;
        laneEnds.push(0);
      }
      laneEnds[lane] = minutes(s.endTime);
      return lane;
    });
    const laneCount = Math.max(laneEnds.length, 1);
    return sorted.map((session, i) => {
      const start = Math.max(minutes(session.startTime), DAY_START);
      const end = Math.min(minutes(session.endTime), DAY_END);
      return {
        session,
        top: start - DAY_START,
        height: Math.max(end - start, 20),
        left: (100 / laneCount) * lanes[i],
        width: 100 / laneCount
      };
    });
  }

  private syncUrl(): void {
    const queryParams: Record<string, string | number | null> = {
      vue: this.view(),
      id: this.view() === 'me' ? null : this.selectedId(),
      semaine: this.weekStart()
    };
    this.router.navigate([], { relativeTo: this.route, queryParams, replaceUrl: true });
  }
}
