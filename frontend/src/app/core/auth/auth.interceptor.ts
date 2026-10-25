import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthToken } from './auth-token';

/** Ajoute « Authorization: Bearer <token> » aux appels de l'API (sauf /auth/login). */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthToken);
  const token = auth.get();
  const requete =
    token && !req.url.includes('/auth/login')
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

  return next(requete).pipe(
    catchError((err) => {
      // token expiré ou invalide : on l'oublie pour qu'une nouvelle connexion soit possible
      if (err instanceof HttpErrorResponse && (err.status === 401 || err.status === 403))
        auth.clear();
      return throwError(() => err);
    }),
  );
};
