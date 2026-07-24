import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, throwError } from 'rxjs';
import { API_BASE } from '../api.config';
import {
  Customer,
  CustomerAuthResponse,
  CustomerLoginRequest,
  CustomerRegisterRequest,
} from '../models/customer.model';

const TOKEN_KEY = 'fieldsync.customer.token';
const REFRESH_KEY = 'fieldsync.customer.refresh';
const CUSTOMER_KEY = 'fieldsync.customer.profile';

/**
 * Sesión de **cliente** — separada por completo de [AuthService] (staff): claves de
 * localStorage distintas, endpoints `/customer/auth/*`, y un JWT de otro proveedor
 * (`AUTH_JWT_CUSTOMER` en el backend). Ambas sesiones pueden coexistir sin pisarse.
 */
@Injectable({ providedIn: 'root' })
export class CustomerAuthService {
  private readonly http = inject(HttpClient);

  private readonly _customer = signal<Customer | null>(this.readCustomer());
  readonly customer = this._customer.asReadonly();
  readonly isAuthenticated = computed(() => this._customer() !== null);

  login(body: CustomerLoginRequest): Observable<CustomerAuthResponse> {
    return this.http
      .post<CustomerAuthResponse>(`${API_BASE}/customer/auth/login`, body)
      .pipe(tap((res) => this.persist(res)));
  }

  register(body: CustomerRegisterRequest): Observable<CustomerAuthResponse> {
    return this.http
      .post<CustomerAuthResponse>(`${API_BASE}/customer/auth/register`, body)
      .pipe(tap((res) => this.persist(res)));
  }

  /** Renueva el access token con el refresh token guardado (rota el refresh token). */
  refresh(): Observable<CustomerAuthResponse> {
    const refreshToken = this.refreshToken();
    if (!refreshToken) return throwError(() => new Error('Sin refresh token'));
    return this.http
      .post<CustomerAuthResponse>(`${API_BASE}/customer/auth/refresh`, { refreshToken })
      .pipe(tap((res) => this.persist(res)));
  }

  logout(): void {
    const refreshToken = this.refreshToken();
    if (refreshToken) {
      // Revoca el refresh token en el servidor (best-effort).
      this.http
        .post(`${API_BASE}/customer/auth/logout`, { refreshToken })
        .subscribe({ error: () => {} });
    }
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(CUSTOMER_KEY);
    this._customer.set(null);
  }

  token(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  refreshToken(): string | null {
    return localStorage.getItem(REFRESH_KEY);
  }

  private persist(res: CustomerAuthResponse): void {
    localStorage.setItem(TOKEN_KEY, res.token);
    localStorage.setItem(REFRESH_KEY, res.refreshToken);
    localStorage.setItem(CUSTOMER_KEY, JSON.stringify(res.customer));
    this._customer.set(res.customer);
  }

  private readCustomer(): Customer | null {
    const raw = localStorage.getItem(CUSTOMER_KEY);
    return raw ? (JSON.parse(raw) as Customer) : null;
  }
}
