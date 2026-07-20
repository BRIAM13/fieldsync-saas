package com.corporacionronceros.fieldsync.routes

import com.corporacionronceros.fieldsync.model.ApiError
import com.corporacionronceros.fieldsync.model.StatusUpdateRequest
import com.corporacionronceros.fieldsync.model.SyncRequest
import com.corporacionronceros.fieldsync.model.SyncResponse
import com.corporacionronceros.fieldsync.repository.WorkOrderRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/** Rutas REST de órdenes de trabajo, montadas bajo /api. */
fun Route.workOrderRoutes(repository: WorkOrderRepository) {
    route("/api/work-orders") {

        // Listado completo
        get {
            call.respond(repository.all())
        }

        // Una orden por id
        get("/{id}") {
            val id = call.parameters["id"]!!
            val order = repository.byId(id)
            if (order == null) {
                call.respond(HttpStatusCode.NotFound, ApiError("Orden $id no encontrada"))
            } else {
                call.respond(order)
            }
        }

        // Cambio de estado
        patch("/{id}/status") {
            val id = call.parameters["id"]!!
            val body = call.receive<StatusUpdateRequest>()
            val updated = repository.updateStatus(id, body.status)
            if (updated == null) {
                call.respond(HttpStatusCode.NotFound, ApiError("Orden $id no encontrada"))
            } else {
                call.respond(updated)
            }
        }
    }

    // Sincronización en bloque de cambios pendientes (espejo del offline-first de Android)
    post("/api/sync") {
        val request = call.receive<SyncRequest>()
        val (applied, rejected) = repository.applyPending(request.changes)
        call.respond(SyncResponse(synced = applied, rejected = rejected))
    }
}
