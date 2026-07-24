import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet, Router } from '@angular/router';
import { NgIf } from '@angular/common';
import { AuthService } from './core/services/auth.service';

const ROLE_LABELS: Record<string, string> = {
  ADMIN: 'Administrador',
  DISPATCHER: 'Despachador',
  TECHNICIAN: 'Técnico',
};

/** Shell del panel: cabecera con marca, navegación y sesión activa, y <router-outlet>. */
@Component({
  selector: 'fs-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NgIf],
  template: `
    <div class="shell">
      <header *ngIf="auth.isAuthenticated()">
        <div class="brand">
          <div class="brand-mark">FS</div>
          <div>
            <div class="brand-name">FieldSync</div>
            <div class="brand-sub">Panel de despacho</div>
          </div>
        </div>

        <nav>
          <a routerLink="/dispatch" routerLinkActive="active">Mapa</a>
          <a routerLink="/orders" routerLinkActive="active">Órdenes</a>
          <a *ngIf="auth.user()?.role === 'ADMIN'" routerLink="/team" routerLinkActive="active">Equipo</a>
        </nav>

        <div class="session">
          <div class="avatar">{{ initials() }}</div>
          <div class="who">
            <div class="who-name">{{ auth.user()?.name }}</div>
            <div class="who-role">{{ roleLabel() }}</div>
          </div>
          <button class="logout" (click)="logout()" title="Cerrar sesión" aria-label="Cerrar sesión">
            ⏻
          </button>
        </div>
      </header>

      <main [class.with-header]="auth.isAuthenticated()">
        <router-outlet />
      </main>
    </div>
  `,
  styles: [`
    .shell { min-height: 100vh; display: flex; flex-direction: column; }

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

    .brand { display: flex; align-items: center; gap: 10px; }
    .brand-mark {
      width: 34px; height: 34px; border-radius: 10px;
      background: linear-gradient(135deg, var(--fs-primary), var(--fs-primary-dark));
      display: grid; place-items: center;
      color: #fff; font-weight: 700; font-size: 13px;
      font-family: 'Space Grotesk', sans-serif;
    }
    .brand-name { font-weight: 700; font-size: 15px; color: var(--fs-text); font-family: 'Space Grotesk', sans-serif; }
    .brand-sub { font-size: 11px; color: var(--fs-text-faint); }

    nav { display: flex; gap: 4px; flex: 1; }
    nav a {
      color: var(--fs-text-muted);
      text-decoration: none;
      padding: 8px 16px;
      border-radius: 100px;
      font-size: 14px;
      font-weight: 500;
      transition: background 0.15s, color 0.15s;
    }
    nav a:hover { background: rgba(255, 255, 255, 0.04); color: var(--fs-text); }
    nav a.active { background: var(--fs-primary-light); color: #60a5fa; font-weight: 600; }

    .session { display: flex; align-items: center; gap: 10px; }
    .avatar {
      width: 32px; height: 32px; border-radius: 50%;
      background: var(--fs-surface-2); border: 1px solid var(--fs-border);
      display: grid; place-items: center;
      font-size: 12px; font-weight: 700; color: var(--fs-text);
    }
    .who-name { font-size: 13px; font-weight: 600; color: var(--fs-text); line-height: 1.2; }
    .who-role { font-size: 11px; color: var(--fs-text-faint); }
    .logout {
      background: transparent;
      border: 1px solid var(--fs-border);
      color: var(--fs-text-muted);
      width: 32px; height: 32px; border-radius: 50%;
      cursor: pointer; font-size: 14px;
      display: grid; place-items: center;
      transition: all 0.15s;
    }
    .logout:hover { border-color: var(--fs-danger); color: var(--fs-danger); background: rgba(220, 38, 38, 0.08); }

    main { flex: 1; padding: 28px; max-width: 1200px; width: 100%; margin: 0 auto; }
  `],
})
export class AppComponent {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  initials(): string {
    const name = this.auth.user()?.name ?? '';
    const parts = name.split(' ').filter(Boolean).slice(0, 2);
    return parts.map((p) => p[0]).join('').toUpperCase() || '?';
  }

  roleLabel(): string {
    const role = this.auth.user()?.role;
    return role ? (ROLE_LABELS[role] ?? role) : '';
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
