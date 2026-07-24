package com.corporacionronceros.fieldsync.security

import com.corporacionronceros.fieldsync.model.ApiError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import io.ktor.server.response.respond

private const val MAX_REGISTRATIONS_PER_WINDOW = 5
private const val WINDOW_MS = 15L * 60 * 1000 // 15 minutos

/**
 * Antifraude compartido por los dos flujos de registro (staff y cliente): honeypot, límite de
 * intentos por IP, y bloqueo de correos desechables. Responde y devuelve `true` si rechazó la
 * petición (el caller debe hacer `return@post` de inmediato); `false` si puede continuar.
 */
suspend fun ApplicationCall.rejectIfAbusiveRegistration(
    email: String,
    honeypot: String?,
    rateLimitScope: String
): Boolean {
    // Honeypot: un campo oculto que ningún usuario real llena. Mismo mensaje que una validación
    // normal — no delatar el mecanismo de detección a quien lo dispara.
    if (!honeypot.isNullOrBlank()) {
        respond(HttpStatusCode.BadRequest, ApiError("No se pudo completar el registro"))
        return true
    }

    val ip = request.origin.remoteHost
    if (!RateLimiter.allow("register:$rateLimitScope:$ip", MAX_REGISTRATIONS_PER_WINDOW, WINDOW_MS)) {
        respond(HttpStatusCode.TooManyRequests, ApiError("Demasiados intentos, intenta más tarde"))
        return true
    }

    if (DisposableEmailBlocklist.isDisposable(email)) {
        respond(HttpStatusCode.BadRequest, ApiError("Usa un correo real, no uno temporal"))
        return true
    }

    return false
}
