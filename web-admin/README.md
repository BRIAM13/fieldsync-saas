# FieldSync — Panel Web Admin (Angular)

Panel de despacho para el administrador/despachador. Asigna órdenes de trabajo a técnicos sobre un mapa.

## Stack

- **Angular 18** (componentes standalone) + **TypeScript** en modo estricto
- **Rutas** con carga diferida (lazy) por feature
- **RxJS** + **Signals** para estado reactivo
- Servicio de **API simulado** (`WorkOrderService`) — se sustituye por `HttpClient` sin tocar componentes

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
