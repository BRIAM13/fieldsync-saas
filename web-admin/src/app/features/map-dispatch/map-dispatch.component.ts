import { Component, inject, OnInit, signal } from '@angular/core';
import { AsyncPipe, NgFor, NgIf } from '@angular/common';
import { WorkOrderService } from '../../core/services/work-order.service';
import { Technician } from '../../core/models/work-order.model';

/**
 * Asignación inteligente en mapa (característica clave #1).
 *
 * En producción el <div class="map"> hospedaría Leaflet/Google Maps con marcadores
 * por orden y técnico. Aquí se representa el lienzo del mapa y el flujo de asignación:
 * seleccionar una orden sin asignar → elegir técnico → el servicio emite el nuevo estado.
 */
@Component({
  selector: 'fs-map-dispatch',
  standalone: true,
  imports: [NgFor, NgIf, AsyncPipe],
  template: `
    <h2>Asignación en mapa</h2>
    <div class="layout">
      <div class="map">
        <span class="map-hint">🗺️ Lienzo del mapa (Leaflet / Google Maps)</span>
        <div class="pin" *ngFor="let o of (orders$ | async)"
             [style.left.%]="pinX(o.location?.lng)" [style.top.%]="pinY(o.location?.lat)"
             [class.assigned]="o.status !== 'UNASSIGNED'"
             (click)="select(o.id)">📍</div>
      </div>

      <aside class="panel">
        <h3>Técnicos</h3>
        <ul>
          <li *ngFor="let t of technicians()">
            <span [class.off]="!t.available">{{ t.name }}</span>
            <button *ngIf="selectedOrderId() && t.available"
                    (click)="assign(t.id)">Asignar</button>
          </li>
        </ul>
        <p *ngIf="selectedOrderId()">Orden seleccionada: <b>{{ selectedOrderId() }}</b></p>
      </aside>
    </div>
  `,
  styles: [`
    .layout { display: grid; grid-template-columns: 2fr 1fr; gap: 16px; }
    .map { position: relative; height: 420px; background: #0b1220;
           border: 1px solid #334155; border-radius: 8px; overflow: hidden; }
    .map-hint { position: absolute; top: 12px; left: 12px; color: #64748b; font-size: 13px; }
    .pin { position: absolute; cursor: pointer; font-size: 20px; transform: translate(-50%, -100%); }
    .pin.assigned { filter: grayscale(1) opacity(.5); }
    .panel { background: var(--fs-surface); border-radius: 8px; padding: 16px; }
    .panel li { display: flex; justify-content: space-between; padding: 6px 0; }
    .off { color: #64748b; text-decoration: line-through; }
    button { background: var(--fs-primary); color: #fff; border: 0; border-radius: 6px;
             padding: 4px 10px; cursor: pointer; }
  `],
})
export class MapDispatchComponent implements OnInit {
  private readonly service = inject(WorkOrderService);

  readonly orders$ = this.service.getWorkOrders();
  readonly technicians = signal<Technician[]>([]);
  readonly selectedOrderId = signal<string | null>(null);

  ngOnInit(): void {
    this.service.getTechnicians().subscribe((t) => this.technicians.set(t));
  }

  select(orderId: string): void {
    this.selectedOrderId.set(orderId);
  }

  assign(technicianId: string): void {
    const orderId = this.selectedOrderId();
    if (!orderId) return;
    this.service.assignOrder(orderId, technicianId);
    this.selectedOrderId.set(null);
  }

  // Proyección lineal simple lng/lat → % del lienzo (placeholder del mapa real).
  pinX(lng: number | undefined): number {
    return lng == null ? 50 : ((lng + 77.06) / 0.08) * 100;
  }
  pinY(lat: number | undefined): number {
    return lat == null ? 50 : ((-12.13 - lat) / -0.1) * 100;
  }
}
