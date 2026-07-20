package com.corporacionronceros.fieldsync.routes

import com.corporacionronceros.fieldsync.tracking.TrackingService
import io.ktor.server.routing.Route
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket

/**
 * WebSocket de seguimiento en tiempo real: /ws/tracking/{orderId}
 * Empuja actualizaciones de posición (lat/lng + ETA) hasta que el técnico llega.
 * Es lo que consume la app cliente en React Native.
 */
fun Route.trackingRoutes(trackingService: TrackingService) {
    webSocket("/ws/tracking/{orderId}") {
        val orderId = call.parameters["orderId"] ?: return@webSocket
        trackingService.trackTechnician(orderId).collect { location ->
            sendSerialized(location)
        }
    }
}
