import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { Filiere, libelleNiveau, semestresDuNiveau } from '../../../core/models/filiere.model';
import { ModuleCours, numeroSemestre } from '../../../core/models/module-cours.model';
import { FiliereApi } from '../../../core/services/filiere-api';
import { ModuleCoursApi } from '../../../core/services/module-cours-api';
import { messageErreur } from '../../../core/services/api-error';
import { FiliereForm } from './filiere-form/filiere-form';
import { ModuleForm } from './module-form/module-form';
import { ConfirmDialog } from './confirm-dialog/confirm-dialog';

interface GroupeSemestre {
  semestre: string;
  modules: ModuleCours[];
  totalHeures: number;
}

type Suppression = { type: 'filiere'; filiere: Filiere } | { type: 'module'; module: ModuleCours };

@Component({
  selector: 'app-filieres-modules-page',
  imports: [FiliereForm, ModuleForm, ConfirmDialog],
  templateUrl: './filieres-modules-page.html',
  styleUrls: ['../../../shared/styles/admin-ui.css', './filieres-modules-page.css'],
})
export class FilieresModulesPage implements OnInit {
  private filiereApi = inject(FiliereApi);
  private moduleApi = inject(ModuleCoursApi);

  // --- état ---
  filieres = signal<Filiere[]>([]);
  modules = signal<ModuleCours[]>([]); // tous les modules, filtrés côté client
  chargement = signal(true);
  erreurChargement = signal<string | null>(null);
  recherche = signal('');
  selectionId = signal<number | null>(null);
  semestreActif = signal('S1');

  // dialogues : undefined = fermé, null = création, objet = modification
  filiereEnEdition = signal<Filiere | null | undefined>(undefined);
  moduleEnEdition = signal<ModuleCours | null | undefined>(undefined);
  semestreNouveauModule = signal('S1');
  aSupprimer = signal<Suppression | null>(null);
  suppressionEnCours = signal(false);

  notification = signal<string | null>(null);
  notificationErreur = signal(false);
  private timerNotif?: ReturnType<typeof setTimeout>;

  // --- valeurs dérivées ---
  filieresFiltrees = computed(() => {
    const q = this.recherche().trim().toLowerCase();
    if (!q) return this.filieres();
    return this.filieres().filter(
      (f) => f.code.toLowerCase().includes(q) || f.nom.toLowerCase().includes(q),
    );
  });

  filiereSelectionnee = computed(
    () => this.filieres().find((f) => f.id === this.selectionId()) ?? null,
  );

  modulesSelection = computed(() =>
    this.modules().filter((m) => m.filiereId === this.selectionId()),
  );

  codesFilieres = computed(() => this.filieres().map((f) => f.code));
  codesModules = computed(() => this.modules().map((m) => m.code));

  groupes = computed<GroupeSemestre[]>(() => {
    const f = this.filiereSelectionnee();
    if (!f) return [];
    const mods = this.modulesSelection();
    // semestres prévus par le niveau + ceux déjà utilisés en base
    const semestres = new Set([...semestresDuNiveau(f.niveau), ...mods.map((m) => m.semestre)]);
    return [...semestres]
      .sort((a, b) => numeroSemestre(a) - numeroSemestre(b))
      .map((semestre) => {
        const duSemestre = mods
          .filter((m) => m.semestre === semestre)
          .sort((a, b) => a.code.localeCompare(b.code));
        return {
          semestre,
          modules: duSemestre,
          totalHeures: duSemestre.reduce((t, m) => t + (m.volumeHoraire ?? 0), 0),
        };
      });
  });

  /** Semestre affiché sous la frise */
  groupeActif = computed(
    () =>
      this.groupes().find((g) => g.semestre === this.semestreActif()) ?? this.groupes()[0] ?? null,
  );

  /** Total des heures du département (en-tête) */
  totalHeuresDepartement = computed(() =>
    this.modules().reduce((t, m) => t + (m.volumeHoraire ?? 0), 0),
  );

  totalHeures = computed(() =>
    this.modulesSelection().reduce((t, m) => t + (m.volumeHoraire ?? 0), 0),
  );

  libelleNiveau = libelleNiveau;

  ngOnInit(): void {
    this.charger();
  }

  selectionner(id: number): void {
    if (id !== this.selectionId()) this.semestreActif.set('S1');
    this.selectionId.set(id);
  }

