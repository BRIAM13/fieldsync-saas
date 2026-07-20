# FieldSync — Roadmap de Desarrollo

> **FieldSync** es un SaaS de gestión de tareas para técnicos de campo (plomeros, electricistas, instaladores).
> Este repositorio es la **pieza central de portafolio** de Briam Ronceros para postular a puestos de **Desarrollador Android**.
> Cada componente está elegido para demostrar, con código real y funcional, las competencias que piden las ofertas de empleo.

---

## 1. Visión del producto

Las empresas de servicios de campo pierden tiempo y dinero por tres problemas: despacho manual de órdenes, técnicos sin conectividad estable en sitio, y clientes sin visibilidad del estado de su servicio. **FieldSync** resuelve los tres con un ecosistema de tres aplicaciones sobre un backend común:

| Pieza | Usuario | Rol en el producto |
|-------|---------|--------------------|
| **App Android** (Kotlin) | Técnico en la calle | Recibe, ejecuta y cierra órdenes de trabajo **offline-first**. |
| **Panel Web** (Angular) | Despachador / administrador | Asigna tareas sobre un mapa y monitorea la operación. |
| **App Cliente** (React Native) | Cliente final | Solicita servicio y sigue al técnico **en tiempo real**. |

---

## 2. Arquitectura del monorepo

```
fieldsync-saas/
├── android-app/     → Kotlin · Clean Architecture · MVVM · Compose · Coroutines/Flow · Room
│                      (+ módulo legacy "history" en MVP para demostrar migración)
├── web-admin/       → Angular · TypeScript · Routing · Servicio de API simulado
├── client-app/      → React Native · seguimiento en tiempo real
└── marketing/       → HyperFrames · video promocional (HTML/CSS/GSAP → MP4)
```

---

## 3. Las 3 características clave (y qué tecnología las demuestra)

1. **Asignación inteligente en mapas** → *Panel Angular*
2. **Sincronización offline-first** → *Android · Room + Coroutines + Flow*
3. **Seguimiento de técnicos en tiempo real** → *App Cliente React Native*

---

## 4. Cómo este proyecto destaca cada tecnología que piden los empleadores

Esta es la sección que traduce el proyecto en **argumentos de entrevista**. Cada fila responde a "¿dónde demuestro X?".

### 🟢 Kotlin
- **Dónde:** toda la `android-app`.
- **Qué demuestra:** dominio idiomático del lenguaje — data classes, sealed classes para estados de UI, extension functions, null-safety, scope functions.
- **Punto de venta:** código Kotlin moderno y limpio, no Java traducido.

### 🟢 Coroutines & Flow
- **Dónde:** capa `data` (repositorios), `domain` (use cases con `suspend`), y `presentation` (ViewModels exponen `StateFlow`).
- **Qué demuestra:** asincronía estructurada (`viewModelScope`, `Dispatchers`), streams reactivos de Room a la UI, manejo de cancelación y errores.
- **Punto de venta:** la sincronización offline-first se apoya en `Flow` para reaccionar a cambios de la base de datos local sin polling.

### 🟢 MVVM + Clean Architecture
- **Dónde:** `android-app` dividida en `data / domain / presentation` + `di`.
- **Qué demuestra:** separación de responsabilidades, inversión de dependencias (el `domain` no conoce a Android), testabilidad, ViewModels sin referencias a la vista.
- **Punto de venta:** arquitectura escalable de nivel producción, no un `Activity` con 1000 líneas.

### 🟢 MVP (arquitectura heredada)
- **Dónde:** módulo `history` dentro de `android-app`.
- **Qué demuestra:** que entiendo el patrón **Model-View-Presenter** clásico (contratos `View`/`Presenter`, presenter que sostiene una referencia a la vista) **y** que sé cómo conviven módulos legacy con módulos MVVM modernos.
- **Punto de venta:** capacidad de mantener y **migrar** código heredado — algo que las empresas con apps antiguas valoran muchísimo.

### 🟢 Jetpack Compose
- **Dónde:** `presentation` de la `android-app`.
- **Qué demuestra:** UI declarativa moderna, estado unidireccional, recomposición eficiente, theming.
- **Punto de venta:** stack de UI actual de Google, no XML legacy (excepto donde se muestra a propósito el contraste).

