import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';
import { customerGuard } from './core/guards/customer.guard';

/**
 * Rutas del panel: /login y /register son la misma pantalla dinámica (empresa/cliente,
 * login/registro); el resto está protegido por el guard de la sesión correspondiente.
 */
export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/auth-gateway.component').then((m) => m.AuthGatewayComponent),
    data: { mode: 'login' },
    title: 'Ingresar',
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/auth-gateway.component').then((m) => m.AuthGatewayComponent),
    data: { mode: 'register' },
    title: 'Crear cuenta',
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
  {
    path: 'portal',
    canActivate: [customerGuard],
    loadComponent: () =>
      import('./features/portal/my-requests.component').then((m) => m.MyRequestsComponent),
    title: 'Mis solicitudes',
  },
  {
    path: 'portal/new',
    canActivate: [customerGuard],
    loadComponent: () =>
      import('./features/portal/new-request.component').then((m) => m.NewRequestComponent),
    title: 'Nueva solicitud',
  },
  {
    path: 'portal/tracking/:id',
    canActivate: [customerGuard],
    loadComponent: () =>
      import('./features/portal/tracking.component').then((m) => m.PortalTrackingComponent),
    title: 'Seguimiento',
  },
  { path: '', redirectTo: 'dispatch', pathMatch: 'full' },
  { path: '**', redirectTo: 'dispatch' },
];
