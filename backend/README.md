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

## Autenticación (JWT) y multi-tenancy

Toda la API de negocio exige un **JWT** (`Authorization: Bearer <token>`). El token lleva el
`companyId`, y **cada consulta se aísla por empresa** (tenant): una empresa nunca ve datos de otra.

- `POST /auth/register` → crea una **empresa nueva + su usuario admin**; devuelve access + refresh token.
- `POST /auth/login` → devuelve access token + refresh token + usuario + empresa.
- `POST /auth/refresh` → renueva el access token (**rota** el refresh token: el viejo queda inválido).
- `POST /auth/logout` → revoca el refresh token.

**Cuentas demo** (sembradas, todas con contraseña `demo1234`):

| Email | Rol | Puede |
|-------|-----|-------|
| `admin@fieldsync.dev` | ADMIN | Todo, incluido crear usuarios |
| `dispatcher@fieldsync.dev` | DISPATCHER | Ver, asignar órdenes y ver técnicos |
| `tech@fieldsync.dev` | TECHNICIAN | Ver órdenes, cambiar estado, sincronizar |

### Roles y permisos (RBAC)

El rol viaja en el JWT y se exige por endpoint con el helper `Route.authorize(vararg roles)`
(responde **403** si el rol no está permitido):

| Endpoint | Roles permitidos |
|----------|------------------|
| `GET /api/work-orders`, `GET /{id}`, `PATCH /{id}/status`, `POST /api/sync` | ADMIN · DISPATCHER · TECHNICIAN |
| `PATCH /api/work-orders/{id}/assignment`, `GET /api/technicians` | ADMIN · DISPATCHER |
| `POST /api/users` (crear usuario en el tenant) | ADMIN |

- Contraseñas hasheadas con **BCrypt** (nunca en claro).
- **Access token corto** (15 min) + **refresh token largo** (30 días), opaco (256 bits) y
  persistido/revocable en la tabla `refresh_tokens`.
- Config del JWT por entorno (`JWT_SECRET`, `JWT_ISSUER`, `JWT_AUDIENCE`, `JWT_ACCESS_VALIDITY_MS`, `JWT_REFRESH_VALIDITY_MS`).
- El WebSocket recibe el token como **query param** (`?token=…`), ya que no admite headers.

## API

| Método | Ruta | Auth | Descripción |
|--------|------|:----:|-------------|
| `POST` | `/auth/register` | — | Crea empresa + admin → access + refresh token |
| `POST` | `/auth/login` | — | Login → access + refresh token |
| `POST` | `/auth/refresh` | — | Renueva el access token (rota el refresh). Body: `{ "refreshToken": "…" }` |
| `POST` | `/auth/logout` | — | Revoca el refresh token. Body: `{ "refreshToken": "…" }` |
| `GET` | `/health` | — | Estado del servicio |
| `GET` | `/api/work-orders` | 🔒 | Órdenes de **mi** empresa |
| `GET` | `/api/work-orders/{id}` | 🔒 | Una orden (404 si no es mía o no existe) |
| `PATCH` | `/api/work-orders/{id}/status` | 🔒 | Cambia el estado. Body: `{ "status": "IN_PROGRESS" }` |
| `PATCH` | `/api/work-orders/{id}/assignment` | 🔒 admin/disp | Asigna a un técnico. Body: `{ "technicianId": "T-01" }` |
| `GET` | `/api/technicians` | 🔒 admin/disp | Técnicos de mi empresa |
| `POST` | `/api/users` | 🔒 admin | Crea un usuario. Body: `{ "name","email","password","role" }` |
| `POST` | `/api/sync` | 🔒 | Aplica cambios pendientes en bloque (offline-first) |
| `WS` | `/ws/tracking/{orderId}?token=JWT` | 🔒 | Stream en tiempo real de posición + ETA |

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

Prueba rápida (login → usar el token):
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@fieldsync.dev","password":"demo1234"}' | jq -r .token)

