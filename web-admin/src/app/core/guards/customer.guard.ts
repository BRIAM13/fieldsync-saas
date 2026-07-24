import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { CustomerAuthService } from '../services/customer-auth.service';

/** Bloquea las rutas del portal de cliente si no hay sesión de cliente; redirige a /login. */
export const customerGuard: CanActivateFn = () => {
  const auth = inject(CustomerAuthService);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return true;
  }
  return router.createUrlTree(['/login']);
};
