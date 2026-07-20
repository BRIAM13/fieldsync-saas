import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

/** Rutas del panel: /login pública; el resto protegido por el guard de sesión. */
export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login.component').then((m) => m.LoginComponent),
    title: 'Ingresar',
  },
  {
    path: 'dispatch',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/map-dispatch/map-dispatch.component').then((m) => m.MapDispatchComponent),
    title: 'Asignación en mapa',
  },
  {
    path: 'orders',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/work-orders/work-orders.component').then((m) => m.WorkOrdersComponent),
    title: 'Órdenes de trabajo',
  },
  { path: '', redirectTo: 'dispatch', pathMatch: 'full' },
  { path: '**', redirectTo: 'dispatch' },
];
