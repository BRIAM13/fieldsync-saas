package com.corporacionronceros.fieldsync.routes

import com.corporacionronceros.fieldsync.security.JwtService
import com.corporacionronceros.fieldsync.tracking.TrackingService
import io.ktor.server.routing.Route
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close

/**
 * WebSocket de seguimiento en tiempo real: /ws/tracking/{orderId}?token=JWT
 *
 * Como los WebSocket del navegador / React Native no pueden enviar el header Authorization,
 * el token viaja como query param y se valida a mano con el verificador JWT antes de emitir.
 */
fun Route.trackingRoutes(trackingService: TrackingService, jwtService: JwtService) {
    webSocket("/ws/tracking/{orderId}") {
        val orderId = call.parameters["orderId"]
        if (orderId == null) {
            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "orderId requerido"))
            return@webSocket
        }

        val token = call.request.queryParameters["token"]
        val authorized = token != null && runCatching { jwtService.verifier.verify(token) }.isSuccess
        if (!authorized) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "token inválido o ausente"))
            return@webSocket
        }

        trackingService.trackTechnician(orderId).collect { location ->
            sendSerialized(location)
        }
    }
}
