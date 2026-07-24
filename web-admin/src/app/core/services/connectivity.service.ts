import { Injectable, signal } from '@angular/core';
import { fromEvent } from 'rxjs';

/**
 * Estado de conectividad global: si el navegador está offline y si la última
 * petición al backend falló por red (status 0 — CORS/timeout/servidor caído).
 * Los servicios de datos reportan aquí para que la UI muestre un aviso único
 * y consistente en vez de que cada pantalla maneje su propio mensaje de red.
 */
@Injectable({ providedIn: 'root' })
export class ConnectivityService {
  readonly online = signal(navigator.onLine);
  readonly backendReachable = signal(true);

  constructor() {
    fromEvent(window, 'online').subscribe(() => this.online.set(true));
    fromEvent(window, 'offline').subscribe(() => this.online.set(false));
  }

  reportRequestFailed(): void {
    this.backendReachable.set(false);
  }

  reportRequestSucceeded(): void {
    this.backendReachable.set(true);
  }
}
