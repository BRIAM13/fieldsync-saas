package com.corporacionronceros.fieldsync.routes

import com.corporacionronceros.fieldsync.model.ApiError
import com.corporacionronceros.fieldsync.model.AssignmentRequest
import com.corporacionronceros.fieldsync.model.StatusUpdateRequest
import com.corporacionronceros.fieldsync.model.SyncRequest
import com.corporacionronceros.fieldsync.model.SyncResponse
import com.corporacionronceros.fieldsync.repository.WorkOrderRepository
import com.corporacionronceros.fieldsync.security.companyId
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/**
 * Rutas REST de órdenes, montadas bajo /api DENTRO de un bloque `authenticate`.
 * Cada operación se resuelve al tenant del token vía `call.companyId()` — aislamiento multi-tenant.
 */
fun Route.workOrderRoutes(repository: WorkOrderRepository) {
    route("/api/work-orders") {

        get {
            call.respond(repository.all(call.companyId()))
        }

        get("/{id}") {
            val id = call.parameters["id"]!!
            val order = repository.byId(call.companyId(), id)
            if (order == null) {
                call.respond(HttpStatusCode.NotFound, ApiError("Orden $id no encontrada"))
            } else {
                call.respond(order)
            }
        }

        patch("/{id}/status") {
            val id = call.parameters["id"]!!
            val body = call.receive<StatusUpdateRequest>()
            val updated = repository.updateStatus(call.companyId(), id, body.status)
            if (updated == null) {
                call.respond(HttpStatusCode.NotFound, ApiError("Orden $id no encontrada"))
            } else {
                call.respond(updated)
            }
        }

        patch("/{id}/assignment") {
            val id = call.parameters["id"]!!
            val body = call.receive<AssignmentRequest>()
            val updated = repository.assign(call.companyId(), id, body.technicianId)
            if (updated == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ApiError("Orden $id o técnico ${body.technicianId} no encontrado")
                )
            } else {
                call.respond(updated)
            }
        }
    }

    get("/api/technicians") {
        call.respond(repository.technicians(call.companyId()))
    }

    post("/api/sync") {
        val request = call.receive<SyncRequest>()
        val (applied, rejected) = repository.applyPending(call.companyId(), request.changes)
        call.respond(SyncResponse(synced = applied, rejected = rejected))
    }
}