curl http://localhost:8080/api/work-orders -H "Authorization: Bearer $TOKEN"
```

## Desplegar gratis

> ✅ **Ya desplegado:** [fieldsync-backend-cipm.onrender.com](https://fieldsync-backend-cipm.onrender.com/health)
> — Render + Aiven Postgres, verificado en vivo (health check, login, API autenticada) **y con
> monitoreo activo** (UptimeRobot, cada 5 min, estado **Up**). Los tres clientes ya apuntan aquí
> por defecto. Lo que sigue documenta cómo se hizo / cómo replicarlo.

El backend se despliega con **Render** (servidor) + Postgres de un proveedor gratuito a tu
elección. El repo trae [`render.yaml`](../render.yaml) en la raíz (Blueprint de Render) para
que el servicio quede preconfigurado — solo falta pegar la URL de la base de datos.

### Elegir proveedor de Postgres: Neon vs. Aiven

Ambos tienen plan gratuito **recurrente** (sin fecha de vencimiento tipo "trial de 12 meses"),
pero el mecanismo de límite es distinto y cambia qué estrategia tiene sentido:

| | **Neon** | **Aiven** |
|---|---|---|
| Límite | **100 horas-CU al mes** (tope duro) | Sin tope de horas — apaga por inactividad |
| Al inactividad | Escala a cero en ~5 min (te ahorra horas) | Se apaga tras un rato sin uso (umbral no publicado) |
| Al agotar el límite | Se suspende hasta el próximo mes | No aplica (no hay contador de horas) |
| ¿Se puede mantener despierto con un ping? | **No — contraproducente.** Pinguearlo 24/7 agota las 100 h en ~4 días y lo suspende por semanas | **Sí, en teoría** — el ping cuenta como actividad y evita el apagado |
| Recursos | Variable según CU | 1 CPU / 1 GB RAM / 1 GB storage, fijo |

**Recomendación:** si aceptas cold-starts ocasionales (perfecto para un portafolio), usa
**Neon** — es el camino más simple, sin trucos. Si quieres exprimir el free tier para que se
sienta "siempre arriba", usa **Aiven** + el keep-alive descrito abajo — con la salvedad honesta
de que ningún free tier está pensado para producción real (ver advertencia al final).

1. **Neon** ([neon.tech](https://neon.tech)) o **Aiven** ([aiven.io](https://aiven.io), servicio
   PostgreSQL, plan **Free**) → crea una cuenta y el servicio → copia el **connection string**
   (`postgres://usuario:contraseña@host:puerto/db`).
