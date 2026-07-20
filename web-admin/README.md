# FieldSync — Panel Web Admin (Angular)

Panel de despacho para el administrador/despachador. Asigna órdenes de trabajo a técnicos sobre un mapa.

## Stack

- **Angular 18** (componentes standalone) + **TypeScript** en modo estricto
- **Rutas** con carga diferida (lazy) por feature
- **RxJS** + **Signals** para estado reactivo
- **`HttpClient` conectado al backend real** (Ktor, `http://localhost:8080`) — órdenes, técnicos y asignación

> **Requiere el backend corriendo** (`cd ../backend && gradle run`). El `WorkOrderService`
> consulta `GET /api/work-orders` y `/api/technicians`, y asigna con `PATCH .../assignment`.
> La URL base está en `core/api.config.ts`.

### Autenticación

- **Login** en `/login` (demo: `admin@fieldsync.dev` / `demo1234`) → `AuthService` guarda el JWT.
- Un **HTTP interceptor** (`authInterceptor`) adjunta `Authorization: Bearer <token>` a la API.
- Un **guard** (`authGuard`) protege `/dispatch` y `/orders` y redirige a `/login` sin sesión.

## Estructura

```
src/app/
├── core/
│   ├── models/    work-order.model.ts   (tipos compartidos)
│   └── services/  work-order.service.ts (API simulada, Observables)
├── features/
│   ├── map-dispatch/   asignación inteligente en mapa  ← característica clave #1
│   └── work-orders/    listado de órdenes
├── app.component.ts    shell + navegación
├── app.routes.ts       rutas lazy
└── app.config.ts       providers (router)
```

## Ejecutar

```bash
npm install
npm start        # ng serve → http://localhost:4200
```

> Requiere Node.js y Angular CLI. Los archivos de configuración (`angular.json`,
> `tsconfig.json`, `package.json`) están listos; `npm install` resuelve el resto.

## 📸 Capturas

Graba las capturas reales con el backend corriendo (ver la [guía de captura](../docs/CAPTURES.md))
y colócalas en `docs/media/`. Luego descomenta:

<!-- ![Login](../docs/media/web-login.png) -->
<!-- ![Asignación en mapa](../docs/media/web-dispatch.gif) -->
