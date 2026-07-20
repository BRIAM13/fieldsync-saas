package com.corporacionronceros.fieldsync.plugins

import com.corporacionronceros.fieldsync.repository.AuthRepository
import com.corporacionronceros.fieldsync.repository.WorkOrderRepository
import com.corporacionronceros.fieldsync.routes.authRoutes
import com.corporacionronceros.fieldsync.routes.trackingRoutes
import com.corporacionronceros.fieldsync.routes.workOrderRoutes
import com.corporacionronceros.fieldsync.security.JwtService
import com.corporacionronceros.fieldsync.tracking.TrackingService
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

/** Monta todas las rutas: salud + auth (públicas) + API protegida por JWT + WebSocket. */
fun Application.configureRouting(
    repository: WorkOrderRepository,
    authRepository: AuthRepository,
    trackingService: TrackingService,
    jwtService: JwtService
) {
    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok", "service" to "fieldsync-backend"))
        }

        // Públicas
        authRoutes(authRepository, jwtService)

        // Protegidas: exigen un JWT válido; el tenant se resuelve del token
        authenticate(AUTH_JWT) {
            workOrderRoutes(repository)
        }

        // WebSocket: valida el token por query param dentro de la ruta
        trackingRoutes(trackingService, jwtService)
    }
}
