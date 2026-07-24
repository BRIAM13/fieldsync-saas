import { Component, inject } from '@angular/core';
import { AsyncPipe, NgFor, NgIf } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { CustomerPortalService } from '../../core/services/customer-portal.service';
import { priorityLabel, statusLabel } from '../../core/utils/labels';

/** "Mis solicitudes": lista del cliente, mismo patrón que work-orders.component.ts (staff). */
@Component({
  selector: 'fs-my-requests',
  standalone: true,
  imports: [NgFor, NgIf, AsyncPipe, RouterLink],
  template: `
    <div class="header-row">
      <div>
        <h2>Mis solicitudes</h2>
        <p class="sub">Servicios que has pedido a tu empresa</p>
      </div>
      <button class="primary" routerLink="/portal/new">+ Nueva solicitud</button>
    </div>

    <div class="fs-error" *ngIf="service.error() as err">
      <span>⚠ {{ err }}</span>
      <button (click)="service.refresh()">Reintentar</button>
    </div>

    <div class="fs-card table-card" *ngIf="!service.error()">
      <table>
        <thead>
          <tr>
            <th>Título</th>
            <th>Dirección</th>
            <th>Prioridad</th>
            <th>Estado</th>
            <th></th>
          </tr>
        </thead>
        <tbody *ngIf="service.loading(); else loaded">
          <tr *ngFor="let _ of skeletonRows">
            <td><span class="skeleton" style="width: 140px"></span></td>
            <td><span class="skeleton" style="width: 160px"></span></td>
            <td><span class="skeleton" style="width: 60px"></span></td>
            <td><span class="skeleton" style="width: 80px"></span></td>
            <td></td>
          </tr>
        </tbody>
        <ng-template #loaded>
          <tbody>
            <tr *ngFor="let r of requests$ | async" (click)="openTracking(r.id)" class="clickable">
              <td class="title-cell">{{ r.title }}</td>
              <td>{{ r.address }}</td>
              <td><span [class]="'badge badge-' + r.priority.toLowerCase()">{{ priorityLabel(r.priority) }}</span></td>
              <td><span [class]="'badge badge-' + r.status.toLowerCase()">{{ statusLabel(r.status) }}</span></td>
              <td class="chevron">›</td>
            </tr>
            <tr *ngIf="(requests$ | async)?.length === 0">
              <td colspan="5" class="empty">Aún no tienes solicitudes.</td>
            </tr>
          </tbody>
        </ng-template>
      </table>
    </div>
  `,
  styles: [`
    .header-row { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 20px; }
    h2 { margin: 0; font-size: 22px; }
    .sub { margin: 4px 0 0; color: var(--fs-text-faint); font-size: 13px; }

    .primary {
      background: var(--fs-primary); color: #fff; border: 0; border-radius: var(--fs-radius-sm);
      padding: 9px 16px; cursor: pointer; font-size: 13px; font-weight: 600;
      transition: background 0.15s;
    }
    .primary:hover { background: var(--fs-primary-dark); }

    .table-card { overflow: hidden; }
    table { width: 100%; border-collapse: collapse; }
    th, td { text-align: left; padding: 14px 18px; border-bottom: 1px solid var(--fs-border); font-size: 13px; }
    th {
      color: var(--fs-text-faint); font-weight: 600; text-transform: uppercase;
      font-size: 11px; letter-spacing: 0.04em;
    }
    tbody tr:last-child td { border-bottom: none; }
    tbody tr.clickable { cursor: pointer; transition: background 0.15s; }
    tbody tr.clickable:hover { background: rgba(255, 255, 255, 0.02); }
    .title-cell { font-weight: 500; }
    .chevron { color: var(--fs-text-faint); text-align: right; font-size: 16px; }
    .empty { text-align: center; color: var(--fs-text-faint); padding: 32px 18px; }
  `],
})
export class MyRequestsComponent {
  readonly service = inject(CustomerPortalService);
  private readonly router = inject(Router);

  readonly requests$ = this.service.getMyRequests();
  readonly skeletonRows = Array.from({ length: 4 });
  readonly statusLabel = statusLabel;
  readonly priorityLabel = priorityLabel;

  constructor() {
    this.service.refresh();
  }

  openTracking(id: string): void {
    this.router.navigate(['/portal/tracking', id]);
  }
}
