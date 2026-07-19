# FieldSync — App Android (Técnicos)

App nativa en **Kotlin** para el técnico de campo. Recibe y cierra órdenes de trabajo con enfoque **offline-first**.

## Stack

- **Kotlin** + **Coroutines / Flow** (asincronía estructurada y reactividad)
- **Clean Architecture** (`domain` / `data` / `presentation`) + **MVVM**
- **Jetpack Compose** (UI declarativa)
- **Room** como fuente de verdad local (offline-first) — expone `Flow`
- **Hilt** para inyección de dependencias
- **JUnit + Turbine** para tests de `Flow`

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
3. `syncPendingChanges()` empuja al backend lo pendiente y limpia la marca.

## Módulo legacy `history/` — MVP (a propósito)

Pantalla de historial implementada con **Model-View-Presenter clásico** (contrato
`View`/`Presenter`, presenter con referencia a la vista, `attach`/`detach` manual).
Sirve para demostrar dos cosas ante un empleador:

1. Que entiendo arquitecturas heredadas (muchas apps en producción aún son MVP).
2. Que sé **contrastarlas y migrarlas** a MVVM (estado observable vs. comandos imperativos).

## Estructura

```
app/src/main/java/com/corporacionronceros/fieldsync/
├── domain/       model · repository (interfaz) · usecase
├── data/         local/room · remote · repository (impl + mappers)
├── presentation/ MainActivity · tasks/ (UiState · ViewModel · Screen)
├── di/           AppModule · RepositoryModule (Hilt)
└── history/      contract · model · presenter · view   ← MVP legacy
```
