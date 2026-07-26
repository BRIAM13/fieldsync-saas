import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  ViewChild,
  computed,
  inject,
  signal,
} from '@angular/core';
import { NgFor, NgIf } from '@angular/common';
import { Subscription } from 'rxjs';
import * as L from 'leaflet';
import { WorkOrderService } from '../../core/services/work-order.service';
import { AuthService } from '../../core/services/auth.service';
import { Specialty, Technician, WorkOrder } from '../../core/models/work-order.model';
import { priorityLabel, specialtyIcon, specialtyLabel, statusLabel } from '../../core/utils/labels';
import { inferSpecialty } from '../../core/utils/specialty-match';
import { PriorityLabelPipe, StatusLabelPipe } from '../../core/pipes/enum-label.pipe';

/**
 * Asignación inteligente en mapa (característica clave #1) con **Leaflet + OpenStreetMap**.
 *
 * Dibuja marcadores reales de órdenes y técnicos sobre un mapa interactivo. Al hacer clic en
 * una orden se selecciona y el panel lateral se convierte en un desglose completo (problema
 * reportado, cliente, dirección, prioridad, estado) con la lista de técnicos ordenada según
 * quién coincide con la especialidad que la orden necesita — inferida por palabras clave de
 * su título, ya que el backend no tiene un campo de categoría separado. Los datos vienen del
 * backend (WorkOrderService).
 */
