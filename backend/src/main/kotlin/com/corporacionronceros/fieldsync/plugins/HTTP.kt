package com.corporacionronceros.fieldsync.plugins

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders

/**
 * Resuelve `call.request.origin.remoteHost` desde el header `X-Forwarded-For` que pone el proxy
 * de Render, en vez de la IP interna del proxy (igual para toda petición). Debe instalarse antes
 * que cualquier plugin que dependa de la IP real del cliente (p. ej. el rate limiter del registro).
 */
fun Application.configureForwardedHeaders() {
    install(XForwardedHeaders)
}

/**
 * CORS para que el panel Angular (localhost:4200) y las apps móviles consuman la API.
 * En producción se restringiría `allowHost` a los orígenes reales.
 */
fun Application.configureCors() {
    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHost("localhost:4200")
        allowHost("localhost:8081") // Metro / Expo dev
        anyHost() // solo desarrollo — acotar en producción
    }
}
