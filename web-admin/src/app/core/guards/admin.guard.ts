import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Bloquea la ruta si el usuario no es ADMIN (el backend lo exige igual en
 * GET/POST /api/users — esto es solo para no mostrar una pantalla que fallará).
 */
export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAuthenticated() && auth.user()?.role === 'ADMIN') {
    return true;
  }
  return router.createUrlTree(['/dispatch']);
};
