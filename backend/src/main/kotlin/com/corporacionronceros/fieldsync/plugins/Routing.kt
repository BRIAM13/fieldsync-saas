package com.corporacionronceros.fieldsync.plugins

import com.corporacionronceros.fieldsync.repository.WorkOrderRepository
import com.corporacionronceros.fieldsync.routes.trackingRoutes
import com.corporacionronceros.fieldsync.routes.workOrderRoutes
import com.corporacionronceros.fieldsync.tracking.TrackingService
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

/** Monta todas las rutas de la API. */
fun Application.configureRouting(
    repository: WorkOrderRepository,
    trackingService: TrackingService
) {
    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok", "service" to "fieldsync-backend"))
        }

        workOrderRoutes(repository)
        trackingRoutes(trackingService)
    }
}
