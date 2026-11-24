import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { PlanningService } from '../../../services/planning.service';
import { GroupeService } from '../../../services/groupe.service';
import { SalleService } from '../../../services/salle.service';
import { TeacherService } from '../../../services/teacher.service';
import { PlanningRequest, SESSION_TYPE_LABELS, SESSION_TYPES, SessionType } from '../../../models/planning.model';
import { Groupe } from '../../../models/groupe.model';
import { Salle } from '../../../models/salle.model';
import { Teacher } from '../../../models/teacher.model';
import { ModuleCours } from '../../../models/module.model';
import { hhmm, longDate, minutes, today } from '../../../utils/dates';

// PGDI2026-75 (US36) : planifier (/emploi-du-temps/seances/new) ou modifier (/emploi-du-temps/seances/:id/edit) une séance
@Component({
  selector: 'app-seance-form',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './seance-form.html',
  styleUrl: './seance-form.css'
})
export class SeanceForm implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly planningService = inject(PlanningService);
  private readonly groupeService = inject(GroupeService);
  private readonly salleService = inject(SalleService);
  private readonly teacherService = inject(TeacherService);

  readonly sessionTypes = SESSION_TYPES;
  readonly typeLabels = SESSION_TYPE_LABELS;

  readonly groupes = signal<Groupe[]>([]);
  readonly modules = signal<ModuleCours[]>([]);
  readonly teachers = signal<Teacher[]>([]);
  readonly salles = signal<Salle[]>([]);

  readonly isEdit = signal(false);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');

  private planningId: number | null = null;

  // Champs du formulaire (signaux : les listes et les avertissements en dépendent)
  readonly sessionDate = signal(today());
  readonly startTime = signal('08:30');
  readonly endTime = signal('10:30');
  readonly sessionType = signal<SessionType>('CM');
  readonly groupeId = signal<number | null>(null);
  readonly moduleId = signal<number | null>(null);
  readonly teacherId = signal<number | null>(null);
  readonly salleId = signal<number | null>(null);
  readonly weeks = signal(1);
  notes = '';

  readonly groupe = computed(() => this.groupes().find(g => g.id === this.groupeId()) ?? null);

  // Seuls les modules de la filière du groupe sont proposés
  readonly groupeModules = computed(() => {
    const groupe = this.groupe();
    return groupe ? this.modules().filter(m => m.filiereId === groupe.filiereId) : [];
  });

  readonly activeSalles = computed(() => this.salles().filter(s => s.isActive || s.id === this.salleId()));

  readonly capacityWarning = computed(() => {
    const salle = this.salles().find(s => s.id === this.salleId());
    const groupe = this.groupe();
    if (!salle || !groupe?.capacity || salle.capacity >= groupe.capacity) return '';
    return `${salle.code} n’a que ${salle.capacity} places pour ${groupe.capacity} étudiants : le serveur refusera cette salle.`;
  });

  readonly timeWarning = computed(() =>
    this.startTime() && this.endTime() && minutes(this.endTime()) <= minutes(this.startTime())
      ? 'L’heure de fin doit être après l’heure de début.'
      : ''
  );

  readonly dateLabel = computed(() => (this.sessionDate() ? longDate(this.sessionDate()) : ''));

  ngOnInit(): void {
    this.groupeService.getAll().subscribe({ next: data => this.groupes.set(data) });
    this.planningService.getModules().subscribe({ next: data => this.modules.set(data) });
    // PGDI2026-92 : seuls les enseignants actifs peuvent recevoir une séance
    this.teacherService.getAll(true).subscribe({ next: data => this.teachers.set(data) });
    this.salleService.getAll().subscribe({ next: data => this.salles.set(data) });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) {
      this.prefillFromQuery();
      return;
    }

    this.isEdit.set(true);
    this.planningId = Number(idParam);
    this.loading.set(true);
    this.planningService.getById(this.planningId).subscribe({
      next: p => {
        this.sessionDate.set(p.sessionDate);
        this.startTime.set(hhmm(p.startTime));
        this.endTime.set(hhmm(p.endTime));
        this.sessionType.set(p.sessionType);
        this.groupeId.set(p.groupeId);
        this.moduleId.set(p.moduleId);
        this.teacherId.set(p.teacherId);
        this.salleId.set(p.salleId);
        this.notes = p.notes ?? '';
        this.loading.set(false);
      },
      error: err => {
        this.error.set(err.status === 404 ? 'Cette séance n’existe pas.' : 'Impossible de charger la séance.');
        this.loading.set(false);
      }
    });
  }

  // Depuis un clic dans la grille : ?date=...&debut=...&groupe=... (ou teacher / salle)
  private prefillFromQuery(): void {
    const q = this.route.snapshot.queryParamMap;
    if (q.get('date')) this.sessionDate.set(q.get('date')!);
    const debut = q.get('debut');
    if (debut) {
      this.startTime.set(debut);
      const end = minutes(debut) + 120;
      this.endTime.set(`${String(Math.floor(end / 60)).padStart(2, '0')}:${String(end % 60).padStart(2, '0')}`);
    }
    if (q.get('groupe')) this.groupeId.set(Number(q.get('groupe')));
    if (q.get('teacher')) this.teacherId.set(Number(q.get('teacher')));
    if (q.get('salle')) this.salleId.set(Number(q.get('salle')));
  }

  onGroupeChange(id: number | null): void {
    this.groupeId.set(id);
    if (!this.groupeModules().some(m => m.id === this.moduleId())) {
      this.moduleId.set(null);
    }
  }

  // Le module propose son enseignant responsable (module.enseignantId = compte utilisateur)
  onModuleChange(id: number | null): void {
    this.moduleId.set(id);
    const module = this.modules().find(m => m.id === id);
    const teacher = this.teachers().find(t => t.userId === module?.enseignantId);
    if (teacher) this.teacherId.set(teacher.id);
  }

  submitForm(): void {
    this.error.set('');
    if (!this.groupeId() || !this.moduleId() || !this.teacherId() || !this.salleId() || !this.sessionDate()) {
      this.error.set('Veuillez renseigner la date, le groupe, le module, l’enseignant et la salle.');
      return;
    }
    if (this.timeWarning()) {
      this.error.set(this.timeWarning());
      return;
    }

    const payload: PlanningRequest = {
      teacherId: this.teacherId()!,
      moduleId: this.moduleId()!,
      groupeId: this.groupeId()!,
      salleId: this.salleId()!,
      sessionType: this.sessionType(),
      sessionDate: this.sessionDate(),
      startTime: this.startTime(),
      endTime: this.endTime(),
      notes: this.notes.trim() || undefined,
      weeks: this.isEdit() ? undefined : Number(this.weeks()) || 1
    };

    // Une série renvoie une liste de séances, une séance seule un objet : seul le succès compte ici
    const request$: Observable<unknown> = this.isEdit()
      ? this.planningService.update(this.planningId!, payload)
      : (payload.weeks ?? 1) > 1
        ? this.planningService.createSerie(payload)
        : this.planningService.create(payload);

    this.saving.set(true);
    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.router.navigate(['/emploi-du-temps'], {
          queryParams: { vue: 'groupe', id: payload.groupeId, semaine: payload.sessionDate }
        });
      },
      error: err => {
        console.error('Erreur d’enregistrement de la séance:', err);
        this.error.set(this.errorMessage(err));
        this.saving.set(false);
      }
    });
  }

  private errorMessage(err: any): string {
    if (err.status === 400 && err.error?.errors) {
      return Object.values(err.error.errors).join(' ');
    }
    if ([400, 404, 409].includes(err.status) && err.error?.message) {
      return err.error.message;
    }
    if (err.status === 401) return 'Votre session a expiré. Veuillez vous reconnecter.';
    if (err.status === 403) return 'Seuls l’administrateur et le chef de département peuvent planifier des séances.';
    return 'L’enregistrement a échoué.';
  }
}
