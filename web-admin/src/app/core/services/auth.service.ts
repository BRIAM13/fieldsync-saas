import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, throwError } from 'rxjs';
import { API_BASE } from '../api.config';
import { AuthResponse, LoginRequest, RegisterRequest, User } from '../models/auth.model';

const TOKEN_KEY = 'fieldsync.token';
const REFRESH_KEY = 'fieldsync.refresh';
const USER_KEY = 'fieldsync.user';

/**
 * Gestiona la sesión: login/registro/refresh contra el backend, persistencia del access +
 * refresh token en localStorage (la sesión sobrevive a recargas) y estado reactivo (signals).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly _user = signal<User | null>(this.readUser());
  readonly user = this._user.asReadonly();
  readonly isAuthenticated = computed(() => this._user() !== null);

  login(body: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${API_BASE}/auth/login`, body)
      .pipe(tap((res) => this.persist(res)));
  }

  register(body: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${API_BASE}/auth/register`, body)
      .pipe(tap((res) => this.persist(res)));
  }

  /** Renueva el access token con el refresh token guardado (rota el refresh token). */
  refresh(): Observable<AuthResponse> {
    const refreshToken = this.refreshToken();
    if (!refreshToken) return throwError(() => new Error('Sin refresh token'));
    return this.http
      .post<AuthResponse>(`${API_BASE}/auth/refresh`, { refreshToken })
      .pipe(tap((res) => this.persist(res)));
  }

  logout(): void {
    const refreshToken = this.refreshToken();
    if (refreshToken) {
      // Revoca el refresh token en el servidor (best-effort).
      this.http.post(`${API_BASE}/auth/logout`, { refreshToken }).subscribe({ error: () => {} });
    }
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USER_KEY);
    this._user.set(null);
  }

  token(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  refreshToken(): string | null {
    return localStorage.getItem(REFRESH_KEY);
  }

  private persist(res: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, res.token);
    localStorage.setItem(REFRESH_KEY, res.refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(res.user));
    this._user.set(res.user);
  }

  private readUser(): User | null {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as User) : null;
  }
}
