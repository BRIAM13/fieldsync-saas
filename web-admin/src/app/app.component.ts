import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet, Router } from '@angular/router';
import { NgIf } from '@angular/common';
import { AuthService } from './core/services/auth.service';

/** Shell del panel: cabecera con navegación + sesión, y <router-outlet>. */
@Component({
  selector: 'fs-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NgIf],
  template: `
    <header>
      <h1>FieldSync · Despacho</h1>
      <nav *ngIf="auth.isAuthenticated()">
        <a routerLink="/dispatch" routerLinkActive="active">Mapa</a>
        <a routerLink="/orders" routerLinkActive="active">Órdenes</a>
        <span class="who">{{ auth.user()?.name }} · {{ auth.user()?.role }}</span>
        <button (click)="logout()">Salir</button>
      </nav>
    </header>
    <main>
      <router-outlet />
    </main>
  `,
  styles: [`
    header { display: flex; align-items: center; justify-content: space-between;
             padding: 16px 24px; background: var(--fs-surface); }
    h1 { font-size: 18px; margin: 0; color: var(--fs-primary); }
    nav { display: flex; align-items: center; gap: 20px; }
    nav a { color: var(--fs-text); text-decoration: none; }
    nav a.active { color: var(--fs-primary); font-weight: 600; }
    .who { color: #94a3b8; font-size: 13px; }
    button { background: transparent; border: 1px solid #334155; color: var(--fs-text);
             border-radius: 6px; padding: 5px 12px; cursor: pointer; }
    main { padding: 24px; }
  `],
})
export class AppComponent {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