  /** Position du semestre dans la frise : actif, rempli (a des modules) ou vide */
  etatSemestre(g: GroupeSemestre): 'actif' | 'rempli' | 'vide' {
    if (g.semestre === this.groupeActif()?.semestre) return 'actif';
    return g.modules.length > 0 ? 'rempli' : 'vide';
  }

  nombreModules(filiereId: number): number {
    return this.modules().filter((m) => m.filiereId === filiereId).length;
  }

  // --- chargement ---
  charger(idASelectionner?: number): void {
    this.chargement.set(true);
    this.erreurChargement.set(null);
    forkJoin({ filieres: this.filiereApi.getAll(), modules: this.moduleApi.getAll() }).subscribe({
      next: ({ filieres, modules }) => {
        this.filieres.set([...filieres].sort((a, b) => a.code.localeCompare(b.code)));
        this.modules.set(modules);
        this.chargement.set(false);
        const triees = this.filieres();
        // par défaut : la première filière qui a des modules (sinon la première tout court)
        const parDefaut =
          triees.find((f) => modules.some((m) => m.filiereId === f.id)) ?? triees[0];
        const id = idASelectionner ?? this.selectionId() ?? parDefaut?.id;
        this.selectionId.set(
          id !== undefined && triees.some((f) => f.id === id) ? id : (parDefaut?.id ?? null),
        );
      },
      error: (err) => {
        this.erreurChargement.set(messageErreur(err, 'Impossible de charger les filières.'));
        this.chargement.set(false);
      },
    });
  }

  // --- filières ---
  filiereEnregistree(f: Filiere): void {
    const creation = this.filiereEnEdition() === null;
    this.filiereEnEdition.set(undefined);
    this.notifier(creation ? `Filière ${f.code} créée.` : `Filière ${f.code} modifiée.`);
    this.charger(f.id);
  }

  demanderSuppressionFiliere(f: Filiere): void {
    const n = this.nombreModules(f.id);
    if (n > 0) {
      this.notifier(
        `La filière ${f.code} contient ${n} module${n > 1 ? 's' : ''}. Supprimez-${n > 1 ? 'les' : 'le'} d’abord.`,
        true,
      );
      return;
    }
    this.aSupprimer.set({ type: 'filiere', filiere: f });
  }

  // --- modules ---
  ouvrirNouveauModule(semestre = this.groupeActif()?.semestre ?? 'S1'): void {
    this.semestreNouveauModule.set(semestre);
    this.moduleEnEdition.set(null);
  }

  moduleEnregistre(m: ModuleCours): void {
    const creation = this.moduleEnEdition() === null;
    this.moduleEnEdition.set(undefined);
    this.notifier(creation ? `Module ${m.code} ajouté.` : `Module ${m.code} modifié.`);
    this.semestreActif.set(m.semestre); // on montre le semestre où le module a été rangé
    this.charger(m.filiereId);
  }

  // --- suppression ---
  messageSuppression(s: Suppression): string {
    return s.type === 'filiere'
      ? `La filière ${s.filiere.code} (${s.filiere.nom}) sera supprimée définitivement.`
      : `Le module ${s.module.code} (${s.module.nom}) sera supprimé définitivement.`;
  }

  confirmerSuppression(): void {
    const s = this.aSupprimer();
    if (!s) return;
    this.suppressionEnCours.set(true);
    const requete =
      s.type === 'filiere'
        ? this.filiereApi.delete(s.filiere.id)
        : this.moduleApi.delete(s.module.id);

    requete.subscribe({
      next: () => {
        this.suppressionEnCours.set(false);
        this.aSupprimer.set(null);
        if (s.type === 'filiere') {
          this.notifier(`Filière ${s.filiere.code} supprimée.`);
          this.selectionId.set(null);
          this.charger();
        } else {
          this.notifier(`Module ${s.module.code} supprimé.`);
          this.charger(s.module.filiereId);
        }
      },
      error: (err) => {
        this.suppressionEnCours.set(false);
        this.aSupprimer.set(null);
        this.notifier(
          messageErreur(
            err,
            'Suppression refusée : l’élément est peut-être encore utilisé ailleurs.',
          ),
          true,
        );
      },
    });
  }

  // --- notification ---
  private notifier(message: string, erreur = false): void {
    clearTimeout(this.timerNotif);
    this.notification.set(message);
    this.notificationErreur.set(erreur);
    this.timerNotif = setTimeout(() => this.notification.set(null), erreur ? 6000 : 3500);
  }
}
