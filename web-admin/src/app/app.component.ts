import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet, Router } from '@angular/router';
import { NgIf } from '@angular/common';
import { AuthService } from './core/services/auth.service';
import { CustomerAuthService } from './core/services/customer-auth.service';
import { ConnectivityService } from './core/services/connectivity.service';

const ROLE_LABELS: Record<string, string> = {
  ADMIN: 'Administrador',
  DISPATCHER: 'Despachador',
  TECHNICIAN: 'Técnico',
};

/**
 * Shell del panel: cabecera con marca, navegación y sesión activa, y <router-outlet>.
 * Convive con **dos** identidades independientes (staff y cliente, ver [AuthService] y
 * [CustomerAuthService]) — la cabecera se adapta a la que esté activa.
 */
@Component({
  selector: 'fs-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NgIf],
  template: `
    <div class="shell">
      <div class="conn-banner offline" *ngIf="!connectivity.online()">
        <span class="dot"></span> Sin conexión a internet — los cambios se reintentarán al reconectar
      </div>
      <div class="conn-banner backend-down" *ngIf="connectivity.online() && !connectivity.backendReachable()">
        <span class="spinner"></span> No se pudo conectar con el servidor — reintentando…
      </div>

      <header *ngIf="auth.isAuthenticated() || customerAuth.isAuthenticated()">
        <div class="brand">
          <div class="brand-mark">FS</div>
          <div>
            <div class="brand-name">FieldSync</div>
            <div class="brand-sub">{{ auth.isAuthenticated() ? 'Panel de despacho' : 'Portal de cliente' }}</div>
          </div>
        </div>

        <nav *ngIf="auth.isAuthenticated()">
          <a routerLink="/dispatch" routerLinkActive="active">Mapa</a>
          <a routerLink="/orders" routerLinkActive="active">Órdenes</a>
          <a *ngIf="auth.user()?.role === 'ADMIN'" routerLink="/team" routerLinkActive="active">Equipo</a>
        </nav>
        <nav *ngIf="!auth.isAuthenticated() && customerAuth.isAuthenticated()">
          <a routerLink="/portal" routerLinkActive="active">Mis solicitudes</a>
          <a routerLink="/portal/new" routerLinkActive="active">Nueva solicitud</a>
        </nav>

        <div class="session">
          <div class="avatar">{{ initials() }}</div>
          <div class="who">
            <div class="who-name">{{ sessionName() }}</div>
            <div class="who-role">{{ sessionRole() }}</div>
          </div>
          <button class="logout" (click)="logout()" title="Cerrar sesión" aria-label="Cerrar sesión">
            ⏻
          </button>
        </div>
      </header>

      <main [class.with-header]="auth.isAuthenticated() || customerAuth.isAuthenticated()">
        <router-outlet />
      </main>
    </div>
  `,
  styles: [`
    .shell { min-height: 100vh; display: flex; flex-direction: column; }

    .conn-banner {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      padding: 9px;
      font-size: 13px;
      font-weight: 600;
      position: sticky;
      top: 0;
      z-index: 20;
    }
    .conn-banner.offline {
      background: rgba(220, 38, 38, 0.18);
      color: #fca5a5;
      border-bottom: 1px solid rgba(220, 38, 38, 0.35);
    }
    .conn-banner.offline .dot {
      width: 7px; height: 7px; border-radius: 50%; background: #f87171;
    }
    .conn-banner.backend-down {
      background: rgba(202, 138, 4, 0.18);
      color: #fde68a;
      border-bottom: 1px solid rgba(202, 138, 4, 0.35);
    }

    header {
      display: flex;
      align-items: center;
      gap: 32px;
      padding: 14px 28px;
      background: rgba(17, 26, 46, 0.85);
      backdrop-filter: blur(12px);
      border-bottom: 1px solid var(--fs-border);
      position: sticky;
      top: 0;
      z-index: 10;
    }

    .brand { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
    .brand-mark {
      width: 34px; height: 34px; border-radius: 10px; flex-shrink: 0;
      background: linear-gradient(135deg, var(--fs-primary), var(--fs-primary-dark));
      display: grid; place-items: center;
      color: #fff; font-weight: 700; font-size: 13px;
      font-family: 'Space Grotesk', sans-serif;
    }
    .brand-name { font-weight: 700; font-size: 15px; color: var(--fs-text); font-family: 'Space Grotesk', sans-serif; white-space: nowrap; }
    .brand-sub { font-size: 11px; color: var(--fs-text-faint); white-space: nowrap; }

    nav {
      display: flex; gap: 4px; flex: 1; min-width: 0;
      overflow-x: auto; scrollbar-width: none;
    }
    nav::-webkit-scrollbar { display: none; }
    nav a {
      color: var(--fs-text-muted);
      text-decoration: none;
      padding: 8px 16px;
      border-radius: 100px;
      font-size: 14px;
      font-weight: 500;
      white-space: nowrap;
      transition: background 0.15s, color 0.15s;
    }
    nav a:hover { background: rgba(255, 255, 255, 0.04); color: var(--fs-text); }
    nav a.active { background: var(--fs-primary-light); color: #60a5fa; font-weight: 600; }

    .session { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
    .avatar {
      width: 32px; height: 32px; border-radius: 50%; flex-shrink: 0;
      background: var(--fs-surface-2); border: 1px solid var(--fs-border);
      display: grid; place-items: center;
      font-size: 12px; font-weight: 700; color: var(--fs-text);
    }
    .who { min-width: 0; }
    .who-name { font-size: 13px; font-weight: 600; color: var(--fs-text); line-height: 1.2; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 160px; }
    .who-role { font-size: 11px; color: var(--fs-text-faint); white-space: nowrap; }
    .logout {
      background: transparent;
      border: 1px solid var(--fs-border);
      color: var(--fs-text-muted);
      width: 32px; height: 32px; border-radius: 50%; flex-shrink: 0;
      cursor: pointer; font-size: 14px;
      display: grid; place-items: center;
      transition: all 0.15s;
    }
    .logout:hover { border-color: var(--fs-danger); color: var(--fs-danger); background: rgba(220, 38, 38, 0.08); }

    main { flex: 1; width: 100%; }
    main.with-header { padding: 28px; max-width: 1200px; margin: 0 auto; }

    @media (max-width: 720px) {
      header { gap: 16px; padding: 12px 16px; }
      .brand-sub { display: none; }
    }
    @media (max-width: 560px) {
      header { gap: 10px; }
      nav a { padding: 8px 12px; font-size: 13px; }
      .who { display: none; }
      main.with-header { padding: 16px; }
    }
  `],
})
export class AppComponent {
  readonly auth = inject(AuthService);
  readonly customerAuth = inject(CustomerAuthService);
  readonly connectivity = inject(ConnectivityService);
  private readonly router = inject(Router);

  sessionName(): string {
    return this.auth.isAuthenticated()
      ? (this.auth.user()?.name ?? '')
      : (this.customerAuth.customer()?.name ?? '');
  }

  sessionRole(): string {
    if (this.auth.isAuthenticated()) return this.roleLabel();
    return this.customerAuth.isAuthenticated() ? 'Cliente' : '';
  }

  initials(): string {
    const name = this.sessionName();
    const parts = name.split(' ').filter(Boolean).slice(0, 2);
    return parts.map((p) => p[0]).join('').toUpperCase() || '?';
  }

  roleLabel(): string {
    const role = this.auth.user()?.role;
    return role ? (ROLE_LABELS[role] ?? role) : '';
  }

  logout(): void {
    if (this.auth.isAuthenticated()) {
      this.auth.logout();
    } else {
      this.customerAuth.logout();
    }
    this.router.navigate(['/login']);
  }
}
