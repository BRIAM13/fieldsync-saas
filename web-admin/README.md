# FieldSync — Panel Web Admin (Angular)

Panel de despacho para el administrador/despachador — asigna órdenes de trabajo a técnicos sobre
un mapa — y, en el mismo dominio, **portal completo para el cliente final**: pide servicio con su
ubicación GPS y sigue a su técnico en vivo. Una sola pantalla de acceso dinámica sirve a ambos
públicos.

**En vivo:** [fieldsync-web-admin.vercel.app](https://fieldsync-web-admin.vercel.app) (Vercel) — conectado al backend real en Render + Aiven Postgres.

## Stack

- **Angular 18** (componentes standalone) + **TypeScript** en modo estricto
- **Rutas** con carga diferida (lazy) por feature
- **RxJS** + **Signals** para estado reactivo
- **`HttpClient` conectado al backend real** (Ktor) — órdenes, técnicos, asignación, usuarios y solicitudes de cliente
- **WebSocket nativo** envuelto en un `Observable` — seguimiento de técnico en vivo para el cliente
- **Leaflet + OpenStreetMap** — mapa interactivo real, tanto en el despacho (staff) como en el seguimiento (cliente)
- **Geolocalización del navegador** (`navigator.geolocation`) — el cliente captura su ubicación real al pedir servicio

> La URL base del backend está centralizada en `core/api.config.ts` y por defecto apunta a
> producción (Render + Aiven). Para desarrollo local contra un backend en tu máquina
> (`cd ../backend && gradle run`), cámbiala temporalmente a `http://localhost:8080`.

### Acceso dinámico: dos identidades, una sola pantalla

`/login` y `/register` son el mismo componente (`AuthGatewayComponent`): una tarjeta que se
desliza entre **Iniciar sesión** y **Registrarse**, con un selector **Empresa** ↔ **Cliente** que
cambia los campos del formulario y a qué API/JWT apunta cada envío. Empresa y Cliente son
**identidades completamente independientes** — servicios, interceptores, guards y claves de
`localStorage` distintos (`AuthService`/`authInterceptor`/`authGuard` vs.
`CustomerAuthService`/`customerAuthInterceptor`/`customerGuard`) — que coexisten en la misma app
sin pisarse, reflejando los dos proveedores JWT separados del backend
(`AUTH_JWT` / `AUTH_JWT_CUSTOMER`).

- **Login empresa** en `/login` (demo: `admin@fieldsync.dev` / `demo1234`).
- **Login cliente** en la misma pantalla, pestaña "Cliente" (demo: `cliente@fieldsync.dev` / `demo1234`).
- `AuthService` / `CustomerAuthService` guardan access + refresh token en `localStorage` (la
  sesión sobrevive a recargas). Cada **HTTP interceptor** adjunta el Bearer correspondiente y,
  ante un **401**, renueva con `/auth/refresh` (o `/customer/auth/refresh`) y reintenta una vez.
- **`guestGuard`** protege `/login` y `/register`: si ya hay sesión activa (de cualquiera de las
  dos identidades), redirige directo a `/dispatch` o `/portal` en vez de mostrar el login —
  evita el caso de ver el login y la cabecera autenticada a la vez.
- **`authGuard`** protege `/dispatch`, `/orders` y `/team`; **`customerGuard`** protege
  `/portal*`. **RBAC en la UI**: el botón "Asignar" y la lista de técnicos solo aparecen para
  ADMIN/DISPATCHER. **Gestión de usuarios** (`/team`, solo ADMIN) va detrás de `adminGuard`
  además del RBAC del backend.
- **Antifraude de registro** (backend, código propio — sin CAPTCHA de terceros): campo honeypot
  invisible, rate limiting por IP con ventana deslizante, y bloqueo de dominios de correo
  desechables. Ambos formularios de registro (empresa y cliente) pasan por el mismo guard.

### Portal de cliente

Tras iniciar sesión como cliente, el panel se convierte en un portal de seguimiento de
servicios — el equivalente web de la app móvil (`client-app`, React Native):

- **`/portal`** — lista de solicitudes propias, con estado y prioridad.
- **`/portal/new`** — nueva solicitud: título, dirección, prioridad y **ubicación GPS real**
  capturada con `navigator.geolocation`; el envío queda deshabilitado hasta capturarla.
- **`/portal/tracking/:id`** — mapa Leaflet con la posición del técnico actualizándose en vivo
  vía WebSocket (`/ws/tracking/{orderId}`), la misma conexión que usa la app React Native.

## Estructura

```
src/app/
├── core/
│   ├── guards/     authGuard, adminGuard, customerGuard, guestGuard
│   ├── models/     auth.model.ts, customer.model.ts, work-order.model.ts (tipos compartidos)
│   └── services/   auth.service.ts, customer-auth.service.ts, customer-portal.service.ts,
│                    customer-tracking.service.ts, user-management.service.ts, work-order.service.ts
├── features/
│   ├── map-dispatch/   mapa Leaflet + asignación inteligente  ← característica clave #1
│   ├── work-orders/    listado de órdenes (staff)
│   ├── users/           gestión de usuarios del tenant (solo ADMIN)
│   ├── auth/             AuthGatewayComponent — login/registro dinámico, dos identidades
│   └── portal/           portal de cliente: mis solicitudes, nueva solicitud (GPS), seguimiento en vivo
├── app.component.ts    shell + navegación (se adapta a la sesión activa)
├── app.routes.ts       rutas lazy
└── app.config.ts       providers (router, interceptores)
```

## Ejecutar

```bash
npm install
npm start        # ng serve → http://localhost:4200
```

> Requiere Node.js y Angular CLI. Los archivos de configuración (`angular.json`,
> `tsconfig.json`, `package.json`) están listos; `npm install` resuelve el resto.

## 📸 Capturas

Capturas reales contra el backend en producción (Render + Aiven):

![Login](../docs/media/web-login.png)
![Asignación en mapa](../docs/media/web-dispatch.gif)
