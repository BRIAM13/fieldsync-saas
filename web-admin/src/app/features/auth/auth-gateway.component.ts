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
 *
 * El tamaño de la tarjeta está atado a `100dvh` (nunca a un alto fijo adivinado), así que es
 * físicamente imposible que dispare scroll de página: siempre cabe en la ventana real del
 * usuario. El deslizamiento usa `transform` (no `left`) para animación por GPU.
 */
@Component({
  selector: 'fs-auth-gateway',
  standalone: true,
  imports: [FormsModule, NgIf, NgFor, RouterLink],
  template: `
    <div class="wrap">
      <div class="stage">
        <div class="accent-panel" [class.on-left]="mode() === 'register'">
          <div class="accent-decor" aria-hidden="true"></div>

          <!-- Escena ilustrada: mapa + tarjetas flotantes con datos en vivo. Puramente
               decorativa (aria-hidden), en animación continua — no requiere interacción. -->
          <div class="map-scene" aria-hidden="true">
            <svg class="map-lines" viewBox="0 0 400 600" preserveAspectRatio="none">
              <path d="M10,470 L100,420 L170,460 L250,380 L340,415 L390,370" />
              <path d="M30,110 L115,150 L85,225 L165,255 L230,210" />
            </svg>
            <span class="map-pulse" style="top: 36%; left: 60%;"></span>

            <div class="float-card card-1">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 21s7-6.2 7-11.5A7 7 0 0 0 5 9.5C5 14.8 12 21 12 21Z" />
                <circle cx="12" cy="9.5" r="2.3" />
              </svg>
              <div><strong>Orden #482</strong><span>En camino</span></div>
            </div>
            <div class="float-card card-2">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="8.5" />
                <path d="M12 7.5V12l3 2" />
              </svg>
              <div><strong>ETA</strong><span>8 min</span></div>
            </div>
            <div class="float-card card-3">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M4 12.5 9.5 18 20 6.5" />
              </svg>
              <div><strong>{{ techCount() }} técnicos</strong><span>activos ahora</span></div>
            </div>
          </div>

          <div class="accent-content">
            <div class="accent-block" *ngIf="mode() === 'login'; else toLogin">
              <h2>¿Aún no tienes cuenta?</h2>
              <p>Regístrate para que puedas iniciar sesión</p>
              <button type="button" class="ghost-btn" (click)="setMode('register')">Registrarse</button>
            </div>
            <ng-template #toLogin>
              <div class="accent-block">
                <h2>¿Ya tienes una cuenta?</h2>
                <p>Inicia sesión para entrar al panel</p>
                <button type="button" class="ghost-btn" (click)="setMode('login')">Iniciar sesión</button>
              </div>
            </ng-template>
          </div>
        </div>

        <div class="form-card" [class.on-right]="mode() === 'register'">
          <div class="brand-row">
            <div class="brand-mark">FS</div>
            <span class="brand-name">FieldSync</span>
          </div>

          <div class="audience-toggle">
            <div class="toggle-indicator" [class.right]="audience() === 'cliente'"></div>
            <button type="button" [class.active]="audience() === 'empresa'" (click)="setAudience('empresa')">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <rect x="4" y="3" width="16" height="18" rx="1.2" />
                <path d="M9 21v-4h6v4M9 7h1M14 7h1M9 11h1M14 11h1M9 15h1M14 15h1" />
              </svg>
              Empresa
            </button>
            <button type="button" [class.active]="audience() === 'cliente'" (click)="setAudience('cliente')">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="8" r="3.4" />
                <path d="M5.5 20c0-3.6 3-6.2 6.5-6.2s6.5 2.6 6.5 6.2" />
              </svg>
              Cliente
            </button>
          </div>

          <h1>{{ mode() === 'login' ? 'Iniciar sesión' : 'Crear cuenta' }}</h1>
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
            <div class="field-group" *ngIf="audience() === 'empresa' && mode() === 'register'">
              <label>Nombre de la empresa</label>
              <input name="companyName" [(ngModel)]="companyName" required />
            </div>

            <!-- Registro de cliente: elegir empresa -->
            <div class="field-group" *ngIf="audience() === 'cliente' && mode() === 'register'">
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
            </div>

            <div class="field-group" *ngIf="mode() === 'register'">
              <label>Tu nombre</label>
              <input name="name" [(ngModel)]="name" required />
            </div>

            <div class="field-group">
              <label>Correo electrónico</label>
              <input type="email" name="email" [(ngModel)]="email" autocomplete="username" required />
            </div>
            <p class="tip field-group" *ngIf="audience() === 'empresa' && mode() === 'register' && isGenericEmail()">
              Tip: usa el correo de tu empresa si tienes uno — mejora la confianza de tus clientes.
            </p>

            <div class="field-group" *ngIf="audience() === 'cliente' && mode() === 'register'">
              <label>Teléfono (opcional)</label>
              <input name="phone" [(ngModel)]="phone" />
            </div>

            <div class="field-group">
              <label>Contraseña</label>
              <input
                type="password"
                name="password"
                [(ngModel)]="password"
                [autocomplete]="mode() === 'login' ? 'current-password' : 'new-password'"
                minlength="6"
                required
              />
            </div>

            <button type="submit" class="submit-btn" [disabled]="loading()">
              <span class="spinner" *ngIf="loading()"></span>
              {{ submitLabel() }}
            </button>

            <p class="err field-group" *ngIf="error()">⚠ {{ error() }}</p>

            <p class="demo-hint field-group" *ngIf="mode() === 'login'">
              Cuenta de demostración:
              <strong>{{ audience() === 'empresa' ? 'admin@fieldsync.dev' : 'cliente@fieldsync.dev' }}</strong> / demo1234
            </p>
          </form>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .wrap {
      height: 100vh;
      height: 100dvh;
      display: grid;
      place-items: center;
      padding: 16px;
      overflow: hidden;
    }

    .stage {
      position: relative;
      width: min(880px, calc(100vw - 32px));
      height: min(660px, calc(100dvh - 32px));
      border-radius: 20px;
      overflow: hidden;
      box-shadow:
        0 24px 64px -16px rgba(0, 0, 0, 0.55),
        0 0 0 1px rgba(255, 255, 255, 0.04),
        0 0 90px -24px rgba(37, 99, 235, 0.35);
      background: var(--fs-bg);
      animation:
        stageIn 0.6s cubic-bezier(0.16, 1, 0.3, 1),
        stageGlow 6s ease-in-out 0.6s infinite;
    }
    @keyframes stageIn {
      from { opacity: 0; transform: translateY(14px) scale(0.98); }
      to { opacity: 1; transform: none; }
    }
    /* Resplandor que respira suavemente alrededor de toda la tarjeta. */
    @keyframes stageGlow {
      0%, 100% {
        box-shadow:
          0 24px 64px -16px rgba(0, 0, 0, 0.55),
          0 0 0 1px rgba(255, 255, 255, 0.04),
          0 0 90px -24px rgba(37, 99, 235, 0.35);
      }
      50% {
        box-shadow:
          0 24px 64px -16px rgba(0, 0, 0, 0.55),
          0 0 0 1px rgba(255, 255, 255, 0.06),
          0 0 120px -20px rgba(37, 99, 235, 0.5);
      }
    }

    .accent-panel {
      position: absolute;
      top: 0;
      left: 45%;
      width: 55%;
      height: 100%;
      z-index: 1;
      background: linear-gradient(150deg, var(--fs-primary), var(--fs-primary-dark) 70%);
      background-size: 160% 160%;
      animation: gradientDrift 12s ease-in-out infinite alternate;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 32px;
      transform: translateX(0);
      transition: transform 0.7s cubic-bezier(0.65, 0, 0.35, 1);
      will-change: transform;
    }
    .accent-panel.on-left { transform: translateX(-81.8182%); }

    /* Barrido de brillo diagonal, siempre en marcha — refuerzo de "vidrio" premium. */
    .accent-panel::after {
      content: '';
      position: absolute;
      inset: 0;
      z-index: 3;
      pointer-events: none;
      background: linear-gradient(115deg, transparent 42%, rgba(255, 255, 255, 0.12) 50%, transparent 58%);
      background-size: 250% 250%;
      animation: sheenSweep 7s ease-in-out infinite;
    }
    @keyframes sheenSweep {
      0% { background-position: 220% 0%; }
      55%, 100% { background-position: -60% 0%; }
    }

    .accent-decor { position: absolute; inset: 0; overflow: hidden; pointer-events: none; }
    .accent-decor::before,
    .accent-decor::after {
      content: '';
      position: absolute;
      border-radius: 50%;
      filter: blur(40px);
    }
    .accent-decor::before {
      width: 190px; height: 190px; top: -70px; right: -50px;
      background: rgba(255, 255, 255, 0.14);
      animation: driftA 9s ease-in-out infinite alternate;
    }
    .accent-decor::after {
      width: 150px; height: 150px; bottom: -55px; left: -35px;
      background: rgba(255, 255, 255, 0.08);
      animation: driftB 11s ease-in-out infinite alternate;
    }
    @keyframes driftA { from { transform: translate(0, 0); } to { transform: translate(-16px, 18px); } }
    @keyframes driftB { from { transform: translate(0, 0); } to { transform: translate(14px, -12px); } }

    @keyframes gradientDrift {
      from { background-position: 0% 0%; }
      to { background-position: 100% 100%; }
    }

    /* Escena de mapa: rutas dibujándose, punto de ubicación pulsando, tarjetas de UI
       flotando con un ligero giro 3D — todo en bucle automático, sin necesidad de hover. */
    .map-scene { position: absolute; inset: 0; pointer-events: none; z-index: 0; perspective: 900px; }
    .map-lines { position: absolute; inset: 0; width: 100%; height: 100%; }
    .map-lines path {
      fill: none;
      stroke: rgba(255, 255, 255, 0.22);
      stroke-width: 2;
      stroke-linecap: round;
      stroke-linejoin: round;
      stroke-dasharray: 6 10;
      animation: dashFlow 6s linear infinite;
    }
    .map-lines path:nth-child(2) {
      stroke: rgba(255, 255, 255, 0.13);
      animation-duration: 8s;
      animation-direction: reverse;
    }
    @keyframes dashFlow { to { stroke-dashoffset: -160; } }

    .map-pulse { position: absolute; width: 9px; height: 9px; border-radius: 50%; background: #fff; }
    .map-pulse::before {
      content: '';
      position: absolute;
      inset: -2px;
      border-radius: 50%;
      border: 1.5px solid rgba(255, 255, 255, 0.85);
      animation: pulseRing 2.4s cubic-bezier(0, 0, 0.2, 1) infinite;
    }
    @keyframes pulseRing {
      from { transform: scale(1); opacity: 0.9; }
      to { transform: scale(3.6); opacity: 0; }
    }

    .float-card {
      position: absolute;
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 9px 12px;
      border-radius: 12px;
      background: rgba(255, 255, 255, 0.14);
      backdrop-filter: blur(8px);
      border: 1px solid rgba(255, 255, 255, 0.28);
      box-shadow: 0 14px 30px -10px rgba(6, 12, 30, 0.6), inset 0 1px 0 rgba(255, 255, 255, 0.35);
      color: #fff;
      animation: floatCard 6.5s ease-in-out infinite;
      transform-style: preserve-3d;
      pointer-events: auto;
      transition: border-color 0.25s, box-shadow 0.25s;
    }
    .float-card:hover {
      border-color: rgba(255, 255, 255, 0.55);
      box-shadow: 0 16px 34px -8px rgba(6, 12, 30, 0.7), inset 0 1px 0 rgba(255, 255, 255, 0.5);
    }
    .float-card svg { width: 15px; height: 15px; flex-shrink: 0; opacity: 0.9; }
    .float-card strong { display: block; font-size: 11.5px; font-weight: 700; line-height: 1.3; white-space: nowrap; }
    .float-card span { display: block; font-size: 10px; color: rgba(255, 255, 255, 0.75); white-space: nowrap; }

    /* Los dos paneles se superponen ~18% en cada borde interior durante el deslizamiento
       (para no dejar hueco), y ese lado alterna entre izquierda/derecha según el modo. Estos
       insets se mantienen fuera de esa franja en ambos modos. */
    .card-1 { top: 12%; left: 24%; animation-delay: 0s; }
    .card-2 { bottom: 22%; right: 24%; animation-delay: 1.4s; animation-duration: 7.2s; }
    .card-3 { bottom: 8%; left: 26%; animation-delay: 0.7s; animation-duration: 6.8s; }

    @keyframes floatCard {
      0%, 100% { transform: perspective(700px) translateY(0) rotateY(0deg) rotateX(0deg); }
      50% { transform: perspective(700px) translateY(-9px) rotateY(4deg) rotateX(-3deg); }
    }

    .accent-content { position: relative; z-index: 2; color: #fff; max-width: 300px; }
    .accent-block { animation: fieldIn 0.4s cubic-bezier(0.16, 1, 0.3, 1); }
    .accent-content h2 { margin: 0 0 8px; font-size: 22px; font-weight: 700; font-family: 'Space Grotesk', sans-serif; }
    .accent-content p { margin: 0 0 18px; color: rgba(255, 255, 255, 0.85); font-size: 13.5px; line-height: 1.5; }
    .ghost-btn {
      background: transparent;
      border: 1.5px solid rgba(255, 255, 255, 0.85);
      color: #fff;
      padding: 10px 22px;
      border-radius: 10px;
      font-weight: 600;
      font-size: 13.5px;
      cursor: pointer;
      transition: background 0.2s, transform 0.2s, border-color 0.2s;
    }
    .ghost-btn:hover { background: rgba(255, 255, 255, 0.14); border-color: #fff; transform: translateY(-1px); }
    .ghost-btn:active { transform: translateY(0); }

    .form-card {
      position: absolute;
      top: 0;
      left: 0;
      width: 55%;
      height: 100%;
      z-index: 2;
      background: var(--fs-surface);
      padding: 28px 36px;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
      justify-content: center;
      transform: translateX(0);
      transition: transform 0.7s cubic-bezier(0.65, 0, 0.35, 1);
      will-change: transform;
    }
    .form-card.on-right { transform: translateX(81.8182%); }

    /* Mismo barrido de brillo que el panel de acento, pero muy tenue para no
       interferir con la lectura del formulario. */
    .form-card::after {
      content: '';
      position: absolute;
      inset: 0;
      z-index: 3;
      pointer-events: none;
      background: linear-gradient(115deg, transparent 44%, rgba(255, 255, 255, 0.05) 50%, transparent 56%);
      background-size: 250% 250%;
      animation: sheenSweep 7s ease-in-out 3.2s infinite;
    }

    .brand-row { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; }
    .brand-mark {
      width: 24px; height: 24px; border-radius: 7px; flex-shrink: 0;
      background: linear-gradient(135deg, var(--fs-primary), var(--fs-primary-dark));
      display: grid; place-items: center;
      color: #fff; font-weight: 700; font-size: 10px;
      font-family: 'Space Grotesk', sans-serif;
    }
    .brand-name { font-size: 12.5px; font-weight: 600; color: var(--fs-text-muted); letter-spacing: 0.01em; }

    .audience-toggle {
      position: relative;
      display: flex;
      background: var(--fs-bg);
      border: 1px solid var(--fs-border);
      border-radius: 100px;
      padding: 4px;
      margin-bottom: 16px;
      flex-shrink: 0;
    }
    .audience-toggle button {
      position: relative;
      z-index: 1;
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      background: transparent;
      border: 0;
      padding: 9px 0;
      border-radius: 100px;
      font-size: 13px;
      font-weight: 600;
      color: var(--fs-text-muted);
      cursor: pointer;
      transition: color 0.25s, transform 0.15s;
    }
    .audience-toggle button svg { width: 14px; height: 14px; flex-shrink: 0; }
    .audience-toggle button.active { color: #fff; }
    .audience-toggle button:active { transform: scale(0.96); }
    .toggle-indicator {
      position: absolute;
      top: 4px;
      left: 4px;
      width: calc(50% - 4px);
      height: calc(100% - 8px);
      background: var(--fs-primary);
      border-radius: 100px;
      box-shadow: 0 0 14px rgba(37, 99, 235, 0.65);
      transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
    }
    .toggle-indicator.right { transform: translateX(100%); }

    h1 { margin: 0; font-size: 22px; font-weight: 700; font-family: 'Space Grotesk', sans-serif; }
    .sub { margin: 4px 0 16px; color: var(--fs-text-faint); font-size: 12.5px; }

    form { display: flex; flex-direction: column; }

    @keyframes fieldIn {
      from { opacity: 0; transform: translateY(7px); }
      to { opacity: 1; transform: translateY(0); }
    }
    .field-group { animation: fieldIn 0.35s cubic-bezier(0.16, 1, 0.3, 1); margin-top: 10px; }
    .field-group:first-child { margin-top: 0; }

    label { display: block; font-size: 11.5px; color: var(--fs-text-muted); margin-bottom: 5px; font-weight: 500; }
    input {
      width: 100%;
      padding: 9px 12px;
      border-radius: 9px;
      border: 1px solid var(--fs-border);
      background: var(--fs-bg);
      color: var(--fs-text);
      font-size: 13.5px;
      transition: border-color 0.2s, box-shadow 0.2s;
    }
    input:focus {
      outline: none;
      border-color: var(--fs-primary);
      box-shadow: 0 0 0 3px var(--fs-primary-light);
    }

    .honeypot { position: absolute; left: -9999px; width: 0; height: 0; opacity: 0; pointer-events: none; }

    .tip { font-size: 10.5px; color: #facc15; line-height: 1.4; margin: 6px 0 0 !important; }

    .company-list { display: flex; flex-wrap: wrap; gap: 6px; }
    .company-chip {
      padding: 6px 12px;
      border-radius: 100px;
      background: var(--fs-bg);
      border: 1px solid var(--fs-border);
      color: var(--fs-text-muted);
      font-size: 11.5px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
    }
    .company-chip:hover { border-color: var(--fs-primary); color: var(--fs-text); }
    .company-chip:active { transform: scale(0.96); }
    .company-chip.selected {
      background: var(--fs-primary-light);
      border-color: var(--fs-primary);
      color: #60a5fa;
    }
    .hint { color: var(--fs-text-faint); font-size: 11.5px; }

    .submit-btn {
      position: relative;
      overflow: hidden;
      margin-top: 18px;
      padding: 11px;
      border: 0;
      border-radius: 9px;
      background: var(--fs-primary);
      color: #fff;
      cursor: pointer;
      font-weight: 700;
      font-size: 13.5px;
      transition: background 0.2s, transform 0.15s, box-shadow 0.2s;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }
    .submit-btn::after {
      content: '';
      position: absolute;
      top: 0;
      left: -60%;
      width: 40%;
      height: 100%;
      background: linear-gradient(120deg, transparent, rgba(255, 255, 255, 0.4), transparent);
      transform: skewX(-20deg);
      transition: left 0.55s ease;
    }
    .submit-btn:hover:not(:disabled) {
      background: var(--fs-primary-dark);
      transform: translateY(-1px);
      box-shadow: 0 8px 20px -6px rgba(37, 99, 235, 0.55);
    }
    .submit-btn:hover:not(:disabled)::after { left: 130%; }
    .submit-btn:active:not(:disabled) { transform: translateY(0); }
    .submit-btn:disabled { opacity: 0.6; cursor: default; }

    .err { color: #f87171; font-size: 12.5px; margin: 10px 0 0 !important; }

    .demo-hint {
      margin: 16px 0 0 !important;
      padding-top: 14px;
      border-top: 1px solid var(--fs-border);
      text-align: center;
      font-size: 11.5px;
      color: var(--fs-text-faint);
      line-height: 1.6;
    }
    .demo-hint strong { color: var(--fs-text-muted); font-weight: 600; }

    @media (max-width: 760px) {
      .wrap { height: auto; min-height: 100vh; min-height: 100dvh; padding: 0; overflow-y: auto; }
      .stage { width: 100%; height: auto; max-height: none; border-radius: 0; box-shadow: none; }
      .accent-panel, .form-card { position: relative; width: 100%; transform: none !important; transition: none; animation: none; }
      .accent-panel { order: 2; padding: 28px 24px; }
      .accent-panel::after, .form-card::after { display: none; }
      .map-scene { display: none; }
      .form-card { order: 1; padding: 28px 24px; overflow-y: visible; justify-content: flex-start; }
      .stage { display: flex; flex-direction: column; }
    }

    @media (prefers-reduced-motion: reduce) {
      .stage,
      .accent-panel,
      .accent-decor::before,
      .accent-decor::after,
      .accent-panel::after,
      .form-card::after,
      .map-lines path,
      .map-pulse::before,
      .float-card,
      .field-group,
      .accent-block {
        animation: none !important;
      }
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
  /** Conteo decorativo de la tarjeta "técnicos activos": sube de 0 a 12 al montar la vista. */
  readonly techCount = signal(0);

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
    this.animateTechCount();
  }

  private animateTechCount(): void {
    const target = 12;
    if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) {
      this.techCount.set(target);
      return;
    }
    const tick = (n: number) => {
      this.techCount.set(n);
      if (n < target) setTimeout(() => tick(n + 1), 55);
    };
    setTimeout(() => tick(1), 450);
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
