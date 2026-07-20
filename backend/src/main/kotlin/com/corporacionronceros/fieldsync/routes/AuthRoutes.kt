package com.corporacionronceros.fieldsync.routes

import com.corporacionronceros.fieldsync.model.ApiError
import com.corporacionronceros.fieldsync.model.AuthResponse
import com.corporacionronceros.fieldsync.model.LoginRequest
import com.corporacionronceros.fieldsync.model.LogoutRequest
import com.corporacionronceros.fieldsync.model.RefreshRequest
import com.corporacionronceros.fieldsync.model.RegisterRequest
import com.corporacionronceros.fieldsync.model.User
import com.corporacionronceros.fieldsync.repository.AuthRepository
import com.corporacionronceros.fieldsync.security.JwtService
import com.corporacionronceros.fieldsync.security.PasswordHasher
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/** Rutas públicas de autenticación: registro, login, refresh y logout. */
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
            call.respondWithTokens(authRepository, jwtService, result.user, HttpStatusCode.Created)
        }

        post("/login") {
            val req = call.receive<LoginRequest>()
            val record = authRepository.findByEmail(req.email)
            if (record == null || !PasswordHasher.verify(req.password, record.passwordHash)) {
                call.respond(HttpStatusCode.Unauthorized, ApiError("Credenciales inválidas"))
                return@post
            }
            call.respondWithTokens(authRepository, jwtService, record.user, HttpStatusCode.OK)
        }

        post("/refresh") {
            val req = call.receive<RefreshRequest>()
            val user = authRepository.consumeRefreshToken(req.refreshToken)
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiError("Refresh token inválido o expirado"))
                return@post
            }
            call.respondWithTokens(authRepository, jwtService, user, HttpStatusCode.OK)
        }

        post("/logout") {
            val req = call.receive<LogoutRequest>()
            authRepository.revokeRefreshToken(req.refreshToken)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

/** Emite un access token + un refresh token nuevo (rotación) y responde con ambos. */
private suspend fun ApplicationCall.respondWithTokens(
    authRepository: AuthRepository,
    jwtService: JwtService,
    user: User,
    status: HttpStatusCode
) {
    val company = authRepository.companyById(user.companyId)
    if (company == null) {
        respond(HttpStatusCode.InternalServerError, ApiError("Empresa no encontrada"))
        return
    }
    val accessToken = jwtService.generateAccessToken(user)
    val expiresAt = System.currentTimeMillis() + jwtService.refreshValidityMs
    val refreshToken = authRepository.createRefreshToken(user.id, expiresAt)
    respond(status, AuthResponse(accessToken, refreshToken, user, company))
}
