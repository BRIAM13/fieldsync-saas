package com.corporacionronceros.fieldsync.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.corporacionronceros.fieldsync.model.User
import java.util.Date

/**
 * Emite y verifica JWT (HMAC256). El token lleva el `companyId` — la pieza clave del
 * multi-tenancy: cada petición se resuelve al tenant del portador, sin que el cliente
 * pueda pedir datos de otra empresa.
 */
class JwtService(
    secret: String,
    val issuer: String,
    val audience: String,
    private val validityMs: Long
) {
    val realm = "fieldsync"
    private val algorithm = Algorithm.HMAC256(secret)

    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    fun generateToken(user: User): String = JWT.create()
        .withIssuer(issuer)
        .withAudience(audience)
        .withClaim(CLAIM_USER_ID, user.id)
        .withClaim(CLAIM_COMPANY_ID, user.companyId)
        .withClaim(CLAIM_ROLE, user.role.name)
        .withExpiresAt(Date(System.currentTimeMillis() + validityMs))
        .sign(algorithm)

    companion object {
        const val CLAIM_USER_ID = "userId"
        const val CLAIM_COMPANY_ID = "companyId"
        const val CLAIM_ROLE = "role"

        /** Config desde entorno con defaults de desarrollo (cámbialos en producción). */
        fun fromEnv(): JwtService = JwtService(
            secret = System.getenv("JWT_SECRET") ?: "dev-secret-change-me-please-min-32-bytes!",
            issuer = System.getenv("JWT_ISSUER") ?: "fieldsync",
            audience = System.getenv("JWT_AUDIENCE") ?: "fieldsync-clients",
            validityMs = System.getenv("JWT_VALIDITY_MS")?.toLongOrNull() ?: (7L * 24 * 60 * 60 * 1000)
        )
    }
}
