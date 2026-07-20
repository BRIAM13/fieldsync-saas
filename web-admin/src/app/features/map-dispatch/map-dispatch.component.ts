import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { NgFor, NgIf } from '@angular/common';
import { Subscription } from 'rxjs';
import * as L from 'leaflet';
import { WorkOrderService } from '../../core/services/work-order.service';
import { AuthService } from '../../core/services/auth.service';
import { Technician, WorkOrder } from '../../core/models/work-order.model';

/**
 * Asignación inteligente en mapa (característica clave #1) con **Leaflet + OpenStreetMap**.
 *
 * Dibuja marcadores reales de órdenes y técnicos sobre un mapa interactivo. Al hacer clic en
 * una orden se selecciona; se le asigna el técnico disponible elegido y se traza la línea
 * orden → técnico. Los datos vienen del backend (WorkOrderService).
 */
@Component({
  selector: 'fs-map-dispatch',
  standalone: true,
  imports: [NgFor, NgIf],
  template: `
    <h2>Asignación en mapa</h2>
    <div class="layout">
      <div #map class="map"></div>

      <aside class="panel">
        <h3>Técnicos</h3>
        <ul>
          <li *ngFor="let t of technicians()">
            <span [class.off]="!t.available">{{ t.name }}</span>
            <button *ngIf="canAssign() && selectedOrderId() && t.available" (click)="assign(t.id)">
              Asignar
            </button>
          </li>
        </ul>
        <p *ngIf="!canAssign()" class="muted">
          Tu rol ({{ role() }}) no puede asignar órdenes.
        </p>
        <p *ngIf="canAssign() && selectedOrderId(); else hint">
          Orden seleccionada: <b>{{ selectedOrderId() }}</b>
        </p>
        <ng-template #hint>
          <p *ngIf="canAssign()" class="muted">Haz clic en un pin de orden 📍 para asignarla.</p>
        </ng-template>
      </aside>
    </div>
  `,
  styles: [`
    .layout { display: grid; grid-template-columns: 2fr 1fr; gap: 16px; }
    .map { height: 460px; border-radius: 8px; overflow: hidden; border: 1px solid #334155; }
    .panel { background: var(--fs-surface); border-radius: 8px; padding: 16px; }
    .panel li { display: flex; justify-content: space-between; align-items: center; padding: 6px 0; }
    .off { color: #64748b; text-decoration: line-through; }
    .muted { color: #64748b; font-size: 13px; }
    button { background: var(--fs-primary); color: #fff; border: 0; border-radius: 6px;
             padding: 4px 10px; cursor: pointer; }
    /* Marcadores (divIcon) */
    :host ::ng-deep .fs-pin {
      font-size: 22px; line-height: 22px; text-align: center;
      filter: drop-shadow(0 1px 2px rgba(0,0,0,.4));
    }
    :host ::ng-deep .fs-pin.assigned { opacity: .5; }
  `],
})
export class MapDispatchComponent implements AfterViewInit, OnDestroy {
  @ViewChild('map') mapEl!: ElementRef<HTMLElement>;

  private readonly service = inject(WorkOrderService);
  private readonly auth = inject(AuthService);

  readonly technicians = signal<Technician[]>([]);
  readonly selectedOrderId = signal<string | null>(null);

  /** RBAC en el cliente: solo ADMIN/DISPATCHER asignan (el backend lo exige de todos modos). */
  readonly role = () => this.auth.user()?.role ?? '—';
  readonly canAssign = () => {
    const r = this.auth.user()?.role;
    return r === 'ADMIN' || r === 'DISPATCHER';
  };

  private map!: L.Map;
  private readonly orderLayer = L.layerGroup();
  private readonly techLayer = L.layerGroup();
  private lastOrders: WorkOrder[] = [];
  private subs = new Subscription();

  ngAfterViewInit(): void {
    this.map = L.map(this.mapEl.nativeElement, { center: [-12.06, -77.03], zoom: 12 });
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap',
      maxZoom: 19,
    }).addTo(this.map);
    this.orderLayer.addTo(this.map);
    this.techLayer.addTo(this.map);
    // El contenedor se dimensiona tras el layout: recalcula el tamaño del mapa.
    setTimeout(() => this.map.invalidateSize(), 0);

    // El endpoint de técnicos es solo ADMIN/DISPATCHER; no lo pidas si el rol no puede.
    if (this.canAssign()) {
      this.subs.add(
        this.service.getTechnicians().subscribe((techs) => {
          this.technicians.set(techs);
          this.renderTechnicians(techs);
          this.renderOrders(this.lastOrders);
        }),
      );
    }
    this.subs.add(
      this.service.getWorkOrders().subscribe((orders) => {
        this.lastOrders = orders;
        this.renderOrders(orders);
      }),
    );
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
    this.map?.remove();
  }

  assign(technicianId: string): void {
    const orderId = this.selectedOrderId();
    if (!orderId) return;
    this.service.assignOrder(orderId, technicianId);
    this.selectedOrderId.set(null);
  }

  private pinIcon(emoji: string, assigned: boolean): L.DivIcon {
    return L.divIcon({
      className: `fs-pin${assigned ? ' assigned' : ''}`,
      html: emoji,
      iconSize: [24, 24],
      iconAnchor: [12, 22],
    });
  }

  private renderOrders(orders: WorkOrder[]): void {
    if (!this.map) return;
    this.orderLayer.clearLayers();
    const techById = new Map(this.technicians().map((t) => [t.id, t]));

    for (const order of orders) {
      if (!order.location) continue;
      const assigned = order.status !== 'UNASSIGNED';
      L.marker([order.location.lat, order.location.lng], {
        icon: this.pinIcon('📍', assigned),
      })
        .bindPopup(`<b>${order.title}</b><br>${order.customerName}<br>${order.status}`)
        .on('click', () => this.selectedOrderId.set(order.id))
        .addTo(this.orderLayer);

      // Línea orden → técnico asignado.
      const tech = order.assignedTechnicianId ? techById.get(order.assignedTechnicianId) : undefined;
      if (order.location && tech) {
        L.polyline(
          [
            [order.location.lat, order.location.lng],
            [tech.location.lat, tech.location.lng],
          ],
          { color: '#2563eb', weight: 2, dashArray: '6 6' },
        ).addTo(this.orderLayer);
      }
    }
  }

  private renderTechnicians(techs: Technician[]): void {
    if (!this.map) return;
    this.techLayer.clearLayers();
    for (const tech of techs) {
      L.marker([tech.location.lat, tech.location.lng], {
        icon: this.pinIcon('🔧', !tech.available),
      })
        .bindPopup(`<b>${tech.name}</b><br>${tech.available ? 'Disponible' : 'Ocupado'}`)
        .addTo(this.techLayer);
    }
  }
}
