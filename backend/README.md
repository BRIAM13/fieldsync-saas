# FieldSync — Backend (Ktor · Kotlin)

API compartida que conecta las tres apps de FieldSync. Escrita en **Kotlin con Ktor**, para
mantener el ecosistema completo en un solo lenguaje (dispositivo ↔ servidor).

## Stack

- **Ktor 2.3** sobre **Netty** — servidor asíncrono con Coroutines
- **kotlinx.serialization** — JSON tipado
- **Exposed + PostgreSQL + HikariCP** — persistencia (transacciones suspend con coroutines)
- **WebSockets** — seguimiento del técnico en tiempo real
- **CORS** — consumo desde el panel Angular y las apps móviles
- **CallLogging + StatusPages** — observabilidad y errores JSON uniformes
- Tests con el **test-host de Ktor** (sin abrir puertos reales)
- **Docker** + `docker-compose` — listo para desplegar

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

El repositorio es una **interfaz** con dos implementaciones intercambiables **sin tocar las rutas**:
`InMemoryWorkOrderRepository` (desarrollo, efímero) y `ExposedWorkOrderRepository` (Postgres).

## Persistencia

La elección es **por variable de entorno**, en `Application.module()`:

- **Sin `DATABASE_URL`** → repositorio **en memoria** (arranque instantáneo, datos efímeros).
- **Con `DATABASE_URL`** → **Postgres vía Exposed**: `DatabaseFactory` abre el pool (Hikari),
  crea el esquema (`SchemaUtils.create`) y siembra datos si las tablas están vacías.

`DATABASE_URL` acepta el formato de los proveedores (`postgres://user:pass@host:port/db`, como
lo exponen **Neon / Supabase / Render**) o `jdbc:postgresql://...` con `DB_USER` / `DB_PASSWORD`.
Las tablas (`work_orders`, `technicians`) se definen con el DSL tipado de Exposed en `db/Tables.kt`.

## API

| Método | Ruta | Descripción |
|--------|------|-------------|
| `GET` | `/health` | Estado del servicio |
| `GET` | `/api/work-orders` | Lista todas las órdenes |
| `GET` | `/api/work-orders/{id}` | Una orden por id (404 si no existe) |
| `PATCH` | `/api/work-orders/{id}/status` | Cambia el estado. Body: `{ "status": "IN_PROGRESS" }` |
| `PATCH` | `/api/work-orders/{id}/assignment` | Asigna a un técnico. Body: `{ "technicianId": "T-01" }` |
| `GET` | `/api/technicians` | Técnicos disponibles para asignar |
| `POST` | `/api/sync` | Aplica cambios pendientes en bloque (offline-first). Body: `{ "changes": [{ "id": "WO-1042", "status": "COMPLETED" }] }` → `{ "synced": 1, "rejected": [] }` |
| `WS` | `/ws/tracking/{orderId}` | Stream en tiempo real de la posición del técnico + ETA |

### Cómo conecta las tres apps (ya cableadas, no stubs)

- **Android** (`WorkOrderApi`, **Ktor client**) → `GET /api/work-orders` y `PATCH /{id}/status`
  (el flujo offline-first del dispositivo empuja los cambios vía WorkManager). Base URL: `http://10.0.2.2:8080`.
- **Angular** (`WorkOrderService`, **HttpClient**) → `GET` órdenes/técnicos, `PATCH` asignación en el panel. Base URL: `http://localhost:8080`.
- **React Native** (`TrackingService`, **WebSocket**) → `/ws/tracking/{orderId}` para el seguimiento en vivo. Base URL: `ws://10.0.2.2:8080` (Android) / `ws://localhost:8080` (iOS).

> Las URLs base están centralizadas: `ApiConfig` (Android), la constante `API_BASE`
> (`work-order.service.ts` en Angular) y `config.ts` (React Native).

## Ejecutar

Requiere **JDK 17 o 21** (el toolchain de Kotlin/Gradle aún no soporta JDK 25).

**Modo desarrollo (sin DB, en memoria):**
```bash
cd backend
gradle run          # → http://localhost:8080/health
gradle test         # tests de integración de la API
```

**Con Postgres local (Docker):**
```bash
cd backend
docker compose up --build     # levanta Postgres + backend, conectados
```

Prueba rápida:
```bash
curl http://localhost:8080/api/work-orders
curl -X PATCH http://localhost:8080/api/work-orders/WO-1042/status \
     -H "Content-Type: application/json" -d '{"status":"IN_PROGRESS"}'
```

## Desplegar gratis (portafolio)

Combinación sin costo para demos: **Neon** (Postgres) + **Render** (servidor).

1. Crea un proyecto en **Neon** → copia su `DATABASE_URL` (`postgres://...`).
2. En **Render** → *New Web Service* apuntando a este repo (`backend/`), runtime Docker.
3. Configura las variables de entorno: `DATABASE_URL` (la de Neon) y `PORT` (Render lo inyecta).
4. Deploy. El backend crea el esquema y siembra datos en el primer arranque.

> ⚠️ En free tier, el **servicio de Render duerme tras ~15 min de inactividad** (cold start en la
> primera petición) y **Neon escala a cero** cuando está inactiva (despierta en la primera query).
> Perfecto para demo; para "always-on" real ambos requieren plan de pago. Verifica los términos
> vigentes de cada proveedor antes de desplegar.

| Variable | Ejemplo | Notas |
|----------|---------|-------|
| `DATABASE_URL` | `postgres://user:pass@host/db` | Sin ella → modo en memoria |
| `DB_USER` / `DB_PASSWORD` | — | Solo si usas `DATABASE_URL` en formato `jdbc:` |
| `PORT` | `8080` | El host suele inyectarla |
| `DB_POOL_SIZE` | `5` | Tamaño del pool Hikari (opcional) |
