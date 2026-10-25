import { HttpErrorResponse } from '@angular/common/http';

/**
 * Transforme une erreur HTTP en message lisible.
 * Le backend lève des RuntimeException sans @RestControllerAdvice : elles arrivent
 * en 500 sans message exploitable, d'où le message de repli `si500`.
 */
export function messageErreur(err: unknown, si500 = 'Le serveur a refusé l’opération.'): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0)
      return 'Le serveur ne répond pas. Vérifiez que le backend est démarré sur le port 8080.';
    if (err.status === 401 || err.status === 403)
      return 'Vous n’êtes pas connecté ou votre session a expiré.';
    if (typeof err.error?.message === 'string' && err.error.message) return err.error.message;
    if (err.status === 400) return 'Certaines données envoyées sont invalides.';
    if (err.status === 404) return 'Élément introuvable, il a peut-être été supprimé.';
    if (err.status >= 500) return si500;
  }
  return 'Une erreur inattendue est survenue.';
}
