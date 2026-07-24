import {
  HttpContextToken,
  HttpErrorResponse,
  HttpInterceptorFn,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { CustomerAuthService } from '../services/customer-auth.service';

/** Marca una petición ya reintentada tras refrescar, para no entrar en bucle. */
const RETRIED = new HttpContextToken<boolean>(() => false);

/**
 * Adjunta el Bearer del **cliente** solo a las rutas de cliente (`/customer/*`,
 * `/api/service-requests*`) — nunca toca llamadas de staff, así ambas sesiones conviven sin
 * pisarse (ver `auth.interceptor.ts`, su equivalente para staff). Mismo patrón de reintento
 * tras 401 con refresh.
 */
export const customerAuthInterceptor: HttpInterceptorFn = (req, next) => {
  const isCustomerRoute =
    req.url.includes('/customer/') || req.url.includes('/api/service-requests');
  if (!isCustomerRoute) return next(req);

  const auth = inject(CustomerAuthService);
  const token = auth.token();
  const isAuthCall = req.url.includes('/customer/auth/');

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
