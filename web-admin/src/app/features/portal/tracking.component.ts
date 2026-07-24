import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { NgIf } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import * as L from 'leaflet';
import { CustomerTrackingService, TechnicianLocation } from '../../core/services/customer-tracking.service';
import { CustomerAuthService } from '../../core/services/customer-auth.service';

/**
 * Seguimiento en vivo de una solicitud — mismo mapa Leaflet que usa el staff en
 * `map-dispatch.component.ts`, pero con un único pin: el técnico, actualizándose en tiempo
 * real vía WebSocket (`CustomerTrackingService`).
 */
@Component({
  selector: 'fs-portal-tracking',
  standalone: true,
  imports: [NgIf, RouterLink],
  template: `
    <div class="header-row">
      <div>
        <h2>Seguimiento en vivo</h2>
        <p class="sub">{{ position() ? 'Tu técnico está en camino' : 'Conectando…' }}</p>
      </div>
      <a routerLink="/portal" class="back-link">← Volver</a>
    </div>

    <div class="map-card">
      <div #map class="map"></div>
      <div class="map-loading" *ngIf="!position() && !error()">
        <span class="spinner"></span> Conectando con el técnico…
      </div>
    </div>

    <div class="fs-error" *ngIf="error()">
      <span>⚠ {{ error() }}</span>
    </div>

    <div class="fs-card eta-card" *ngIf="position() as p">
      <div class="tech-dot"></div>
      <div>
        <div class="tech-name">{{ p.technicianName }}</div>
        <div class="tech-eta">
          {{ p.arrived ? '¡Tu técnico ha llegado!' : 'Llega en ~' + p.etaMinutes + ' min' }}
        </div>
      </div>
    </div>
  `,
  styles: [`
    .header-row { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 20px; }
    h2 { margin: 0; font-size: 22px; }
    .sub { margin: 4px 0 0; color: var(--fs-text-faint); font-size: 13px; }
    .back-link { color: var(--fs-text-muted); text-decoration: none; font-size: 13px; font-weight: 600; }
    .back-link:hover { color: var(--fs-text); }

    .map-card {
      position: relative;
      border-radius: var(--fs-radius-lg);
      overflow: hidden;
      border: 1px solid var(--fs-border);
      box-shadow: var(--fs-shadow);
    }
    .map { height: 420px; }
    .map-loading {
      position: absolute; inset: 0;
      display: flex; align-items: center; justify-content: center; gap: 10px;
      background: rgba(11, 18, 32, 0.72); backdrop-filter: blur(2px);
      color: var(--fs-text-muted); font-size: 13px; font-weight: 500;
    }

    .eta-card {
      margin-top: 16px; padding: 18px; display: flex; align-items: center; gap: 14px;
    }
    .tech-dot {
      width: 12px; height: 12px; border-radius: 50%; background: #4ade80; flex-shrink: 0;
      box-shadow: 0 0 0 4px rgba(74, 222, 128, 0.18);
    }
    .tech-name { font-size: 16px; font-weight: 700; color: var(--fs-text); }
    .tech-eta { font-size: 14px; color: #60a5fa; margin-top: 2px; }

    :host ::ng-deep .fs-pin { font-size: 26px; filter: drop-shadow(0 1px 2px rgba(0,0,0,.4)); }
  `],
})
export class PortalTrackingComponent implements AfterViewInit, OnDestroy {
  @ViewChild('map') mapEl!: ElementRef<HTMLElement>;

  private readonly route = inject(ActivatedRoute);
  private readonly tracking = inject(CustomerTrackingService);
  private readonly customerAuth = inject(CustomerAuthService);

  readonly position = signal<TechnicianLocation | null>(null);
  readonly error = signal<string | null>(null);

  private map!: L.Map;
  private marker: L.Marker | null = null;
  private sub?: Subscription;

  ngAfterViewInit(): void {
    this.map = L.map(this.mapEl.nativeElement, { center: [-12.06, -77.03], zoom: 13 });
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap',
      maxZoom: 19,
    }).addTo(this.map);
    setTimeout(() => this.map.invalidateSize(), 0);

    const orderId = this.route.snapshot.paramMap.get('id');
    const token = this.customerAuth.token();
    if (!orderId || !token) {
      this.error.set('No se pudo iniciar el seguimiento.');
      return;
    }

    this.sub = this.tracking.track(orderId, token).subscribe({
      next: (loc) => {
        this.position.set(loc);
        this.renderMarker(loc);
      },
      error: () => this.error.set('Se perdió la conexión con el seguimiento en vivo.'),
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    this.map?.remove();
  }

  private renderMarker(loc: TechnicianLocation): void {
    if (!this.map) return;
    if (!this.marker) {
      this.marker = L.marker([loc.lat, loc.lng], {
        icon: L.divIcon({ className: 'fs-pin', html: '🔧', iconSize: [26, 26], iconAnchor: [13, 24] }),
      }).addTo(this.map);
    } else {
      this.marker.setLatLng([loc.lat, loc.lng]);
    }
    this.map.panTo([loc.lat, loc.lng]);
  }
}
