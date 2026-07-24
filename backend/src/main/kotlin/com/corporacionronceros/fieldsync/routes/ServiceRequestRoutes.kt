package com.corporacionronceros.fieldsync.routes

import com.corporacionronceros.fieldsync.model.ApiError
import com.corporacionronceros.fieldsync.model.ServiceRequestCreate
import com.corporacionronceros.fieldsync.repository.CustomerRepository
import com.corporacionronceros.fieldsync.repository.WorkOrderRepository
import com.corporacionronceros.fieldsync.security.authorizeCustomer
import com.corporacionronceros.fieldsync.security.companyId
import com.corporacionronceros.fieldsync.security.customerId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

/** El cliente solicita un servicio y trackea sus propias solicitudes. Solo clientes (no staff). */
fun Route.serviceRequestRoutes(repository: WorkOrderRepository, customerRepository: CustomerRepository) {
    authorizeCustomer {
        post("/api/service-requests") {
            val req = call.receive<ServiceRequestCreate>()
            if (req.title.isBlank() || req.address.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiError("Título y dirección son obligatorios"))
                return@post
            }
            if (req.lat !in -90.0..90.0 || req.lng !in -180.0..180.0) {
                call.respond(HttpStatusCode.BadRequest, ApiError("Ubicación inválida"))
                return@post
            }
            val customer = customerRepository.findById(call.customerId())
            if (customer == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiError("Cliente no encontrado"))
                return@post
            }
            val order = repository.create(call.companyId(), customer.id, customer.name, req)
            call.respond(HttpStatusCode.Created, order)
        }

        get("/api/service-requests/mine") {
            call.respond(repository.byCustomer(call.companyId(), call.customerId()))
        }
    }
}
