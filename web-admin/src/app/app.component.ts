import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

/** Shell del panel: cabecera de navegación + <router-outlet>. */
@Component({
  selector: 'fs-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header>
      <h1>FieldSync · Despacho</h1>
      <nav>
        <a routerLink="/dispatch" routerLinkActive="active">Mapa</a>
        <a routerLink="/orders" routerLinkActive="active">Órdenes</a>
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
    nav a { color: var(--fs-text); margin-left: 20px; text-decoration: none; }
    nav a.active { color: var(--fs-primary); font-weight: 600; }
    main { padding: 24px; }
  `],
})
export class AppComponent {}
