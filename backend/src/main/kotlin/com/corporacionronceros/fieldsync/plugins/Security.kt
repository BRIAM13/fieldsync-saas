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
const val AUTH_JWT_CUSTOMER = "auth-jwt-customer"

/**
 * Instala la autenticación JWT con **dos proveedores independientes**, sobre el mismo
 * verifier/clave: uno para staff (exige `userId`) y uno para clientes (exige `customerId`).
 * Un token de cliente no tiene `userId`, así que jamás autentica bajo [AUTH_JWT] — y viceversa,
 * un token de staff no tiene `customerId` y jamás autentica bajo [AUTH_JWT_CUSTOMER].
 *
 * El resultado siempre es un rechazo, pero el código exacto depende de la ruta: las rutas
 * "desnudas" (sin `authorize()`/`authorizeCustomer()` anidado, p. ej. `GET /api/work-orders`)
 * responden 401 vía el `challenge` de aquí abajo. Las rutas con guard de rol anidado (p. ej.
 * `POST /api/users` bajo `authorize(ADMIN)`) responden 403: ese guard corre antes de que el
 * challenge de autenticación tenga oportunidad de fijar la respuesta. En ningún caso un token
 * del proveedor equivocado llega a ejecutar la lógica de negocio de la ruta.
 */
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

        jwt(AUTH_JWT_CUSTOMER) {
            realm = jwt.realm
            verifier(jwt.verifier)
            validate { credential ->
                val companyId = credential.payload.getClaim(JwtService.CLAIM_COMPANY_ID).asString()
                val customerId = credential.payload.getClaim(JwtService.CLAIM_CUSTOMER_ID).asString()
                if (!companyId.isNullOrBlank() && !customerId.isNullOrBlank()) {
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
