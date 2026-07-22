import { Component, inject } from '@angular/core';
import { AsyncPipe, NgFor } from '@angular/common';
import { WorkOrderService } from '../../core/services/work-order.service';
import { PriorityLabelPipe, StatusLabelPipe } from '../../core/pipes/enum-label.pipe';

/** Listado tabular de todas las órdenes de trabajo (observa el servicio con async pipe). */
@Component({
  selector: 'fs-work-orders',
  standalone: true,
  imports: [NgFor, AsyncPipe, PriorityLabelPipe, StatusLabelPipe],
  template: `
    <div class="header-row">
      <div>
        <h2>Órdenes de trabajo</h2>
        <p class="sub">Todas las órdenes de tu empresa</p>
      </div>
    </div>

    <div class="fs-card table-card">
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
        <tbody>
          <tr *ngFor="let o of orders$ | async">
            <td class="mono">{{ o.id }}</td>
            <td class="title-cell">{{ o.title }}</td>
            <td>{{ o.customerName }}</td>
            <td><span [class]="'badge badge-' + o.priority.toLowerCase()">{{ o.priority | priorityLabel }}</span></td>
            <td><span [class]="'badge badge-' + o.status.toLowerCase()">{{ o.status | statusLabel }}</span></td>
          </tr>
        </tbody>
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
  private readonly service = inject(WorkOrderService);
  readonly orders$ = this.service.getWorkOrders();
}
