import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, tap, throwError } from 'rxjs';
import { ConnectivityService } from '../services/connectivity.service';

/**
 * Marca el backend como no alcanzable ante un error de red (status 0: CORS,
 * timeout, servidor caído) y lo marca alcanzable de nuevo en cuanto una
 * petición cualquiera responde con éxito. No interfiere con errores HTTP
 * normales (401, 404, etc.) — esos sí llegaron al servidor.
 */
export const connectivityInterceptor: HttpInterceptorFn = (req, next) => {
  const connectivity = inject(ConnectivityService);
  return next(req).pipe(
    tap(() => connectivity.reportRequestSucceeded()),
    catchError((err: HttpErrorResponse) => {
      if (err.status === 0) {
        connectivity.reportRequestFailed();
      }
      return throwError(() => err);
    }),
  );
};
