import {
  AfterViewInit,
  Component,
  ElementRef,
  OnInit,
  computed,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Filiere, semestresDuNiveau } from '../../../../core/models/filiere.model';
import {
  ModuleCours,
  ModuleCoursRequest,
  numeroSemestre,
} from '../../../../core/models/module-cours.model';
import { ModuleCoursApi } from '../../../../core/services/module-cours-api';
import { messageErreur } from '../../../../core/services/api-error';

@Component({
  selector: 'app-module-form',
  imports: [ReactiveFormsModule],
  templateUrl: './module-form.html',
  styleUrl: '../../../../shared/styles/admin-ui.css',
})
export class ModuleForm implements OnInit, AfterViewInit {
  filiere = input.required<Filiere>();
  
  module = input<ModuleCours | null>(null);
  /** semestre pré-sélectionné quand on ajoute depuis un groupe ("S1", "S2"...) */
  semestreParDefaut = input('S1');
  /** codes déjà pris (tous les modules), pour prévenir le doublon */
  codesExistants = input<string[]>([]);

  enregistre = output<ModuleCours>();
  ferme = output<void>();

  private fb = inject(FormBuilder);
  private api = inject(ModuleCoursApi);
  private dlg = viewChild.required<ElementRef<HTMLDialogElement>>('dlg');

  enCours = signal(false);
  erreur = signal<string | null>(null);

  semestres = computed(() => {
    const liste = semestresDuNiveau(this.filiere().niveau);
    const actuel = this.module()?.semestre;
    if (actuel && !liste.includes(actuel)) liste.push(actuel);
    return liste.sort((a, b) => numeroSemestre(a) - numeroSemestre(b));
  });

  form = this.fb.group({
    code: this.fb.nonNullable.control('', [
      Validators.required,
      Validators.maxLength(30),
      Validators.pattern(/^[A-Za-z0-9_-]+$/),
    ]),
    nom: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(150)]),
    semestre: this.fb.nonNullable.control('S1', Validators.required),
    volumeHoraire: this.fb.control<number | null>(null, [Validators.min(1), Validators.max(300)]),
  });

  get estModification(): boolean {
    return this.module() !== null;
  }

  ngOnInit(): void {
    const m = this.module();
    if (m) {
      this.form.setValue({
        code: m.code,
        nom: m.nom,
        semestre: m.semestre,
        volumeHoraire: m.volumeHoraire,
      });
    } else {
      this.form.controls.semestre.setValue(this.semestreParDefaut());
    }
  }

  ngAfterViewInit(): void {
    this.dlg().nativeElement.showModal();
  }

  invalide(champ: keyof typeof this.form.controls): boolean {
    const c = this.form.controls[champ];
    return c.invalid && (c.touched || c.dirty);
  }

  enregistrer(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const code = v.code.trim().toUpperCase();
    const ancienCode = this.module()?.code.toUpperCase();

    if (code !== ancienCode && this.codesExistants().some((c) => c.toUpperCase() === code)) {
      this.erreur.set(`Un module avec le code « ${code} » existe déjà.`);
      return;
    }

    const m = this.module();
    const vh = v.volumeHoraire as number | string | null;
    const corps: ModuleCoursRequest = {
      code,
      nom: v.nom.trim(),
      semestre: v.semestre,
      volumeHoraire: vh === null || vh === '' ? null : Number(vh),
      filiereId: this.filiere().id,
      // on conserve l'enseignant actuel : le backend le retire si on envoie null
      enseignantId: m?.enseignantId ?? null,
    };
    const requete = m ? this.api.update(m.id, corps) : this.api.create(corps);

    this.enCours.set(true);
    this.erreur.set(null);
    requete.subscribe({
      next: (resultat) => this.enregistre.emit(resultat),
      error: (err) => {
        this.erreur.set(
          messageErreur(
            err,
            'Enregistrement refusé par le serveur. Vérifiez que le code n’est pas déjà utilisé.',
          ),
        );
        this.enCours.set(false);
      },
    });
  }
}
