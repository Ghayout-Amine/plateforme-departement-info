import {
  AfterViewInit,
  Component,
  ElementRef,
  OnInit,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Filiere, FiliereRequest, NIVEAUX } from '../../../../core/models/filiere.model';
import { FiliereApi } from '../../../../core/services/filiere-api';
import { messageErreur } from '../../../../core/services/api-error';

@Component({
  selector: 'app-filiere-form',
  imports: [ReactiveFormsModule],
  templateUrl: './filiere-form.html',
  styleUrl: '../../../../shared/styles/admin-ui.css',
})
export class FiliereForm implements OnInit, AfterViewInit {

  filiere = input<Filiere | null>(null);
 
  codesExistants = input<string[]>([]);

  enregistree = output<Filiere>();
  fermee = output<void>();

  private fb = inject(FormBuilder);
  private api = inject(FiliereApi);
  private dlg = viewChild.required<ElementRef<HTMLDialogElement>>('dlg');

  niveaux = [...NIVEAUX];
  enCours = signal(false);
  erreur = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    code: [
      '',
      [Validators.required, Validators.maxLength(30), Validators.pattern(/^[A-Za-z0-9_-]+$/)],
    ],
    nom: ['', [Validators.required, Validators.maxLength(150)]],
    niveau: ['LICENCE', Validators.required],
    description: [''],
  });

  get estModification(): boolean {
    return this.filiere() !== null;
  }

  ngOnInit(): void {
    const f = this.filiere();
    if (!f) return;
    // niveau inconnu déjà en base , l'ajoute à la liste pour ne pas le perdre
    if (!this.niveaux.some((n) => n.value === f.niveau)) {
      this.niveaux.push({ value: f.niveau, label: f.niveau, semestres: 6 });
    }
    this.form.setValue({
      code: f.code,
      nom: f.nom,
      niveau: f.niveau,
      description: f.description ?? '',
    });
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
    const ancienCode = this.filiere()?.code.toUpperCase();

    if (code !== ancienCode && this.codesExistants().some((c) => c.toUpperCase() === code)) {
      this.erreur.set(`Une filière avec le code « ${code} » existe déjà.`);
      return;
    }

    const f = this.filiere();
    const corps: FiliereRequest = {
      code,
      nom: v.nom.trim(),
      niveau: v.niveau,
      description: v.description.trim() || null,
     
      responsableId: f?.responsableId ?? null,
    };
    const requete = f ? this.api.update(f.id, corps) : this.api.create(corps);

    this.enCours.set(true);
    this.erreur.set(null);
    requete.subscribe({
      next: (resultat) => this.enregistree.emit(resultat),
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
