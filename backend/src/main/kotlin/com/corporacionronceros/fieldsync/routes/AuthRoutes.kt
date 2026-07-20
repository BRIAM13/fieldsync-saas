package com.corporacionronceros.fieldsync.routes

import com.corporacionronceros.fieldsync.model.ApiError
import com.corporacionronceros.fieldsync.model.AuthResponse
import com.corporacionronceros.fieldsync.model.LoginRequest
import com.corporacionronceros.fieldsync.model.RegisterRequest
import com.corporacionronceros.fieldsync.repository.AuthRepository
import com.corporacionronceros.fieldsync.security.JwtService
import com.corporacionronceros.fieldsync.security.PasswordHasher
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/** Rutas públicas de autenticación: registro (crea empresa + admin) e inicio de sesión. */
fun Route.authRoutes(authRepository: AuthRepository, jwtService: JwtService) {
    route("/auth") {

        post("/register") {
            val req = call.receive<RegisterRequest>()
            if (req.email.isBlank() || req.password.length < 6 || req.companyName.isBlank()) {
                call.respond(HttpStatusCode.BadRequest,
                    ApiError("Email, empresa y contraseña (≥6) son obligatorios"))
                return@post
            }
            if (authRepository.emailExists(req.email)) {
                call.respond(HttpStatusCode.Conflict, ApiError("El email ya está registrado"))
                return@post
            }
            val hash = PasswordHasher.hash(req.password)
            val result = authRepository.createCompanyWithAdmin(req.companyName, req.name, req.email, hash)
            val token = jwtService.generateToken(result.user)
            call.respond(HttpStatusCode.Created, AuthResponse(token, result.user, result.company))
        }

        post("/login") {
            val req = call.receive<LoginRequest>()
            val record = authRepository.findByEmail(req.email)
            if (record == null || !PasswordHasher.verify(req.password, record.passwordHash)) {
                call.respond(HttpStatusCode.Unauthorized, ApiError("Credenciales inválidas"))
                return@post
            }
            val company = authRepository.companyById(record.user.companyId)
            if (company == null) {
                call.respond(HttpStatusCode.InternalServerError, ApiError("Empresa no encontrada"))
                return@post
            }
            val token = jwtService.generateToken(record.user)
            call.respond(AuthResponse(token, record.user, company))
        }
    }
}
