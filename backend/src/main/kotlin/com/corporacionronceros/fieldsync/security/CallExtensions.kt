package com.corporacionronceros.fieldsync.security

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal

/** Tenant (empresa) del portador del token — la fuente de verdad para aislar datos. */
fun ApplicationCall.companyId(): String =
    principal<JWTPrincipal>()!!.payload.getClaim(JwtService.CLAIM_COMPANY_ID).asString()

/** Id del usuario autenticado. */
fun ApplicationCall.userId(): String =
    principal<JWTPrincipal>()!!.payload.getClaim(JwtService.CLAIM_USER_ID).asString()
