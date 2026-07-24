import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, fromEvent, tap } from 'rxjs';
import { CreateUserRequest, User } from '../models/auth.model';
import { API_BASE } from '../api.config';

/**
 * Gestión de usuarios del tenant, conectada al backend real (`GET`/`POST /api/users`,
 * ambos protegidos por RBAC — solo ADMIN). Mismo patrón que WorkOrderService: caché
 * reactiva local que los componentes observan con `async` pipe, más signals de
 * loading/error para que la pantalla muestre skeleton o un aviso con reintento.
 */
@Injectable({ providedIn: 'root' })
export class UserManagementService {
  private readonly http = inject(HttpClient);
  private readonly usersSubject = new BehaviorSubject<User[]>([]);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  private hasLoadedOnce = false;

  constructor() {
    fromEvent(window, 'online').subscribe(() => this.refresh());
  }

  getUsers(): Observable<User[]> {
    return this.usersSubject.asObservable();
  }

  /** Re-consulta la API y emite la lista fresca. */
  refresh(): void {
    this.error.set(null);
    if (!this.hasLoadedOnce) this.loading.set(true);
    this.http.get<User[]>(`${API_BASE}/api/users`).subscribe({
      next: (users) => {
        this.usersSubject.next(users);
        this.loading.set(false);
        this.hasLoadedOnce = true;
      },
      error: () => {
        this.loading.set(false);
        this.error.set('No se pudo cargar el equipo.');
      },
    });
  }

  /** Crea un usuario y refresca la lista al terminar. */
  createUser(body: CreateUserRequest): Observable<User> {
    return this.http
      .post<User>(`${API_BASE}/api/users`, body)
      .pipe(tap(() => this.refresh()));
  }
}
