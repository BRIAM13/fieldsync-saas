import { Component, inject } from '@angular/core';
import { AsyncPipe, NgFor, NgIf } from '@angular/common';
import { WorkOrderService } from '../../core/services/work-order.service';
import { PriorityLabelPipe, StatusLabelPipe } from '../../core/pipes/enum-label.pipe';

/** Listado tabular de todas las órdenes de trabajo (observa el servicio con async pipe). */
@Component({
  selector: 'fs-work-orders',
  standalone: true,
  imports: [NgFor, NgIf, AsyncPipe, PriorityLabelPipe, StatusLabelPipe],
  template: `
    <div class="header-row">
      <div>
        <h2>Órdenes de trabajo</h2>
        <p class="sub">Todas las órdenes de tu empresa</p>
      </div>
    </div>

    <div class="fs-error" *ngIf="service.error() as err">
      <span>⚠ {{ err }}</span>
      <button (click)="service.refresh()">Reintentar</button>
    </div>

    <div class="fs-card table-card" *ngIf="!service.error()">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Título</th>
            <th>Cliente</th>
            <th>Prioridad</th>
            <th>Estado</th>
          </tr>
        </thead>
        <tbody *ngIf="service.loading(); else loaded">
          <tr *ngFor="let _ of skeletonRows">
            <td><span class="skeleton" style="width: 70px"></span></td>
            <td><span class="skeleton" style="width: 140px"></span></td>
            <td><span class="skeleton" style="width: 110px"></span></td>
            <td><span class="skeleton" style="width: 60px"></span></td>
            <td><span class="skeleton" style="width: 80px"></span></td>
          </tr>
        </tbody>
        <ng-template #loaded>
          <tbody>
            <tr *ngFor="let o of orders$ | async">
              <td class="mono">{{ o.id }}</td>
              <td class="title-cell">{{ o.title }}</td>
              <td>{{ o.customerName }}</td>
              <td><span [class]="'badge badge-' + o.priority.toLowerCase()">{{ o.priority | priorityLabel }}</span></td>
              <td><span [class]="'badge badge-' + o.status.toLowerCase()">{{ o.status | statusLabel }}</span></td>
            </tr>
          </tbody>
        </ng-template>
      </table>
    </div>
  `,
  styles: [`
    .header-row { margin-bottom: 20px; }
    h2 { margin: 0; font-size: 22px; }
    .sub { margin: 4px 0 0; color: var(--fs-text-faint); font-size: 13px; }

    .table-card { overflow: hidden; }
    table { width: 100%; border-collapse: collapse; }
    th, td { text-align: left; padding: 14px 18px; border-bottom: 1px solid var(--fs-border); font-size: 13px; }
    th {
      color: var(--fs-text-faint);
      font-weight: 600;
      text-transform: uppercase;
      font-size: 11px;
      letter-spacing: 0.04em;
    }
    tbody tr { transition: background 0.15s; }
    tbody tr:hover { background: rgba(255, 255, 255, 0.02); }
    tbody tr:last-child td { border-bottom: none; }
    .mono { font-family: 'DM Mono', monospace; color: var(--fs-text-faint); font-size: 12px; }
    .title-cell { font-weight: 500; }
  `],
})
export class WorkOrdersComponent {
  readonly service = inject(WorkOrderService);
  readonly orders$ = this.service.getWorkOrders();
  readonly skeletonRows = Array.from({ length: 5 });
}
