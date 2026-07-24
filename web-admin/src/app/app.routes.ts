import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';

/** Rutas del panel: /login pública; el resto protegido por el guard de sesión. */
export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login.component').then((m) => m.LoginComponent),
    title: 'Ingresar',
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register.component').then((m) => m.RegisterComponent),
    title: 'Registra tu empresa',
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
  {
    path: 'team',
    canActivate: [authGuard, adminGuard],
    loadComponent: () =>
      import('./features/users/user-management.component').then((m) => m.UserManagementComponent),
    title: 'Equipo',
  },
  { path: '', redirectTo: 'dispatch', pathMatch: 'full' },
  { path: '**', redirectTo: 'dispatch' },
];
