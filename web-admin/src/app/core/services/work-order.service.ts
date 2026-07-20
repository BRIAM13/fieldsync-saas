import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
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

  constructor() {
    this.refresh();
  }

  /** Stream de órdenes que los componentes observan. */
  getWorkOrders(): Observable<WorkOrder[]> {
    return this.ordersSubject.asObservable();
  }

  /** Re-consulta la API y emite la lista fresca. */
  refresh(): void {
    this.http
      .get<WorkOrder[]>(`${API_BASE}/api/work-orders`)
      .subscribe((orders) => this.ordersSubject.next(orders));
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