@Component({
  selector: 'fs-map-dispatch',
  standalone: true,
  imports: [NgFor, NgIf, PriorityLabelPipe, StatusLabelPipe],
  template: `
    <div class="header-row">
      <div>
        <h2>Asignación en mapa</h2>
        <p class="sub">Elige una orden y asígnala al técnico más adecuado</p>
      </div>
    </div>

    <div class="fs-error" *ngIf="service.error() as err">
      <span>⚠ {{ err }}</span>
      <button (click)="retry()">Reintentar</button>
    </div>

    <div class="layout">
      <div class="map-card">
        <div #map class="map"></div>
        <div class="map-loading" *ngIf="service.loading()">
          <span class="spinner"></span> Cargando mapa…
        </div>
      </div>

      <aside class="panel fs-card">
        <ng-container *ngIf="canAssign() && selectedOrder() as order; else generalPanel">
          <div class="detail-card">
            <button class="back-btn" (click)="clearSelection()">← Ver todos los técnicos</button>

            <div class="detail-top">
              <span [class]="'badge badge-' + order.priority.toLowerCase()">{{ order.priority | priorityLabel }}</span>
              <span [class]="'badge badge-' + order.status.toLowerCase()">{{ order.status | statusLabel }}</span>
            </div>
            <h3 class="problem-title">{{ order.title }}</h3>

            <dl class="detail-list">
              <dt>Cliente</dt>
              <dd>{{ order.customerName }}</dd>
              <dt>Ubicación</dt>
              <dd>{{ order.address }}</dd>
              <dt>Programada</dt>
              <dd>{{ formatScheduled(order.scheduledAtEpochMs) }}</dd>
            </dl>

            <div class="assign-section">
              <h4>Asignar técnico</h4>
              <ul *ngIf="!techniciansLoading(); else techSkeleton">
                <li
                  *ngFor="let t of rankedTechnicians()"
                  [class.off]="!t.available"
                  [class.suggested]="t.specialty === suggestedSpecialty()"
                >
                  <div class="tech-dot" [class.available]="t.available"></div>
                  <div class="tech-info">
                    <span class="tech-name">
                      {{ t.name }}
                      <span class="suggested-tag" *ngIf="t.specialty === suggestedSpecialty()">★ Sugerido</span>
                    </span>
                    <span class="tech-status">
                      <span [class]="'badge badge-' + t.specialty.toLowerCase()">
                        {{ specialtyIcon(t.specialty) }} {{ specialtyLabel(t.specialty) }}
                      </span>
                      {{ t.available ? 'Disponible' : 'Ocupado' }}
                    </span>
                  </div>
                  <button *ngIf="t.available" (click)="assign(t.id)">Asignar</button>
                </li>
              </ul>
              <ng-template #techSkeleton>
                <ul>
                  <li *ngFor="let _ of skeletonRows">
                    <span class="skeleton" style="width: 10px; height: 10px; border-radius: 50%"></span>
                    <div class="tech-info">
                      <span class="skeleton" style="width: 100px; margin-bottom: 4px"></span>
                      <span class="skeleton" style="width: 60px; height: 10px"></span>
                    </div>
                  </li>
                </ul>
              </ng-template>
            </div>
          </div>
        </ng-container>

        <ng-template #generalPanel>
          <div class="panel-header">
            <h3>Técnicos</h3>
            <span class="count" *ngIf="!techniciansLoading()">{{ technicians().length }}</span>
          </div>

          <ul *ngIf="canAssign() && techniciansLoading(); else techList">
            <li *ngFor="let _ of skeletonRows">
              <span class="skeleton" style="width: 10px; height: 10px; border-radius: 50%"></span>
              <div class="tech-info">
                <span class="skeleton" style="width: 100px; margin-bottom: 4px"></span>
                <span class="skeleton" style="width: 60px; height: 10px"></span>
              </div>
            </li>
          </ul>

          <ng-template #techList>
            <ul>
              <li *ngFor="let t of technicians()" [class.off]="!t.available">
                <div class="tech-dot" [class.available]="t.available"></div>
                <div class="tech-info">
                  <span class="tech-name">{{ t.name }}</span>
                  <span class="tech-status">
                    <span [class]="'badge badge-' + t.specialty.toLowerCase()">
                      {{ specialtyIcon(t.specialty) }} {{ specialtyLabel(t.specialty) }}
                    </span>
                    {{ t.available ? 'Disponible' : 'Ocupado' }}
                  </span>
                </div>
              </li>
            </ul>
          </ng-template>

          <div class="footer-note" *ngIf="!canAssign()">
            <span class="lock">🔒</span>
            Tu rol ({{ roleLabel() }}) no puede asignar órdenes.
          </div>
          <div class="footer-note" *ngIf="canAssign()">
            Haz clic en un pin de orden 📍 en el mapa para ver el desglose y asignarla.
          </div>
        </ng-template>
      </aside>
    </div>
  `,
  styles: [`
    .header-row { margin-bottom: 20px; }
    h2 { margin: 0; font-size: 22px; }
    .sub { margin: 4px 0 0; color: var(--fs-text-faint); font-size: 13px; }

    .layout { display: grid; grid-template-columns: 2fr 1fr; gap: 16px; align-items: start; }

    .map-card {
      position: relative;
      border-radius: var(--fs-radius-lg);
      overflow: hidden;
      border: 1px solid var(--fs-border);
      box-shadow: var(--fs-shadow);
    }
    .map { height: 480px; }
    .map-loading {
      position: absolute;
      inset: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      background: rgba(11, 18, 32, 0.72);
      backdrop-filter: blur(2px);
      color: var(--fs-text-muted);
      font-size: 13px;
      font-weight: 500;
      z-index: 5;
    }

    .panel { padding: 18px; }
    .panel-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
    .panel-header h3 { margin: 0; font-size: 15px; }
    .count {
      background: var(--fs-primary-light); color: #60a5fa;
      font-size: 12px; font-weight: 700; padding: 2px 9px; border-radius: 100px;
    }

    ul { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 2px; }
    li {
      display: flex; align-items: center; gap: 10px;
      padding: 10px 8px; border-radius: var(--fs-radius-sm);
      transition: background 0.15s;
    }
    li:hover { background: rgba(255, 255, 255, 0.03); }
    li.off { opacity: 0.55; }
    li.suggested { background: var(--fs-primary-light); }

    .tech-dot {
      width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0;
      background: var(--fs-text-faint);
    }
    .tech-dot.available { background: #4ade80; box-shadow: 0 0 0 3px rgba(74, 222, 128, 0.18); }

    .tech-info { display: flex; flex-direction: column; flex: 1; min-width: 0; gap: 3px; }
    .tech-name { font-size: 13px; font-weight: 500; color: var(--fs-text); display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
    .tech-status { font-size: 11px; color: var(--fs-text-faint); display: flex; align-items: center; gap: 6px; }

    .suggested-tag {
      font-size: 10px; font-weight: 700; color: #facc15;
      background: rgba(250, 204, 21, 0.14); padding: 1px 7px; border-radius: 100px;
    }

    button {
      background: var(--fs-primary); color: #fff; border: 0; border-radius: var(--fs-radius-sm);
      padding: 6px 12px; cursor: pointer; font-size: 12px; font-weight: 600;
      transition: background 0.15s;
    }
    button:hover { background: var(--fs-primary-dark); }

    .footer-note {
      margin-top: 14px; padding: 10px 12px;
      background: var(--fs-surface-2); border-radius: var(--fs-radius-sm);
      color: var(--fs-text-faint); font-size: 12px; line-height: 1.5;
    }
    .footer-note .lock { margin-right: 4px; }

    /* Tarjeta de desglose de la orden seleccionada */
    .detail-card { animation: detailIn 0.22s ease-out; }
    @keyframes detailIn {
      from { opacity: 0; transform: translateY(4px); }
      to { opacity: 1; transform: translateY(0); }
    }
    .back-btn {
      background: none; color: var(--fs-text-faint); padding: 0; font-size: 12px;
      font-weight: 500; margin-bottom: 12px;
    }
    .back-btn:hover { color: var(--fs-text); background: none; }
    .detail-top { display: flex; gap: 6px; margin-bottom: 8px; }
    .problem-title { margin: 0 0 14px; font-size: 16px; line-height: 1.35; }

    .detail-list { margin: 0 0 18px; display: grid; grid-template-columns: auto 1fr; gap: 6px 12px; }
    .detail-list dt { font-size: 11px; color: var(--fs-text-faint); text-transform: uppercase; letter-spacing: 0.03em; }
    .detail-list dd { margin: 0; font-size: 13px; color: var(--fs-text); }

    .assign-section { border-top: 1px solid var(--fs-border); padding-top: 14px; }
    .assign-section h4 { margin: 0 0 8px; font-size: 12px; color: var(--fs-text-faint); text-transform: uppercase; letter-spacing: 0.03em; }

    /* Marcadores (divIcon) */
    :host ::ng-deep .fs-pin {
      font-size: 22px; line-height: 22px; text-align: center;
      filter: drop-shadow(0 1px 2px rgba(0,0,0,.4));
    }
    :host ::ng-deep .fs-pin.assigned { opacity: .5; }
    :host ::ng-deep .leaflet-popup-content-wrapper {
      background: var(--fs-surface); color: var(--fs-text);
      border: 1px solid var(--fs-border); border-radius: var(--fs-radius-sm);
    }
    :host ::ng-deep .leaflet-popup-tip { background: var(--fs-surface); }
  `],
})
export class MapDispatchComponent implements AfterViewInit, OnDestroy {
  @ViewChild('map') mapEl!: ElementRef<HTMLElement>;

