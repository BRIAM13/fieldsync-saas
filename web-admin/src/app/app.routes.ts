import { Routes } from '@angular/router';

/** Rutas del panel con carga diferida (lazy) de cada feature standalone. */
export const routes: Routes = [
  { path: '', redirectTo: 'dispatch', pathMatch: 'full' },
  {
    path: 'dispatch',
    loadComponent: () =>
      import('./features/map-dispatch/map-dispatch.component').then((m) => m.MapDispatchComponent),
    title: 'Asignación en mapa',
  },
  {
    path: 'orders',
    loadComponent: () =>
      import('./features/work-orders/work-orders.component').then((m) => m.WorkOrdersComponent),
    title: 'Órdenes de trabajo',
  },
  { path: '**', redirectTo: 'dispatch' },
];
