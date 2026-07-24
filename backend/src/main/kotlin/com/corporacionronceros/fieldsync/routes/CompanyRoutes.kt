package com.corporacionronceros.fieldsync.routes

import com.corporacionronceros.fieldsync.model.CompanySummary
import com.corporacionronceros.fieldsync.repository.AuthRepository
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/** Lista pública de empresas (id + nombre) — el cliente elige la suya al registrarse. */
fun Route.companyRoutes(authRepository: AuthRepository) {
    get("/api/companies") {
        call.respond(authRepository.allCompanies().map { CompanySummary(it.id, it.name) })
    }
}
