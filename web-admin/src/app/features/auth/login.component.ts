import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NgIf } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';

/** Pantalla de inicio de sesión del panel de despacho. */
@Component({
  selector: 'fs-login',
  standalone: true,
  imports: [FormsModule, NgIf],
  template: `
    <div class="wrap">
      <div class="panel">
        <div class="hero">
          <div class="mark">🔧</div>
          <h1>FieldSync</h1>
          <p>Gestión de servicios de campo</p>
        </div>

        <form class="form" (ngSubmit)="submit()">
          <label>Correo electrónico</label>
          <input type="email" name="email" [(ngModel)]="email" autocomplete="username" required />

          <label>Contraseña</label>
          <input
            type="password"
            name="password"
            [(ngModel)]="password"
            autocomplete="current-password"
            required
          />

          <button type="submit" [disabled]="loading()">
            <span class="spinner" *ngIf="loading()"></span>
            {{ loading() ? 'Ingresando…' : 'Ingresar' }}
          </button>

          <p class="err" *ngIf="error()">⚠ {{ error() }}</p>

          <div class="hint">
            Cuenta de demostración<br />
            <strong>admin&#64;fieldsync.dev</strong> / demo1234
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .wrap {
      min-height: 100vh;
      display: grid;
      place-items: center;
      padding: 24px;
    }
    .panel {
      width: 380px;
      border-radius: 20px;
      overflow: hidden;
      background: var(--fs-surface);
      border: 1px solid var(--fs-border);
      box-shadow: var(--fs-shadow);
    }
    .hero {
      padding: 36px 28px 28px;
      background: linear-gradient(135deg, var(--fs-primary), var(--fs-primary-dark));
      text-align: center;
    }
    .mark { font-size: 34px; margin-bottom: 8px; }
    .hero h1 { margin: 0; color: #fff; font-size: 22px; }
    .hero p { margin: 4px 0 0; color: rgba(255, 255, 255, 0.85); font-size: 13px; }

    .form { display: flex; flex-direction: column; gap: 6px; padding: 28px; }
    label { font-size: 12px; color: var(--fs-text-muted); margin-top: 10px; font-weight: 500; }
    input {
      padding: 12px 14px;
      border-radius: 12px;
      border: 1px solid var(--fs-border);
      background: var(--fs-bg);
      color: var(--fs-text);
      font-size: 14px;
      transition: border-color 0.15s;
    }
    input:focus { outline: none; border-color: var(--fs-primary); }

    button {
      margin-top: 20px;
      padding: 13px;
      border: 0;
      border-radius: 12px;
      background: var(--fs-primary);
      color: #fff;
      cursor: pointer;
      font-weight: 600;
      font-size: 14px;
      transition: background 0.15s;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }
    button:hover:not(:disabled) { background: var(--fs-primary-dark); }
    button:disabled { opacity: 0.6; cursor: default; }

    .err { color: #f87171; font-size: 13px; margin: 4px 0 0; }

    .hint {
      margin-top: 18px;
      padding: 12px;
      border-radius: 10px;
      background: var(--fs-surface-2);
      color: var(--fs-text-faint);
      font-size: 12px;
      text-align: center;
      line-height: 1.6;
    }
    .hint strong { color: var(--fs-text-muted); }
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
      error: (err: HttpErrorResponse) => {
        this.error.set(
          err.status === 0
            ? 'Sin conexión con el servidor. Verifica tu internet e intenta de nuevo.'
            : 'Credenciales inválidas',
        );
        this.loading.set(false);
      },
    });
  }
}
