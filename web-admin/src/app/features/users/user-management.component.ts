import { Component, inject, signal } from '@angular/core';
import { AsyncPipe, NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { UserManagementService } from '../../core/services/user-management.service';
import { UserRole } from '../../core/models/auth.model';

const ROLE_LABELS: Record<UserRole, string> = {
  ADMIN: 'Administrador',
  DISPATCHER: 'Despachador',
  TECHNICIAN: 'Técnico',
};

/**
 * Gestión de usuarios del tenant (equipo). Solo accesible para ADMIN — protegido por
 * `adminGuard` en el cliente y por RBAC en el backend (`GET`/`POST /api/users`).
 */
@Component({
  selector: 'fs-user-management',
  standalone: true,
  imports: [NgFor, NgIf, AsyncPipe, FormsModule],
  template: `
    <div class="header-row">
      <div>
        <h2>Equipo</h2>
        <p class="sub">Usuarios con acceso al panel de tu empresa</p>
      </div>
      <button class="primary" (click)="showForm.set(!showForm())">
        {{ showForm() ? 'Cancelar' : '+ Nuevo usuario' }}
      </button>
    </div>

    <form class="fs-card form-card" *ngIf="showForm()" (ngSubmit)="submit()">
      <div class="grid">
        <div>
          <label>Nombre</label>
          <input name="name" [(ngModel)]="name" required />
        </div>
        <div>
          <label>Correo electrónico</label>
          <input type="email" name="email" [(ngModel)]="email" required />
        </div>
        <div>
          <label>Contraseña</label>
          <input type="password" name="password" [(ngModel)]="password" minlength="6" required />
        </div>
        <div>
          <label>Rol</label>
          <select name="role" [(ngModel)]="role">
            <option value="ADMIN">Administrador</option>
            <option value="DISPATCHER">Despachador</option>
            <option value="TECHNICIAN">Técnico</option>
          </select>
        </div>
      </div>
      <div class="form-actions">
        <button type="submit" class="primary" [disabled]="submitting()">
          <span class="spinner" *ngIf="submitting()"></span>
          {{ submitting() ? 'Creando…' : 'Crear usuario' }}
        </button>
        <p class="err" *ngIf="error()">⚠ {{ error() }}</p>
      </div>
    </form>

    <div class="fs-error" *ngIf="service.error() as err">
      <span>⚠ {{ err }}</span>
      <button (click)="service.refresh()">Reintentar</button>
    </div>

    <div class="fs-card table-card" *ngIf="!service.error()">
      <div class="table-scroll">
      <table>
        <thead>
          <tr>
            <th>Nombre</th>
            <th>Correo</th>
            <th>Rol</th>
          </tr>
        </thead>
        <tbody *ngIf="service.loading(); else loaded">
          <tr *ngFor="let _ of skeletonRows">
            <td><span class="skeleton" style="width: 130px"></span></td>
            <td><span class="skeleton" style="width: 170px"></span></td>
            <td><span class="skeleton" style="width: 90px"></span></td>
          </tr>
        </tbody>
        <ng-template #loaded>
          <tbody>
            <tr *ngFor="let u of users$ | async">
              <td class="title-cell">{{ u.name }}</td>
              <td class="mono">{{ u.email }}</td>
              <td><span [class]="'badge badge-' + u.role.toLowerCase()">{{ roleLabel(u.role) }}</span></td>
            </tr>
          </tbody>
        </ng-template>
      </table>
      </div>
    </div>
  `,
  styles: [`
    .header-row { display: flex; flex-wrap: wrap; gap: 12px; align-items: flex-start; justify-content: space-between; margin-bottom: 20px; }
    h2 { margin: 0; font-size: 22px; }
    .sub { margin: 4px 0 0; color: var(--fs-text-faint); font-size: 13px; }

    .primary {
      background: var(--fs-primary); color: #fff; border: 0; border-radius: var(--fs-radius-sm);
      padding: 9px 16px; cursor: pointer; font-size: 13px; font-weight: 600;
      transition: background 0.15s;
      display: inline-flex; align-items: center; gap: 8px;
    }
    .primary:hover:not(:disabled) { background: var(--fs-primary-dark); }
    .primary:disabled { opacity: 0.6; cursor: default; }

    .form-card { padding: 20px; margin-bottom: 20px; }
    .grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
    label { display: block; font-size: 12px; color: var(--fs-text-muted); margin-bottom: 6px; font-weight: 500; }
    input, select {
      width: 100%; padding: 9px 11px; border-radius: var(--fs-radius-sm);
      border: 1px solid var(--fs-border); background: var(--fs-bg); color: var(--fs-text);
      font-size: 13px;
    }
    input:focus, select:focus { outline: none; border-color: var(--fs-primary); }
    .form-actions { margin-top: 16px; display: flex; align-items: center; gap: 14px; }
    .err { color: #f87171; font-size: 13px; margin: 0; }

    .table-card { overflow: hidden; }
    .table-scroll { overflow-x: auto; }
    table { width: 100%; min-width: 480px; border-collapse: collapse; }
    th, td { text-align: left; padding: 14px 18px; border-bottom: 1px solid var(--fs-border); font-size: 13px; }
    th {
      color: var(--fs-text-faint); font-weight: 600; text-transform: uppercase;
      font-size: 11px; letter-spacing: 0.04em;
    }
    tbody tr:hover { background: rgba(255, 255, 255, 0.02); }
    tbody tr:last-child td { border-bottom: none; }
    .mono { font-family: 'DM Mono', monospace; color: var(--fs-text-faint); font-size: 12px; }
    .title-cell { font-weight: 500; }

    @media (max-width: 640px) {
      .grid { grid-template-columns: 1fr; }
    }
  `],
})
export class UserManagementComponent {
  readonly service = inject(UserManagementService);

  readonly users$ = this.service.getUsers();
  readonly skeletonRows = Array.from({ length: 4 });
  readonly showForm = signal(false);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  name = '';
  email = '';
  password = '';
  role: UserRole = 'TECHNICIAN';

  constructor() {
    this.service.refresh();
  }

  roleLabel(role: UserRole): string {
    return ROLE_LABELS[role] ?? role;
  }

  submit(): void {
    this.submitting.set(true);
    this.error.set(null);
    this.service
      .createUser({ name: this.name, email: this.email, password: this.password, role: this.role })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.showForm.set(false);
          this.name = '';
          this.email = '';
          this.password = '';
          this.role = 'TECHNICIAN';
        },
        error: (err: HttpErrorResponse) => {
          this.submitting.set(false);
          this.error.set(
            err.status === 0
              ? 'Sin conexión con el servidor. Intenta de nuevo.'
              : err.status === 409
                ? 'Ese correo ya está registrado.'
                : (err.error?.message ?? 'No se pudo crear el usuario.')
          );
        },
      });
  }
}