  readonly service = inject(WorkOrderService);
  private readonly auth = inject(AuthService);

  readonly technicians = signal<Technician[]>([]);
  readonly techniciansLoading = signal(true);
  readonly selectedOrderId = signal<string | null>(null);
  readonly orders = signal<WorkOrder[]>([]);
  readonly skeletonRows = Array.from({ length: 3 });

  readonly specialtyLabel = specialtyLabel;
  readonly specialtyIcon = specialtyIcon;

  /** La orden que el mapa tiene seleccionada, resuelta desde la lista viva de órdenes. */
  readonly selectedOrder = computed(
    () => this.orders().find((o) => o.id === this.selectedOrderId()) ?? null,
  );

  /** Especialidad que el título de la orden sugiere (palabras clave — ver specialty-match.ts). */
  readonly suggestedSpecialty = computed<Specialty | null>(() => {
    const order = this.selectedOrder();
    return order ? inferSpecialty(order.title) : null;
  });

  /** Técnicos con el que coincide la especialidad sugerida primero; nunca oculta al resto. */
  readonly rankedTechnicians = computed(() => {
    const suggested = this.suggestedSpecialty();
    const techs = this.technicians();
    if (!suggested) return techs;
    return [...techs].sort((a, b) => {
      const aMatch = a.specialty === suggested ? 0 : 1;
      const bMatch = b.specialty === suggested ? 0 : 1;
      return aMatch - bMatch;
    });
  });

  /** RBAC en el cliente: solo ADMIN/DISPATCHER asignan (el backend lo exige de todos modos). */
  readonly roleLabel = () => {
    const map: Record<string, string> = { ADMIN: 'Administrador', DISPATCHER: 'Despachador', TECHNICIAN: 'Técnico' };
    const r = this.auth.user()?.role;
    return r ? (map[r] ?? r) : '—';
  };
  readonly canAssign = () => {
    const r = this.auth.user()?.role;
    return r === 'ADMIN' || r === 'DISPATCHER';
  };

  private map!: L.Map;
  private readonly orderLayer = L.layerGroup();
  private readonly techLayer = L.layerGroup();
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
      this.loadTechnicians();
    } else {
      this.techniciansLoading.set(false);
    }
    this.subs.add(
      this.service.getWorkOrders().subscribe((orders) => {
        this.orders.set(orders);
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

  clearSelection(): void {
    this.selectedOrderId.set(null);
  }

  retry(): void {
    this.service.refresh();
    if (this.canAssign()) this.loadTechnicians();
  }

  formatScheduled(epochMs: number): string {
    return new Intl.DateTimeFormat('es-PE', {
      day: 'numeric',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(epochMs));
  }

  private loadTechnicians(): void {
    this.subs.add(
      this.service.getTechnicians().subscribe({
        next: (techs) => {
          this.technicians.set(techs);
          this.techniciansLoading.set(false);
          this.renderTechnicians(techs);
          this.renderOrders(this.orders());
        },
        error: () => this.techniciansLoading.set(false),
      }),
    );
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
        .bindPopup(
          `<b>${order.title}</b><br>${order.customerName}<br>${order.address}<br>${priorityLabel(order.priority)} · ${statusLabel(order.status)}`,
        )
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
        .bindPopup(
          `<b>${tech.name}</b><br>${specialtyIcon(tech.specialty)} ${specialtyLabel(tech.specialty)}<br>${tech.available ? 'Disponible' : 'Ocupado'}`,
        )
        .addTo(this.techLayer);
    }
  }
}
