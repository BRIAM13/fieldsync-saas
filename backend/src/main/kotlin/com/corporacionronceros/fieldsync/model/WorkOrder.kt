package com.corporacionronceros.fieldsync.model

import kotlinx.serialization.Serializable

/** Contrato JSON de una orden de trabajo. Refleja el modelo de dominio de la app Android. */
@Serializable
data class WorkOrder(
    val id: String,
    val title: String,
    val customerName: String,
    val address: String,
    val priority: Priority,
    val status: WorkOrderStatus,
    val scheduledAtEpochMs: Long
)

@Serializable
enum class Priority { LOW, MEDIUM, HIGH, URGENT }

@Serializable
enum class WorkOrderStatus { ASSIGNED, IN_PROGRESS, ON_HOLD, COMPLETED, CANCELLED }

/** Cuerpo de PATCH /api/work-orders/{id}/status */
@Serializable
data class StatusUpdateRequest(val status: WorkOrderStatus)

/** Un cambio local pendiente que el cliente offline-first empuja al reconectar. */
@Serializable
data class PendingChange(val id: String, val status: WorkOrderStatus)

/** Cuerpo de POST /api/sync */
@Serializable
data class SyncRequest(val changes: List<PendingChange>)

/** Respuesta de POST /api/sync */
@Serializable
data class SyncResponse(val synced: Int, val rejected: List<String> = emptyList())

/** Error uniforme de la API. */
@Serializable
data class ApiError(val message: String)
