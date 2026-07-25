import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { CustomerAuthService } from '../services/customer-auth.service';

/**
 * Bloquea /login y /register si ya hay una sesión activa (staff o cliente) — evita el bug de
 * ver la cabecera con navegación y la pantalla de login a la vez (p. ej. al volver atrás con
 * el navegador estando ya autenticado). Redirige a la vista principal de la sesión activa.
 */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const customerAuth = inject(CustomerAuthService);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return router.createUrlTree(['/dispatch']);
  }
  if (customerAuth.isAuthenticated()) {
    return router.createUrlTree(['/portal']);
  }
  return true;
};
