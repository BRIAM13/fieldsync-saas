import { Component, inject } from '@angular/core';
import { AsyncPipe, NgFor } from '@angular/common';
import { WorkOrderService } from '../../core/services/work-order.service';

/** Listado tabular de todas las órdenes de trabajo (observa el servicio con async pipe). */
@Component({
  selector: 'fs-work-orders',
  standalone: true,
  imports: [NgFor, AsyncPipe],
  template: `
    <h2>Órdenes de trabajo</h2>
    <table>
      <thead>
        <tr><th>ID</th><th>Título</th><th>Cliente</th><th>Prioridad</th><th>Estado</th></tr>
      </thead>
      <tbody>
        <tr *ngFor="let o of orders$ | async">
          <td>{{ o.id }}</td>
          <td>{{ o.title }}</td>
          <td>{{ o.customerName }}</td>
          <td>{{ o.priority }}</td>
          <td>{{ o.status }}</td>
        </tr>
      </tbody>
    </table>
  `,
  styles: [`
    table { width: 100%; border-collapse: collapse; }
    th, td { text-align: left; padding: 10px 12px; border-bottom: 1px solid #334155; }
    th { color: #94a3b8; font-weight: 600; }
  `],
})
export class WorkOrdersComponent {
  private readonly service = inject(WorkOrderService);
  readonly orders$ = this.service.getWorkOrders();
}
