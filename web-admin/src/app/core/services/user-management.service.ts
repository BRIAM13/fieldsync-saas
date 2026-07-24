import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { CreateUserRequest, User } from '../models/auth.model';
import { API_BASE } from '../api.config';

/**
 * Gestión de usuarios del tenant, conectada al backend real (`GET`/`POST /api/users`,
 * ambos protegidos por RBAC — solo ADMIN). Mismo patrón que WorkOrderService: caché
 * reactiva local que los componentes observan con `async` pipe.
 */
@Injectable({ providedIn: 'root' })
export class UserManagementService {
  private readonly http = inject(HttpClient);
  private readonly usersSubject = new BehaviorSubject<User[]>([]);

  getUsers(): Observable<User[]> {
    return this.usersSubject.asObservable();
  }

  /** Re-consulta la API y emite la lista fresca. */
  refresh(): void {
    this.http
      .get<User[]>(`${API_BASE}/api/users`)
      .subscribe((users) => this.usersSubject.next(users));
  }

  /** Crea un usuario y refresca la lista al terminar. */
  createUser(body: CreateUserRequest): Observable<User> {
    return this.http
      .post<User>(`${API_BASE}/api/users`, body)
      .pipe(tap(() => this.refresh()));
  }
}