### 🟢 Angular + TypeScript
- **Dónde:** `web-admin`.
- **Qué demuestra:** competencia full-stack — routing, servicios inyectables, tipado fuerte, arquitectura por features. Un panel de despacho real con un servicio de API simulado para órdenes de trabajo.
- **Punto de venta:** un dev Android que también entiende el frontend web del producto = perfil más completo.

### 🟢 React Native
- **Dónde:** `client-app`.
- **Qué demuestra:** desarrollo móvil multiplataforma; comprensión de las diferencias entre nativo (Kotlin) y cross-platform (RN) y **cuándo usar cada uno**.
- **Punto de venta:** versatilidad móvil; capaz de argumentar trade-offs nativo vs. multiplataforma en entrevista.

---

## 5. Fases de desarrollo

### Fase 0 — Fundaciones (actual)
- [x] Estructura del monorepo.
- [x] `ROADMAP.md` con el mapeo tecnología → competencia.
- [ ] Inicializar repositorio Git.

### Fase 1 — Núcleo Android (prioridad de portafolio)
- [x] Esqueleto Clean Architecture + MVVM (`data / domain / presentation / di`).
- [x] Modelo de dominio `WorkOrder` + use cases (observe / update / sync).
- [x] Room como fuente de verdad local (offline-first).
- [x] Repositorio con `Flow`, ViewModels con `StateFlow`, pantalla en Compose.
- [x] Módulo legacy `history` en MVP (contrato View/Presenter).
- [x] Navegación (Navigation Compose): lista → detalle de orden con `SavedStateHandle`.
- [x] Sincronización real en 2.º plano: **WorkManager** (+ Hilt) con restricción de red + `NetworkMonitor`.
- [x] `NetworkMonitor` / `SyncScheduler` como interfaces → ViewModel testeable con fakes.
- [x] Tests: ordenamiento, use case de sync, y ViewModel (StateFlow + Turbine + coroutines-test).

### Fase 2 — Panel de despacho (Angular)
- [ ] Proyecto Angular + routing.
- [ ] `WorkOrderService` (API simulada).
- [ ] Vista de asignación sobre mapa.

### Fase 3 — App Cliente (React Native)
- [ ] Scaffold RN.
- [ ] Pantalla de seguimiento en tiempo real del técnico.

### Fase 4 — Backend real y sincronización
- [x] API REST en **Ktor** (Kotlin) que une las tres apps: órdenes + sync + health.
- [x] Canal en **tiempo real** (WebSocket) para el seguimiento del técnico.
- [x] CORS, logging, errores JSON uniformes, tests de integración (test-host de Ktor).
- [x] Clientes conectados a la API real: Android (Ktor client), Angular (HttpClient), RN (WebSocket).
- [x] Backend extendido: técnicos, asignación y ubicación para el panel de despacho.
- [x] Persistencia **Postgres + Exposed** (Hikari), seleccionable por `DATABASE_URL`; repo en memoria como fallback.
- [x] Listo para desplegar: Dockerfile + docker-compose + guía Neon/Render.
- [x] **Autenticación JWT + multi-tenancy** por empresa (BCrypt, aislamiento por tenant, tests).
- [x] Login cableado en los 3 clientes: Angular (interceptor+guard), Android (TokenStore+Bearer+login), RN (login+WS token).
- [ ] Resolución de conflictos de sincronización (last-write-wins → estrategia por campo).
- [ ] Persistencia del token en cliente (DataStore/EncryptedPrefs) y refresh tokens.

### Fase 5 — Marketing (HyperFrames)
- [x] Video promocional 15–30 s que simula la UI y resalta las 3 características clave.
- [x] Render a MP4 para LinkedIn / portafolio.

### Fase 6 — Pulido de portafolio
- [x] README raíz con overview, mapa del monorepo y tabla tecnología→competencia.
- [x] Diagrama de arquitectura (Clean Architecture + flujo offline-first, Mermaid).
- [x] Tests unitarios (JUnit + Turbine para `Flow`, ViewModels con coroutines-test).
- [x] CI con GitHub Actions (tests Android + build Angular).
- [ ] Capturas / GIFs por app en los README.

---

## 6. Estado actual

**Fases 0–3, 5 y buena parte de la 6 completas.** Las tres apps tienen su esqueleto real
(Android profundizado con navegación, WorkManager y tests), el video promocional está renderizado,
y el repo tiene README con arquitectura + CI. Siguiente gran hito: **Fase 4 (backend real)**.
