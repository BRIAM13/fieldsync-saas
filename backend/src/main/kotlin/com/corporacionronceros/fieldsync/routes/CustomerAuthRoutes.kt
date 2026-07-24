package com.corporacionronceros.fieldsync.routes

import com.corporacionronceros.fieldsync.model.ApiError
import com.corporacionronceros.fieldsync.model.Customer
import com.corporacionronceros.fieldsync.model.CustomerAuthResponse
import com.corporacionronceros.fieldsync.model.CustomerLoginRequest
import com.corporacionronceros.fieldsync.model.CustomerLogoutRequest
import com.corporacionronceros.fieldsync.model.CustomerRefreshRequest
import com.corporacionronceros.fieldsync.model.CustomerRegisterRequest
import com.corporacionronceros.fieldsync.repository.AuthRepository
import com.corporacionronceros.fieldsync.repository.CustomerRepository
import com.corporacionronceros.fieldsync.security.JwtService
import com.corporacionronceros.fieldsync.security.PasswordHasher
import com.corporacionronceros.fieldsync.security.rejectIfAbusiveRegistration
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/** Rutas públicas de autenticación de **clientes** (no staff): registro, login, refresh, logout. */
fun Route.customerAuthRoutes(
    customerRepository: CustomerRepository,
    authRepository: AuthRepository,
    jwtService: JwtService
) {
    route("/customer/auth") {

        post("/register") {
            val req = call.receive<CustomerRegisterRequest>()
            if (req.email.isBlank() || req.password.length < 6 || req.companyId.isBlank()) {
                call.respond(HttpStatusCode.BadRequest,
                    ApiError("Email, empresa y contraseña (≥6) son obligatorios"))
                return@post
            }
            if (call.rejectIfAbusiveRegistration(req.email, req.website, "customer")) return@post
            if (authRepository.companyById(req.companyId) == null) {
                call.respond(HttpStatusCode.BadRequest, ApiError("Empresa no encontrada"))
                return@post
            }
            if (customerRepository.emailExists(req.email)) {
                call.respond(HttpStatusCode.Conflict, ApiError("El email ya está registrado"))
                return@post
            }
            val hash = PasswordHasher.hash(req.password)
            val customer = customerRepository.createCustomer(req.companyId, req.name, req.email, req.phone, hash)
            if (customer == null) {
                call.respond(HttpStatusCode.Conflict, ApiError("El email ya está registrado"))
                return@post
            }
            call.respondWithCustomerTokens(customerRepository, authRepository, jwtService, customer, HttpStatusCode.Created)
        }

        post("/login") {
            val req = call.receive<CustomerLoginRequest>()
            val record = customerRepository.findByEmail(req.email)
            if (record == null || !PasswordHasher.verify(req.password, record.passwordHash)) {
                call.respond(HttpStatusCode.Unauthorized, ApiError("Credenciales inválidas"))
                return@post
            }
            call.respondWithCustomerTokens(customerRepository, authRepository, jwtService, record.customer, HttpStatusCode.OK)
        }

        post("/refresh") {
            val req = call.receive<CustomerRefreshRequest>()
            val customer = customerRepository.consumeRefreshToken(req.refreshToken)
            if (customer == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiError("Refresh token inválido o expirado"))
                return@post
            }
            call.respondWithCustomerTokens(customerRepository, authRepository, jwtService, customer, HttpStatusCode.OK)
        }

        post("/logout") {
            val req = call.receive<CustomerLogoutRequest>()
            customerRepository.revokeRefreshToken(req.refreshToken)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

/** Emite un access token + un refresh token nuevo (rotación) y responde con ambos. */
private suspend fun ApplicationCall.respondWithCustomerTokens(
    customerRepository: CustomerRepository,
    authRepository: AuthRepository,
    jwtService: JwtService,
    customer: Customer,
    status: HttpStatusCode
) {
    val company = authRepository.companyById(customer.companyId)
    if (company == null) {
        respond(HttpStatusCode.InternalServerError, ApiError("Empresa no encontrada"))
        return
    }
    val accessToken = jwtService.generateCustomerAccessToken(customer)
    val expiresAt = System.currentTimeMillis() + jwtService.refreshValidityMs
    val refreshToken = customerRepository.createRefreshToken(customer.id, expiresAt)
    respond(status, CustomerAuthResponse(accessToken, refreshToken, customer, company))
}
