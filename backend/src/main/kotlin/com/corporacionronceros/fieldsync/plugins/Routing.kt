package com.corporacionronceros.fieldsync.plugins

import com.corporacionronceros.fieldsync.db.DatabaseFactory
import com.corporacionronceros.fieldsync.repository.AuthRepository
import com.corporacionronceros.fieldsync.repository.CustomerRepository
import com.corporacionronceros.fieldsync.repository.WorkOrderRepository
import com.corporacionronceros.fieldsync.routes.authRoutes
import com.corporacionronceros.fieldsync.routes.companyRoutes
import com.corporacionronceros.fieldsync.routes.customerAuthRoutes
import com.corporacionronceros.fieldsync.routes.serviceRequestRoutes
import com.corporacionronceros.fieldsync.routes.trackingRoutes
import com.corporacionronceros.fieldsync.routes.userRoutes
import com.corporacionronceros.fieldsync.routes.workOrderRoutes
import com.corporacionronceros.fieldsync.security.JwtService
import com.corporacionronceros.fieldsync.tracking.TrackingService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

/** Monta todas las rutas: salud + auth (públicas) + API protegida por JWT + WebSocket. */
fun Application.configureRouting(
    repository: WorkOrderRepository,
    authRepository: AuthRepository,
    customerRepository: CustomerRepository,
    trackingService: TrackingService,
    jwtService: JwtService
) {
    routing {
        // Toca la DB de verdad (SELECT 1) cuando hay Postgres configurado — no es solo
        // un "sigo vivo": confirma la conexión real y, pingueado periódicamente (p. ej.
        // UptimeRobot), genera la actividad que evita el auto-apagado de proveedores
        // free-tier basados en inactividad (ver backend/README.md § Mantener despierto).
        get("/health") {
            val dbHealthy = DatabaseFactory.ping()
            val body = mapOf(
                "status" to if (dbHealthy) "ok" else "degraded",
                "service" to "fieldsync-backend",
                "db" to if (!DatabaseFactory.isConfigured) "in-memory" else if (dbHealthy) "connected" else "unreachable"
            )
            call.respond(if (dbHealthy) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable, body)
        }

        // Públicas
        authRoutes(authRepository, jwtService)
        customerAuthRoutes(customerRepository, authRepository, jwtService)
        companyRoutes(authRepository)

        // Protegidas (staff): exigen un JWT de staff válido; el tenant se resuelve del token,
        // y cada ruta aplica sus propios roles (RBAC) donde corresponde.
        authenticate(AUTH_JWT) {
            workOrderRoutes(repository)
            userRoutes(authRepository)
        }

        // Protegidas (clientes): proveedor de autenticación separado — un token de staff
        // nunca autentica aquí, y viceversa (ver plugins/Security.kt).
        authenticate(AUTH_JWT_CUSTOMER) {
            serviceRequestRoutes(repository, customerRepository)
        }

        // WebSocket: valida el token por query param dentro de la ruta
        trackingRoutes(trackingService, jwtService)
    }
}
