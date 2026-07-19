import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, delay, of } from 'rxjs';
import { Technician, WorkOrder } from '../models/work-order.model';

/**
 * Servicio de API SIMULADO para las órdenes de trabajo.
 *
 * Expone el estado como Observables (patrón reactivo de Angular con RxJS).
 * En producción se cambiaría el cuerpo de cada método por llamadas HttpClient,
 * sin tocar los componentes que lo consumen.
 */
@Injectable({ providedIn: 'root' })
export class WorkOrderService {
  private readonly orders$ = new BehaviorSubject<WorkOrder[]>(SEED_ORDERS);

  /** Stream de órdenes que los componentes observan. */
  getWorkOrders(): Observable<WorkOrder[]> {
    return this.orders$.asObservable();
  }

  /** Técnicos disponibles para asignar (latencia simulada). */
  getTechnicians(): Observable<Technician[]> {
    return of(SEED_TECHNICIANS).pipe(delay(300));
  }

  /** Asigna una orden a un técnico y emite el nuevo estado. */
  assignOrder(orderId: string, technicianId: string): void {
    const next = this.orders$.value.map((o) =>
      o.id === orderId
        ? { ...o, assignedTechnicianId: technicianId, status: 'ASSIGNED' as const }
        : o
    );
    this.orders$.next(next);
  }
}

const SEED_TECHNICIANS: Technician[] = [
  { id: 'T-01', name: 'Carlos Ramírez', location: { lat: -12.046, lng: -77.043 }, available: true },
  { id: 'T-02', name: 'Lucía Fernández', location: { lat: -12.089, lng: -77.021 }, available: true },
  { id: 'T-03', name: 'Miguel Torres', location: { lat: -12.112, lng: -76.998 }, available: false },
];

const SEED_ORDERS: WorkOrder[] = [
  { id: 'WO-1042', title: 'Fuga en tubería principal', customerName: 'Ferretería El Sol',
    address: 'Av. Los Álamos 234', location: { lat: -12.05, lng: -77.04 },
    priority: 'URGENT', status: 'UNASSIGNED', assignedTechnicianId: null },
  { id: 'WO-1043', title: 'Instalación de tablero eléctrico', customerName: 'Condominio Las Palmas',
    address: 'Jr. Independencia 87', location: { lat: -12.08, lng: -77.02 },
    priority: 'HIGH', status: 'UNASSIGNED', assignedTechnicianId: null },
  { id: 'WO-1044', title: 'Mantenimiento de calentador', customerName: 'Sra. Quispe',
    address: 'Calle Lima 12', location: { lat: -12.10, lng: -77.00 },
    priority: 'MEDIUM', status: 'UNASSIGNED', assignedTechnicianId: null },
];
