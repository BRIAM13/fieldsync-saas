# FieldSync — App Android (Técnicos)

App nativa en **Kotlin** para el técnico de campo. Recibe y cierra órdenes de trabajo con enfoque **offline-first**.

## Stack

- **Kotlin** + **Coroutines / Flow** (asincronía estructurada y reactividad)
- **Clean Architecture** (`domain` / `data` / `presentation`) + **MVVM**
- **Jetpack Compose** + **Navigation Compose** (UI declarativa, multi-pantalla)
- **Room** como fuente de verdad local (offline-first) — expone `Flow`
- **WorkManager** (+ Hilt) para sincronización real en segundo plano con restricción de red
- **Hilt** para inyección de dependencias
- **JUnit + Turbine + coroutines-test** para tests de `Flow`, use cases y ViewModels

## Capas (Clean Architecture)

```
presentation/   Compose + ViewModel (StateFlow, viewModelScope). No conoce data.
     │  depende de ▼
domain/         Modelos puros, use cases, interfaz WorkOrderRepository. Sin Android.
     ▲  implementa │
data/           Room (WorkOrderDao emite Flow), API simulada, RepositoryImpl, mappers.
di/             Hilt: enlaza la interfaz del dominio con la implementación de datos.
```

La regla de oro: **las dependencias apuntan hacia adentro**. `domain` no importa nada de Android, Room ni Compose; por eso su lógica se testea sin instrumentación (ver `ObserveWorkOrdersUseCaseTest`).

## Flujo offline-first

1. La UI **siempre** lee desde Room vía `Flow` → responde al instante, con o sin red.
2. Un cambio de estado se escribe **local primero** y se marca `pendingSync`.
3. `SyncScheduler` encola un **WorkManager** worker con restricción `NetworkType.CONNECTED`:
   se ejecuta solo cuando hay red y se aplaza (con backoff exponencial) cuando no la hay.
4. `NetworkMonitor` (callbackFlow sobre `ConnectivityManager`) avisa al `TasksViewModel`
   cuando vuelve la conexión, que entonces solicita la sincronización automáticamente.
5. `SyncWorkOrdersWorker` (`@HiltWorker`, `CoroutineWorker`) ejecuta `syncPendingChanges()`
   en segundo plano y devuelve `retry()` ante fallo para que WorkManager reintente.

## Navegación y autenticación

`Single-Activity` + **Navigation Compose**: **login** (`LoginScreen`) → lista de órdenes
(`TasksScreen`) → detalle (`TaskDetailScreen`). El `TaskDetailViewModel` lee el `orderId` del
`SavedStateHandle` y observa esa orden reactivamente.

El login (demo: `admin@fieldsync.dev` / `demo1234`) guarda el JWT en `TokenStore`, y el
`HttpClient` de Ktor adjunta `Authorization: Bearer <token>` en cada petición (evaluado por
request, token dinámico). `AuthRepository` mantiene el dominio ajeno a Ktor.

## Testabilidad (por qué la arquitectura importa)

`NetworkMonitor` y `SyncScheduler` son **interfaces**: el `TasksViewModel` depende de
abstracciones, no de clases Android concretas. En los tests se sustituyen por fakes y se
verifica, sin emulador, que: las órdenes se ordenan por prioridad, el contador de pendientes
es correcto, y **recuperar la conexión dispara una sincronización**.

## Módulo legacy `history/` — MVP (a propósito)

Pantalla de historial implementada con **Model-View-Presenter clásico** (contrato
`View`/`Presenter`, presenter con referencia a la vista, `attach`/`detach` manual).
Sirve para demostrar dos cosas ante un empleador:

1. Que entiendo arquitecturas heredadas (muchas apps en producción aún son MVP).
2. Que sé **contrastarlas y migrarlas** a MVVM (estado observable vs. comandos imperativos).

## Estructura

```
app/src/main/java/com/corporacionronceros/fieldsync/
├── domain/       model · repository (interfaz) · usecase (observe/update/sync)
├── data/         local/room · remote · repository (impl + mappers)
│                 connectivity/ (NetworkMonitor + impl) · sync/ (Worker · Scheduler + impl)
├── presentation/ MainActivity · navigation/ (NavHost · Screen)
│                 tasks/ (lista) · detail/ (detalle de orden)
├── di/           AppModule · RepositoryModule (Hilt binds)
└── history/      contract · model · presenter · view   ← MVP legacy
```