2. **Render** ([render.com](https://render.com)) → crea una cuenta → *New* → **Blueprint** →
   conecta tu GitHub y selecciona este repo. Render detecta `render.yaml` automáticamente y
   preconfigura el servicio `fieldsync-backend` (Docker, `backend/Dockerfile`, plan free,
   health check en `/health`, `JWT_SECRET` autogenerado).
3. Antes de confirmar el deploy, pega la `DATABASE_URL` (de Neon o Aiven) en el campo que Render
   deja vacío para esa variable (queda marcada `sync: false` en el blueprint — **nunca va en el
   repo en texto plano**, se pega directo en el dashboard de Render).
4. Deploy. El backend crea el esquema y siembra los datos demo en el primer arranque.
5. Prueba: `curl https://<tu-servicio>.onrender.com/health` → debe responder
   `{"status":"ok","db":"connected",...}`.

> ⚠️ En free tier, el **servicio de Render duerme tras ~15 min de inactividad** (cold start en la
> primera petición) además del comportamiento propio de tu proveedor de Postgres (tabla arriba).
> Verifica los términos vigentes de cada proveedor antes de desplegar — cambian seguido.

### Mantener despierto con UptimeRobot (opcional)

`GET /health` no es un simple "sigo vivo": cuando hay Postgres configurado, ejecuta un
`SELECT 1` real contra la base. Eso significa que pinguearlo periódicamente cumple dos
funciones a la vez — confirma que el backend **y** la base de datos responden, y genera
tráfico real hacia la DB, lo cual **evita el auto-apagado en proveedores basados en inactividad
como Aiven** (en Neon, no lo hagas — ver tabla arriba, es contraproducente por su tope de horas).

1. Crea una cuenta gratis en [UptimeRobot](https://uptimerobot.com).
2. **Add New Monitor** → tipo `HTTP(s)` → URL: `https://<tu-servicio>.onrender.com/health`.
3. Intervalo: **cada 5 minutos** (cómodamente por debajo de los ~15 min de Render y de
   cualquier umbral razonable de inactividad de Aiven, que no publican un número exacto).
4. Guarda. UptimeRobot te avisará además si el backend o la DB caen (respuesta ≠ 200).

> ⚠️ **Gotcha real que nos pasó**: el monitor `HTTP(s)` de UptimeRobot pinguea con **`HEAD`**
> por defecto, no `GET`. Una ruta de Ktor registrada solo con `get("/health") { ... }` no
> responde a `HEAD` — UptimeRobot marcaba el servicio como "Abajo" con `405 Method Not Allowed`
> en cada chequeo, aunque `GET /health` funcionara perfecto en el navegador o con `curl`. El
> backend ya trae el fix (`install(AutoHeadResponse)` en `plugins/Monitoring.kt`, responde
> `HEAD` automáticamente en toda ruta `GET`); si escribes una ruta nueva pensada para un monitor
> externo, verifica también con `curl -I` (`HEAD`), no solo con `curl` (`GET`) — es fácil que
> pase el chequeo manual y falle igual con el monitor real.
>
> **Confirma que quedó bien**: en el dashboard de UptimeRobot, el estado debe decir **Up** en
> verde y "Últimas 24 horas" en 100% — no te fíes solo de que `/health` responda rápido a
> mano, eso no prueba que el monitor mismo esté contento.

**Antes de depender de esto para un SaaS real**, dos advertencias sin filtro:

- Es una técnica **común y generalmente tolerada** para mantener despiertos servicios gratuitos,
  pero **no es un uso oficialmente soportado** — ambos proveedores presentan su plan gratuito
  como "para explorar/aprender", no para producción continua. No hay garantía de que no limiten
  o penalicen cuentas que lo hacen 24/7.
- Aunque se mantenga despierto, el techo de **1 CPU / 1 GB RAM** (Aiven free) sigue siendo el
  límite real de capacidad para tráfico de verdad.
- Si esto va a servir a usuarios reales que dependen de la disponibilidad, la alternativa más
  robusta y barata es un plan pago pequeño (p. ej. Render Starter ~$7/mes + Aiven Developer
  ~$5/mes, que además **no** se apaga por inactividad) en vez de pelear contra los límites
  gratuitos con keep-alives.

| Variable | Ejemplo | Notas |
|----------|---------|-------|
| `DATABASE_URL` | `postgres://user:pass@host/db` | Sin ella → modo en memoria. Pégala en Render, no en el repo |
| `DB_USER` / `DB_PASSWORD` | — | Solo si usas `DATABASE_URL` en formato `jdbc:` |
| `PORT` | `8080` | El host suele inyectarla |
| `DB_POOL_SIZE` | `5` | Tamaño del pool Hikari (opcional) |
| `JWT_SECRET` | (autogenerado por el blueprint) | Cámbialo si no usas el blueprint |
| `JWT_ISSUER` / `JWT_AUDIENCE` | `fieldsync` / `fieldsync-clients` | Opcionales |
| `JWT_ACCESS_VALIDITY_MS` | `900000` | Vigencia del access token (15 min por defecto) |
| `JWT_REFRESH_VALIDITY_MS` | `2592000000` | Vigencia del refresh token (30 días por defecto) |
