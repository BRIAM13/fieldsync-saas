package com.corporacionronceros.fieldsync.routes

import com.corporacionronceros.fieldsync.model.ApiError
import com.corporacionronceros.fieldsync.model.CreateUserRequest
import com.corporacionronceros.fieldsync.model.UserRole
import com.corporacionronceros.fieldsync.repository.AuthRepository
import com.corporacionronceros.fieldsync.security.PasswordHasher
import com.corporacionronceros.fieldsync.security.authorize
import com.corporacionronceros.fieldsync.security.companyId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

/** Gestión de usuarios dentro del tenant. Solo el ADMIN puede ver y crear usuarios. */
fun Route.userRoutes(authRepository: AuthRepository) {
    authorize(UserRole.ADMIN) {
        get("/api/users") {
            call.respond(authRepository.listUsers(call.companyId()))
        }

        post("/api/users") {
            val req = call.receive<CreateUserRequest>()
            if (req.email.isBlank() || req.password.length < 6) {
                call.respond(HttpStatusCode.BadRequest,
                    ApiError("Email y contraseña (≥6) son obligatorios"))
                return@post
            }
            val user = authRepository.createUser(
                companyId = call.companyId(),
                name = req.name,
                email = req.email,
                passwordHash = PasswordHasher.hash(req.password),
                role = req.role
            )
            if (user == null) {
                call.respond(HttpStatusCode.Conflict, ApiError("El email ya está registrado"))
            } else {
                call.respond(HttpStatusCode.Created, user)
            }
        }
    }
}
