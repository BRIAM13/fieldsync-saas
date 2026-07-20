import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Adjunta el header `Authorization: Bearer <token>` a las llamadas a la API.
 * No lo añade a las rutas de auth (login/registro), que aún no tienen token.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(AuthService).token();
  const isAuthCall = req.url.includes('/auth/');

  if (token && !isAuthCall) {
    return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
  }
  return next(req);
};
