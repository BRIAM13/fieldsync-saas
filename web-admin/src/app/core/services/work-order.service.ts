import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, fromEvent } from 'rxjs';
import { Technician, WorkOrder } from '../models/work-order.model';
import { API_BASE } from '../api.config';

/**
 * Servicio de órdenes de trabajo **conectado al backend real** (Ktor) vía HttpClient.
 *
 * Mantiene un BehaviorSubject como caché reactiva local: los componentes se suscriben a
 * `getWorkOrders()` con `async` pipe, y cada mutación (asignar) re-consulta la API y emite
 * la lista fresca — así la UI reacciona sin que los componentes conozcan el transporte HTTP.
 */
@Injectable({ providedIn: 'root' })
export class WorkOrderService {
  private readonly http = inject(HttpClient);
  private readonly ordersSubject = new BehaviorSubject<WorkOrder[]>([]);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  private hasLoadedOnce = false;

  constructor() {
    this.refresh();
    // Al recuperar la conexión, refresca en silencio — solo muestra el skeleton de nuevo
    // si nunca llegó a cargar datos (evita parpadeos en refrescos de fondo).
    fromEvent(window, 'online').subscribe(() => this.refresh());
  }

  /** Stream de órdenes que los componentes observan. */
  getWorkOrders(): Observable<WorkOrder[]> {
    return this.ordersSubject.asObservable();
  }

  /** Re-consulta la API y emite la lista fresca. */
  refresh(): void {
    this.error.set(null);
    if (!this.hasLoadedOnce) this.loading.set(true);
    this.http.get<WorkOrder[]>(`${API_BASE}/api/work-orders`).subscribe({
      next: (orders) => {
        this.ordersSubject.next(orders);
        this.loading.set(false);
        this.hasLoadedOnce = true;
      },
      error: () => {
        this.loading.set(false);
        this.error.set('No se pudieron cargar las órdenes de trabajo.');
      },
    });
  }

  /** Técnicos disponibles para asignar (una consulta puntual). */
  getTechnicians(): Observable<Technician[]> {
    return this.http.get<Technician[]>(`${API_BASE}/api/technicians`);
  }

  /** Asigna una orden a un técnico en el backend y refresca la lista. */
  assignOrder(orderId: string, technicianId: string): void {
    this.http
      .patch<WorkOrder>(`${API_BASE}/api/work-orders/${orderId}/assignment`, {
        technicianId,
      })
      .subscribe(() => this.refresh());
  }
}
