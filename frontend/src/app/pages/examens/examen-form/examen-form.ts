import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ExamenService } from '../../../services/examen.service';
import { PlanningService } from '../../../services/planning.service';
import { GroupeService } from '../../../services/groupe.service';
import { SalleService } from '../../../services/salle.service';
import { EXAM_TYPE_LABELS, EXAM_TYPES, ExamType, ExamenRequest } from '../../../models/examen.model';
import { Groupe } from '../../../models/groupe.model';
import { Salle } from '../../../models/salle.model';
import { ModuleCours } from '../../../models/module.model';
import { hhmm, longDate, minutes, today } from '../../../utils/dates';

// PGDI2026-75 (US49) : planifier (/examens/new) ou modifier (/examens/:id/edit) une session d'examen
@Component({
  selector: 'app-examen-form',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './examen-form.html',
  styleUrl: '../../emploi-du-temps/seance-form/seance-form.css'
})
export class ExamenForm implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly examenService = inject(ExamenService);
  private readonly planningService = inject(PlanningService);
  private readonly groupeService = inject(GroupeService);
  private readonly salleService = inject(SalleService);

  readonly examTypes = EXAM_TYPES;
  readonly typeLabels = EXAM_TYPE_LABELS;

  readonly groupes = signal<Groupe[]>([]);
  readonly modules = signal<ModuleCours[]>([]);
  readonly salles = signal<Salle[]>([]);

  readonly isEdit = signal(false);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  private examenId: number | null = null;

  title = '';
  notes = '';
  readonly examType = signal<ExamType>('CC');
  readonly examDate = signal(today());
  readonly startTime = signal('09:00');
  readonly endTime = signal('11:00');
  readonly groupeId = signal<number | null>(null);
  readonly moduleId = signal<number | null>(null);
  readonly salleIds = signal<number[]>([]);

  readonly groupe = computed(() => this.groupes().find(g => g.id === this.groupeId()) ?? null);
  readonly groupeModules = computed(() => {
    const groupe = this.groupe();
    return groupe ? this.modules().filter(m => m.filiereId === groupe.filiereId) : [];
  });
  readonly activeSalles = computed(() => this.salles().filter(s => s.isActive || this.salleIds().includes(s.id)));

  // Places réunies par les salles cochées, comparées à l'effectif du groupe
  readonly places = computed(() =>
    this.salles().filter(s => this.salleIds().includes(s.id)).reduce((sum, s) => sum + s.capacity, 0)
  );
  readonly enoughPlaces = computed(() => !this.groupe()?.capacity || this.places() >= this.groupe()!.capacity!);
  readonly dateLabel = computed(() => (this.examDate() ? longDate(this.examDate()) : ''));

  ngOnInit(): void {
    this.groupeService.getAll().subscribe({ next: data => this.groupes.set(data) });
    this.planningService.getModules().subscribe({ next: data => this.modules.set(data) });
    this.salleService.getAll().subscribe({ next: data => this.salles.set(data) });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) return;

    this.isEdit.set(true);
    this.examenId = Number(idParam);
    this.loading.set(true);
    this.examenService.getById(this.examenId).subscribe({
      next: e => {
        this.title = e.title;
        this.notes = e.notes ?? '';
        this.examType.set(e.examType);
        this.examDate.set(e.examDate);
        this.startTime.set(hhmm(e.startTime));
        this.endTime.set(hhmm(e.endTime));
        this.groupeId.set(e.groupeId);
        this.moduleId.set(e.moduleId);
        this.salleIds.set([...e.salleIds]);
        this.loading.set(false);
      },
      error: err => {
        this.error.set(err.status === 404 ? 'Cet examen n’existe pas.' : 'Impossible de charger l’examen.');
        this.loading.set(false);
      }
    });
  }

  onGroupeChange(id: number | null): void {
    this.groupeId.set(id);
    if (!this.groupeModules().some(m => m.id === this.moduleId())) this.moduleId.set(null);
  }

  // Titre proposé automatiquement : « Examen final Génie Logiciel »
  onModuleChange(id: number | null): void {
    this.moduleId.set(id);
    const module = this.modules().find(m => m.id === id);
    if (module && !this.title.trim()) this.title = `${this.typeLabels[this.examType()]} ${module.nom}`;
  }

  toggleSalle(id: number, checked: boolean): void {
    this.salleIds.update(ids => (checked ? [...ids, id] : ids.filter(x => x !== id)));
  }

  submitForm(): void {
    this.error.set('');
    if (!this.title.trim() || !this.groupeId() || !this.moduleId() || !this.examDate()) {
      this.error.set('Veuillez renseigner le titre, la date, le groupe et le module.');
      return;
    }
    if (this.salleIds().length === 0) {
      this.error.set('Choisissez au moins une salle.');
      return;
    }
    if (minutes(this.endTime()) <= minutes(this.startTime())) {
      this.error.set('L’heure de fin doit être après l’heure de début.');
      return;
    }

    const payload: ExamenRequest = {
      title: this.title.trim(),
      examType: this.examType(),
      moduleId: this.moduleId()!,
      groupeId: this.groupeId()!,
      salleIds: this.salleIds(),
      examDate: this.examDate(),
      startTime: this.startTime(),
      endTime: this.endTime(),
      notes: this.notes.trim() || undefined
    };

    const request$ = this.isEdit()
      ? this.examenService.update(this.examenId!, payload)
      : this.examenService.create(payload);

    this.saving.set(true);
    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.router.navigate(['/examens']);
      },
      error: err => {
        console.error('Erreur d’enregistrement de l’examen:', err);
        this.error.set(
          err.status === 400 && err.error?.errors ? Object.values(err.error.errors).join(' ')
            : [400, 404, 409].includes(err.status) && err.error?.message ? err.error.message
            : err.status === 403 ? 'Seuls l’administrateur et le chef de département peuvent planifier des examens.'
            : 'L’enregistrement a échoué.'
        );
        this.saving.set(false);
      }
    });
  }
}
