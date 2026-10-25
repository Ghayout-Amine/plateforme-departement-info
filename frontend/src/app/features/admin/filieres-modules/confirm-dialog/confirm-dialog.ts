import { AfterViewInit, Component, ElementRef, input, output, viewChild } from '@angular/core';

@Component({
  selector: 'app-confirm-dialog',
  styleUrl: '../../../../shared/styles/admin-ui.css',
  template: `
    <dialog #dlg class="dialog dialog--petit" (cancel)="annuler.emit()">
      <h2 class="dialog__titre">{{ titre() }}</h2>
      <p class="dialog__texte">{{ message() }}</p>
      <div class="dialog__actions">
        <button type="button" class="btn btn--secondaire" (click)="annuler.emit()">Annuler</button>
        <button
          type="button"
          class="btn btn--danger"
          [disabled]="enCours()"
          (click)="confirmer.emit()"
        >
          {{ enCours() ? 'Suppression…' : libelleConfirmer() }}
        </button>
      </div>
    </dialog>
  `,
})
export class ConfirmDialog implements AfterViewInit {
  titre = input.required<string>();
  message = input.required<string>();
  libelleConfirmer = input('Supprimer');
  enCours = input(false);

  confirmer = output<void>();
  annuler = output<void>();

  private dlg = viewChild.required<ElementRef<HTMLDialogElement>>('dlg');

  ngAfterViewInit(): void {
    this.dlg().nativeElement.showModal();
  }
}
