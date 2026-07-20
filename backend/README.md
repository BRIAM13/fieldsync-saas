# FieldSync — Backend (Ktor · Kotlin)

API compartida que conecta las tres apps de FieldSync. Escrita en **Kotlin con Ktor**, para
mantener el ecosistema completo en un solo lenguaje (dispositivo ↔ servidor).

## Stack

- **Ktor 2.3** sobre **Netty** — servidor asíncrono con Coroutines
- **kotlinx.serialization** — JSON tipado
- **WebSockets** — seguimiento del técnico en tiempo real
- **CORS** — consumo desde el panel Angular y las apps móviles
- **CallLogging + StatusPages** — observabilidad y errores JSON uniformes
- Tests con el **test-host de Ktor** (sin abrir puertos reales)

## Arquitectura

```
src/main/kotlin/com/corporacionronceros/fieldsync/
├── Application.kt        punto de entrada (embeddedServer) + ensamblado de plugins
├── plugins/              Serialization · HTTP(CORS) · Sockets · Monitoring · Routing
├── model/               DTOs @Serializable (WorkOrder, TechnicianLocation, sync)
├── repository/          WorkOrderRepository (interfaz) + InMemory (Mutex, thread-safe)
├── routes/              workOrderRoutes (REST) · trackingRoutes (WebSocket)
└── tracking/            TrackingService (Flow de posiciones)
```

El repositorio es una **interfaz**: la implementación en memoria se puede cambiar por una con
base de datos (Exposed + Postgres) sin tocar las rutas.

## API

| Método | Ruta | Descripción |
|--------|------|-------------|
| `GET` | `/health` | Estado del servicio |
| `GET` | `/api/work-orders` | Lista todas las órdenes |
| `GET` | `/api/work-orders/{id}` | Una orden por id (404 si no existe) |
| `PATCH` | `/api/work-orders/{id}/status` | Cambia el estado. Body: `{ "status": "IN_PROGRESS" }` |
| `POST` | `/api/sync` | Aplica cambios pendientes en bloque (offline-first). Body: `{ "changes": [{ "id": "WO-1042", "status": "COMPLETED" }] }` → `{ "synced": 1, "rejected": [] }` |
| `WS` | `/ws/tracking/{orderId}` | Stream en tiempo real de la posición del técnico + ETA |

### Cómo conecta las tres apps

- **Android** (`WorkOrderApi`) → `GET /api/work-orders` y `POST /api/sync` (el flujo offline-first
  del dispositivo empuja aquí sus cambios pendientes vía WorkManager).
- **Angular** (`WorkOrderService`) → `GET`/`PATCH` de órdenes para el panel de despacho.
- **React Native** (`TrackingService`) → el WebSocket `/ws/tracking/{orderId}` para el seguimiento en vivo.

> Hoy cada cliente usa un stub local con estos mismos contratos; apuntarlos a este backend es un
> cambio de URL base (ver los `TODO: baseUrl` en cada servicio cliente).

## Ejecutar

Requiere **JDK 17 o 21** (recomendado; el toolchain de Kotlin/Gradle aún no soporta JDK 25).

```bash
cd backend
gradle run          # o abrir en IntelliJ IDEA y ejecutar Application.kt
# → http://localhost:8080/health

gradle test         # tests de integración de la API
```

Prueba rápida:

```bash
curl http://localhost:8080/api/work-orders
curl -X PATCH http://localhost:8080/api/work-orders/WO-1042/status \
     -H "Content-Type: application/json" -d '{"status":"IN_PROGRESS"}'
```
