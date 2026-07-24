import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, fromEvent } from 'rxjs';
import { API_BASE } from '../api.config';
import {
  CompanySummary,
  CustomerServiceRequest,
  ServiceRequestCreate,
} from '../models/customer.model';

/**
 * Datos del portal de cliente: empresas públicas (para el registro) y las solicitudes propias
 * del cliente. Mismo patrón reactivo que `work-order.service.ts`/`user-management.service.ts`.
 */
@Injectable({ providedIn: 'root' })
export class CustomerPortalService {
  private readonly http = inject(HttpClient);
  private readonly requestsSubject = new BehaviorSubject<CustomerServiceRequest[]>([]);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  private hasLoadedOnce = false;

  constructor() {
    fromEvent(window, 'online').subscribe(() => this.refresh());
  }

  /** Lista pública de empresas — para el selector del registro de cliente. */
  getCompanies(): Observable<CompanySummary[]> {
    return this.http.get<CompanySummary[]>(`${API_BASE}/api/companies`);
  }

  getMyRequests(): Observable<CustomerServiceRequest[]> {
    return this.requestsSubject.asObservable();
  }

  refresh(): void {
    this.error.set(null);
    if (!this.hasLoadedOnce) this.loading.set(true);
    this.http.get<CustomerServiceRequest[]>(`${API_BASE}/api/service-requests/mine`).subscribe({
      next: (requests) => {
        this.requestsSubject.next(requests);
        this.loading.set(false);
        this.hasLoadedOnce = true;
      },
      error: () => {
        this.loading.set(false);
        this.error.set('No se pudieron cargar tus solicitudes.');
      },
    });
  }

  createServiceRequest(body: ServiceRequestCreate): Observable<CustomerServiceRequest> {
    return this.http.post<CustomerServiceRequest>(`${API_BASE}/api/service-requests`, body);
  }
}
