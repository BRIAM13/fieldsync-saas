package com.corporacionronceros.fieldsync.model

import kotlinx.serialization.Serializable

/** Actualización de posición emitida por el WebSocket de seguimiento en tiempo real. */
@Serializable
data class TechnicianLocation(
    val orderId: String,
    val technicianId: String,
    val technicianName: String,
    val lat: Double,
    val lng: Double,
    val etaMinutes: Int,
    val arrived: Boolean = false
)
