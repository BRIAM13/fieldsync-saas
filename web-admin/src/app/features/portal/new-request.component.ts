import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgFor, NgIf } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { CustomerPortalService } from '../../core/services/customer-portal.service';
import { Priority } from '../../core/models/work-order.model';

const PRIORITIES: { value: Priority; label: string }[] = [
  { value: 'LOW', label: 'Baja' },
  { value: 'MEDIUM', label: 'Media' },
  { value: 'HIGH', label: 'Alta' },
  { value: 'URGENT', label: 'Urgente' },
];

/**
 * Formulario de nueva solicitud. Usa la geolocalización del **navegador** (misma idea que
 * `expo-location` en la app móvil) — el envío queda deshabilitado hasta capturar la ubicación,
 * para que la orden llegue con `location` real y aparezca como pin en el mapa de despacho.
 */
@Component({
  selector: 'fs-new-request',
  standalone: true,
  imports: [FormsModule, NgIf, NgFor, RouterLink],
  template: `
    <div class="header-row">
      <div>
        <h2>Nueva solicitud</h2>
        <p class="sub">Cuéntanos qué necesitas y dónde</p>
      </div>
      <a routerLink="/portal" class="back-link">← Volver</a>
    </div>

    <form class="fs-card form-card" (ngSubmit)="submit()">
      <label>¿Qué necesitas?</label>
      <input name="title" [(ngModel)]="title" placeholder="Ej. Fuga en tubería principal" required />

      <label>Dirección</label>
      <input name="address" [(ngModel)]="address" placeholder="Calle, número, referencia" required />

      <label>Prioridad</label>
      <div class="chip-row">
        <button
          type="button"
          *ngFor="let p of priorities"
          class="chip"
          [class.selected]="priority === p.value"
          (click)="priority = p.value"
        >
          {{ p.label }}
        </button>
      </div>

      <label>Ubicación</label>
      <button type="button" class="location-btn" (click)="captureLocation()" [disabled]="locating()">
        <span class="spinner" *ngIf="locating()"></span>
        {{ locationLabel() }}
      </button>
      <p class="err" *ngIf="locationError()">⚠ {{ locationError() }}</p>

      <p class="err" *ngIf="error()">⚠ {{ error() }}</p>

      <button type="submit" class="submit-btn" [disabled]="!location || submitting()">
        <span class="spinner" *ngIf="submitting()"></span>
        {{ submitting() ? 'Enviando…' : (location ? 'Enviar solicitud' : 'Captura tu ubicación para continuar') }}
      </button>
    </form>
  `,
  styles: [`
    .header-row { display: flex; flex-wrap: wrap; gap: 12px; align-items: flex-start; justify-content: space-between; margin-bottom: 20px; }
    h2 { margin: 0; font-size: 22px; }
    .sub { margin: 4px 0 0; color: var(--fs-text-faint); font-size: 13px; }
    .back-link { color: var(--fs-text-muted); text-decoration: none; font-size: 13px; font-weight: 600; }
    .back-link:hover { color: var(--fs-text); }

    .form-card { padding: 24px; max-width: 520px; display: flex; flex-direction: column; }
    label { font-size: 12px; color: var(--fs-text-muted); margin: 14px 0 6px; font-weight: 500; }
    label:first-child { margin-top: 0; }
    input {
      padding: 11px 13px; border-radius: var(--fs-radius-sm);
      border: 1px solid var(--fs-border); background: var(--fs-bg); color: var(--fs-text);
      font-size: 14px;
    }
    input:focus { outline: none; border-color: var(--fs-primary); }

    .chip-row { display: flex; flex-wrap: wrap; gap: 8px; }
    .chip {
      padding: 8px 14px; border-radius: 100px; background: var(--fs-bg);
      border: 1px solid var(--fs-border); color: var(--fs-text-muted);
      font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.15s;
    }
    .chip.selected { background: var(--fs-primary-light); border-color: var(--fs-primary); color: #60a5fa; }

    .location-btn {
      display: flex; align-items: center; justify-content: center; gap: 8px;
      padding: 12px; border-radius: var(--fs-radius-sm);
      background: var(--fs-bg); border: 1px solid var(--fs-border); color: var(--fs-text);
      font-size: 13px; font-weight: 600; cursor: pointer;
    }
    .location-btn:disabled { opacity: 0.7; cursor: default; }

    .submit-btn {
      margin-top: 22px; padding: 14px; border: 0; border-radius: var(--fs-radius-sm);
      background: var(--fs-primary); color: #fff; font-weight: 700; font-size: 14px;
      cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 8px;
      transition: background 0.15s;
    }
    .submit-btn:hover:not(:disabled) { background: var(--fs-primary-dark); }
    .submit-btn:disabled { background: var(--fs-surface-2); cursor: default; }

    .err { color: #f87171; font-size: 13px; margin: 6px 0 0; }
  `],
})
export class NewRequestComponent {
  private readonly service = inject(CustomerPortalService);
  private readonly router = inject(Router);

  readonly priorities = PRIORITIES;

  title = '';
  address = '';
  priority: Priority = 'MEDIUM';
  location: { lat: number; lng: number } | null = null;

  readonly locating = signal(false);
  readonly locationError = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  locationLabel(): string {
    if (this.locating()) return 'Obteniendo ubicación…';
    if (this.location) return `📍 Ubicación capturada (${this.location.lat.toFixed(4)}, ${this.location.lng.toFixed(4)})`;
    return '📍 Usar mi ubicación actual';
  }

  captureLocation(): void {
    if (!navigator.geolocation) {
      this.locationError.set('Tu navegador no soporta geolocalización.');
      return;
    }
    this.locating.set(true);
    this.locationError.set(null);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        this.location = { lat: pos.coords.latitude, lng: pos.coords.longitude };
        this.locating.set(false);
      },
      () => {
        this.locationError.set('No se pudo obtener tu ubicación. Revisa los permisos del navegador.');
        this.locating.set(false);
      },
    );
  }

  submit(): void {
    if (!this.location) return;
    if (!this.title.trim() || !this.address.trim()) {
      this.error.set('Título y dirección son obligatorios');
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.service
      .createServiceRequest({
        title: this.title.trim(),
        address: this.address.trim(),
        priority: this.priority,
        lat: this.location.lat,
        lng: this.location.lng,
      })
      .subscribe({
        next: () => {
          this.service.refresh();
          this.router.navigate(['/portal']);
        },
        error: (err: HttpErrorResponse) => {
          this.submitting.set(false);
          this.error.set(
            err.status === 0
              ? 'Sin conexión con el servidor. Intenta de nuevo.'
              : (err.error?.message ?? 'No se pudo enviar la solicitud.'),
          );
        },
      });
  }
}
