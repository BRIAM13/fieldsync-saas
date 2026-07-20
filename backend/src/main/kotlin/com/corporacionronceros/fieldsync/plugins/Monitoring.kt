package com.corporacionronceros.fieldsync.plugins

import com.corporacionronceros.fieldsync.model.ApiError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respond
import org.slf4j.event.Level

/** Logging de peticiones + traducción de excepciones a una respuesta JSON uniforme. */
fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiError(cause.message ?: "Error interno del servidor")
            )
        }
    }
}
