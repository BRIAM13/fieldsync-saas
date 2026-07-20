package com.corporacionronceros.fieldsync.plugins

import com.corporacionronceros.fieldsync.model.ApiError
import com.corporacionronceros.fieldsync.security.JwtService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond

const val AUTH_JWT = "auth-jwt"

/** Instala la autenticación JWT; el principal expone los claims (userId, companyId, role). */
fun Application.configureSecurity(jwt: JwtService) {
    install(Authentication) {
        jwt(AUTH_JWT) {
            realm = jwt.realm
            verifier(jwt.verifier)
            validate { credential ->
                val companyId = credential.payload.getClaim(JwtService.CLAIM_COMPANY_ID).asString()
                val userId = credential.payload.getClaim(JwtService.CLAIM_USER_ID).asString()
                if (!companyId.isNullOrBlank() && !userId.isNullOrBlank()) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, ApiError("Token inválido o ausente"))
            }
        }
    }
}
