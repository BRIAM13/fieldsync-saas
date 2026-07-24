import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgFor, NgIf } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';
import { CustomerAuthService } from '../../core/services/customer-auth.service';
import { CustomerPortalService } from '../../core/services/customer-portal.service';
import { CompanySummary } from '../../core/models/customer.model';

type Audience = 'empresa' | 'cliente';
type Mode = 'login' | 'register';

const GENERIC_EMAIL_DOMAINS = ['gmail.com', 'hotmail.com', 'outlook.com', 'yahoo.com', 'live.com', 'icloud.com'];

/**
 * Acceso único y dinámico del panel: elige **Empresa** o **Cliente**, y dentro de cada una
 * alterna entre **Iniciar sesión** y **Registrarse** con una tarjeta que se desliza (mismo
 * patrón visual de las referencias, en el tema oscuro de FieldSync). Reemplaza los antiguos
 * `login.component.ts`/`register.component.ts` — ahora también sirve a los clientes, que antes
 * solo podían entrar desde la app móvil.
 */
@Component({
  selector: 'fs-auth-gateway',
  standalone: true,
  imports: [FormsModule, NgIf, NgFor, RouterLink],
  template: `
    <div class="wrap">
      <div class="stage">
        <div class="accent-panel" [class.on-left]="mode() === 'register'">
          <div class="accent-content">
            <ng-container *ngIf="mode() === 'login'; else toLogin">
              <h2>¿Aún no tienes cuenta?</h2>
              <p>Regístrate para que puedas iniciar sesión</p>
              <button type="button" class="ghost-btn" (click)="setMode('register')">Registrarse</button>
            </ng-container>
            <ng-template #toLogin>
              <h2>¿Ya tienes una cuenta?</h2>
              <p>Inicia sesión para entrar al panel</p>
              <button type="button" class="ghost-btn" (click)="setMode('login')">Iniciar sesión</button>
            </ng-template>
          </div>
        </div>

        <div class="form-card" [class.on-right]="mode() === 'register'">
          <div class="audience-toggle">
            <div class="toggle-indicator" [class.right]="audience() === 'cliente'"></div>
            <button type="button" [class.active]="audience() === 'empresa'" (click)="setAudience('empresa')">
              🏢 Empresa
            </button>
            <button type="button" [class.active]="audience() === 'cliente'" (click)="setAudience('cliente')">
              🧑 Cliente
            </button>
          </div>

          <h1>{{ mode() === 'login' ? 'Iniciar sesión' : 'Registrarse' }}</h1>
          <p class="sub">
            {{ audience() === 'empresa' ? 'Panel de administración de tu empresa' : 'Solicita y sigue tus servicios' }}
          </p>

          <form (ngSubmit)="submit()">
            <!-- Honeypot: invisible para una persona, un bot suele llenarlo igual. -->
            <input
              type="text"
              name="website"
              [(ngModel)]="honeypot"
              class="honeypot"
              tabindex="-1"
              autocomplete="off"
              aria-hidden="true"
            />

            <!-- Registro de empresa: nombre de la empresa -->
            <ng-container *ngIf="audience() === 'empresa' && mode() === 'register'">
              <label>Nombre de la empresa</label>
              <input name="companyName" [(ngModel)]="companyName" required />
            </ng-container>

            <!-- Registro de cliente: elegir empresa -->
            <ng-container *ngIf="audience() === 'cliente' && mode() === 'register'">
              <label>Empresa</label>
              <div class="company-list">
                <button
                  type="button"
                  *ngFor="let c of companies()"
                  class="company-chip"
                  [class.selected]="companyId === c.id"
                  (click)="companyId = c.id"
                >
                  {{ c.name }}
                </button>
                <span class="hint" *ngIf="!companies().length">Cargando empresas…</span>
              </div>
            </ng-container>

            <ng-container *ngIf="mode() === 'register'">
              <label>Tu nombre</label>
              <input name="name" [(ngModel)]="name" required />
            </ng-container>

            <label>Correo electrónico</label>
            <input type="email" name="email" [(ngModel)]="email" autocomplete="username" required />
            <p class="tip" *ngIf="audience() === 'empresa' && mode() === 'register' && isGenericEmail()">
              💡 Tip: usa el correo de tu empresa si tienes uno — mejora la confianza de tus clientes.
            </p>

            <ng-container *ngIf="audience() === 'cliente' && mode() === 'register'">
              <label>Teléfono (opcional)</label>
              <input name="phone" [(ngModel)]="phone" />
            </ng-container>

            <label>Contraseña</label>
            <input
              type="password"
              name="password"
              [(ngModel)]="password"
              [autocomplete]="mode() === 'login' ? 'current-password' : 'new-password'"
              minlength="6"
              required
            />

            <button type="submit" class="submit-btn" [disabled]="loading()">
              <span class="spinner" *ngIf="loading()"></span>
              {{ submitLabel() }}
            </button>

            <p class="err" *ngIf="error()">⚠ {{ error() }}</p>

            <div class="hint-box" *ngIf="audience() === 'empresa' && mode() === 'login'">
              Cuenta de demostración<br />
              <strong>admin&#64;fieldsync.dev</strong> / demo1234
            </div>
          </form>
        </div>
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

    .stage {
      position: relative;
      width: min(880px, 100%);
      height: 620px;
      max-height: 90vh;
      border-radius: 24px;
      overflow: hidden;
      box-shadow: var(--fs-shadow);
      background: var(--fs-bg);
    }

    .accent-panel {
      position: absolute;
      top: 0;
      left: 45%;
      width: 55%;
      height: 100%;
      z-index: 1;
      background: linear-gradient(150deg, var(--fs-primary), var(--fs-primary-dark) 70%);
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 48px;
      transition: left 0.65s cubic-bezier(0.65, 0, 0.35, 1);
    }
    .accent-panel.on-left { left: 0; }
    .accent-content { color: #fff; max-width: 320px; }
    .accent-content h2 { margin: 0 0 10px; font-size: 24px; font-family: 'Space Grotesk', sans-serif; }
    .accent-content p { margin: 0 0 22px; color: rgba(255, 255, 255, 0.85); font-size: 14px; line-height: 1.5; }
    .ghost-btn {
      background: transparent;
      border: 1.5px solid #fff;
      color: #fff;
      padding: 10px 22px;
      border-radius: 10px;
      font-weight: 600;
      font-size: 14px;
      cursor: pointer;
      transition: background 0.15s;
    }
    .ghost-btn:hover { background: rgba(255, 255, 255, 0.12); }

    .form-card {
      position: absolute;
      top: 0;
      left: 0;
      width: 55%;
      height: 100%;
      z-index: 2;
      background: var(--fs-surface);
      padding: 40px 44px;
      overflow-y: auto;
      transition: left 0.65s cubic-bezier(0.65, 0, 0.35, 1);
    }
    .form-card.on-right { left: 45%; }

    .audience-toggle {
      position: relative;
      display: flex;
      background: var(--fs-bg);
      border: 1px solid var(--fs-border);
      border-radius: 100px;
      padding: 4px;
      margin-bottom: 24px;
    }
    .audience-toggle button {
      position: relative;
      z-index: 1;
      flex: 1;
      background: transparent;
      border: 0;
      padding: 9px 0;
      border-radius: 100px;
      font-size: 13px;
      font-weight: 600;
      color: var(--fs-text-muted);
      cursor: pointer;
      transition: color 0.2s;
    }
    .audience-toggle button.active { color: #fff; }
    .toggle-indicator {
      position: absolute;
      top: 4px;
      left: 4px;
      width: calc(50% - 4px);
      height: calc(100% - 8px);
      background: var(--fs-primary);
      border-radius: 100px;
      transition: transform 0.3s cubic-bezier(0.65, 0, 0.35, 1);
    }
    .toggle-indicator.right { transform: translateX(100%); }

    h1 { margin: 0; font-size: 24px; font-family: 'Space Grotesk', sans-serif; }
    .sub { margin: 4px 0 22px; color: var(--fs-text-faint); font-size: 13px; }

    form { display: flex; flex-direction: column; gap: 4px; }
    label { font-size: 12px; color: var(--fs-text-muted); margin-top: 10px; font-weight: 500; }
    input {
      padding: 11px 13px;
      border-radius: 10px;
      border: 1px solid var(--fs-border);
      background: var(--fs-bg);
      color: var(--fs-text);
      font-size: 14px;
      transition: border-color 0.15s;
    }
    input:focus { outline: none; border-color: var(--fs-primary); }

    .honeypot { position: absolute; left: -9999px; width: 0; height: 0; opacity: 0; pointer-events: none; }

    .tip { margin: 6px 0 0; font-size: 11px; color: #facc15; line-height: 1.4; }

    .company-list { display: flex; flex-wrap: wrap; gap: 8px; }
    .company-chip {
      padding: 7px 13px;
      border-radius: 100px;
      background: var(--fs-bg);
      border: 1px solid var(--fs-border);
      color: var(--fs-text-muted);
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.15s;
    }
    .company-chip.selected {
      background: var(--fs-primary-light);
      border-color: var(--fs-primary);
      color: #60a5fa;
    }
    .hint { color: var(--fs-text-faint); font-size: 12px; }

    .submit-btn {
      margin-top: 22px;
      padding: 13px;
      border: 0;
      border-radius: 10px;
      background: var(--fs-primary);
      color: #fff;
      cursor: pointer;
      font-weight: 700;
      font-size: 14px;
      transition: background 0.15s;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }
    .submit-btn:hover:not(:disabled) { background: var(--fs-primary-dark); }
    .submit-btn:disabled { opacity: 0.6; cursor: default; }

    .err { color: #f87171; font-size: 13px; margin: 10px 0 0; }

    .hint-box {
      margin-top: 18px;
      padding: 11px;
      border-radius: 10px;
      background: var(--fs-surface-2);
      color: var(--fs-text-faint);
      font-size: 12px;
      text-align: center;
      line-height: 1.6;
    }
    .hint-box strong { color: var(--fs-text-muted); }

    @media (max-width: 760px) {
      .stage { height: auto; min-height: 640px; }
      .accent-panel, .form-card { position: relative; width: 100%; left: 0 !important; transition: none; }
      .accent-panel { order: 2; padding: 32px 24px; }
      .form-card { order: 1; padding: 32px 24px; }
      .stage { display: flex; flex-direction: column; }
    }
  `],
})
export class AuthGatewayComponent {
  private readonly auth = inject(AuthService);
  private readonly customerAuth = inject(CustomerAuthService);
  private readonly portal = inject(CustomerPortalService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly audience = signal<Audience>('empresa');
  readonly mode = signal<Mode>(this.route.snapshot.data['mode'] === 'register' ? 'register' : 'login');
  readonly companies = signal<CompanySummary[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  companyName = '';
  companyId = '';
  name = '';
  email = '';
  phone = '';
  password = '';
  /** Honeypot: nunca debería llenarse por una persona real. */
  honeypot = '';

  constructor() {
    this.portal.getCompanies().subscribe((list) => {
      this.companies.set(list);
      if (list.length > 0 && !this.companyId) this.companyId = list[0].id;
    });
  }

  setMode(mode: Mode): void {
    this.mode.set(mode);
    this.error.set(null);
  }

  setAudience(audience: Audience): void {
    this.audience.set(audience);
    this.error.set(null);
  }

  isGenericEmail(): boolean {
    const domain = this.email.split('@')[1]?.toLowerCase();
    return !!domain && GENERIC_EMAIL_DOMAINS.includes(domain);
  }

  submitLabel(): string {
    if (this.loading()) return this.mode() === 'login' ? 'Ingresando…' : 'Creando cuenta…';
    return this.mode() === 'login' ? 'Ingresar' : 'Crear cuenta';
  }

  submit(): void {
    this.loading.set(true);
    this.error.set(null);

    const onError = (err: HttpErrorResponse) => {
      this.loading.set(false);
      this.error.set(
        err.status === 0
          ? 'Sin conexión con el servidor. Intenta de nuevo.'
          : err.status === 429
            ? 'Demasiados intentos. Espera unos minutos e intenta de nuevo.'
            : (err.error?.message ?? 'Ocurrió un error. Intenta de nuevo.'),
      );
    };

    if (this.audience() === 'empresa') {
      const staffCall =
        this.mode() === 'login'
          ? this.auth.login({ email: this.email, password: this.password })
          : this.auth.register({
              companyName: this.companyName,
              name: this.name,
              email: this.email,
              password: this.password,
              website: this.honeypot,
            });
      staffCall.subscribe({
        next: () => this.router.navigate(['/dispatch']),
        error: onError,
      });
    } else {
      const customerCall =
        this.mode() === 'login'
          ? this.customerAuth.login({ email: this.email, password: this.password })
          : this.customerAuth.register({
              companyId: this.companyId,
              name: this.name,
              email: this.email,
              phone: this.phone || undefined,
              password: this.password,
              website: this.honeypot,
            });
      customerCall.subscribe({
        next: () => this.router.navigate(['/portal']),
        error: onError,
      });
    }
  }
}
