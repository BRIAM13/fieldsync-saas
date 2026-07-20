import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NgIf } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';

/** Pantalla de inicio de sesión del panel de despacho. */
@Component({
  selector: 'fs-login',
  standalone: true,
  imports: [FormsModule, NgIf],
  template: `
    <div class="wrap">
      <form class="card" (ngSubmit)="submit()">
        <h1>FieldSync</h1>
        <p class="sub">Panel de despacho</p>

        <label>Email</label>
        <input type="email" name="email" [(ngModel)]="email" autocomplete="username" required />

        <label>Contraseña</label>
        <input type="password" name="password" [(ngModel)]="password"
               autocomplete="current-password" required />

        <button type="submit" [disabled]="loading()">
          {{ loading() ? 'Ingresando…' : 'Ingresar' }}
        </button>

        <p class="err" *ngIf="error()">{{ error() }}</p>
        <p class="hint">Demo: admin&#64;fieldsync.dev / demo1234</p>
      </form>
    </div>
  `,
  styles: [`
    .wrap { min-height: 80vh; display: grid; place-items: center; }
    .card { display: flex; flex-direction: column; gap: 8px; width: 320px;
            background: var(--fs-surface); padding: 28px; border-radius: 12px; }
    h1 { margin: 0; color: var(--fs-primary); font-size: 24px; }
    .sub { margin: 0 0 12px; color: #94a3b8; font-size: 13px; }
    label { font-size: 12px; color: #94a3b8; margin-top: 6px; }
    input { padding: 10px; border-radius: 8px; border: 1px solid #334155;
            background: #0b1220; color: var(--fs-text); }
    button { margin-top: 14px; padding: 11px; border: 0; border-radius: 8px;
             background: var(--fs-primary); color: #fff; cursor: pointer; font-weight: 600; }
    button:disabled { opacity: .6; cursor: default; }
    .err { color: #f87171; font-size: 13px; }
    .hint { color: #64748b; font-size: 12px; margin-top: 4px; }
  `],
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  email = 'admin@fieldsync.dev';
  password = 'demo1234';
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  submit(): void {
    this.loading.set(true);
    this.error.set(null);
    this.auth.login({ email: this.email, password: this.password }).subscribe({
      next: () => this.router.navigate(['/dispatch']),
      error: () => {
        this.error.set('Credenciales inválidas');
        this.loading.set(false);
      },
    });
  }
}
