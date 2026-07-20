import {
  HttpContextToken,
  HttpErrorResponse,
  HttpInterceptorFn,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/** Marca una petición ya reintentada tras refrescar, para no entrar en bucle. */
const RETRIED = new HttpContextToken<boolean>(() => false);

/**
 * Adjunta `Authorization: Bearer <token>` a la API y, ante un 401, intenta **renovar**
 * el access token con el refresh token y reintenta la petición una vez. Si el refresh
 * falla, cierra la sesión.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const isAuthCall = req.url.includes('/auth/');

  const authed =
    token && !isAuthCall
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

  return next(authed).pipe(
    catchError((err: HttpErrorResponse) => {
      const canRefresh =
        err.status === 401 &&
        !isAuthCall &&
        !!auth.refreshToken() &&
        !req.context.get(RETRIED);

      if (!canRefresh) return throwError(() => err);

      return auth.refresh().pipe(
        switchMap((res) => {
          const retried = req.clone({
            setHeaders: { Authorization: `Bearer ${res.token}` },
            context: req.context.set(RETRIED, true),
          });
          return next(retried);
        }),
        catchError((refreshErr) => {
          auth.logout();
          return throwError(() => refreshErr);
        }),
      );
    }),
  );
};
