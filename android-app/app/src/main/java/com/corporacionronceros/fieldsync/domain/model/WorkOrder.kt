package com.corporacionronceros.fieldsync.domain.model

/**
 * Modelo de dominio puro: una orden de trabajo asignada a un técnico de campo.
 *
 * No conoce Room ni la red — es la fuente de verdad conceptual del negocio.
 * Las capas de datos mapean sus entidades/DTOs hacia y desde este modelo.
 */
data class WorkOrder(
    val id: String,
    val title: String,
    val customerName: String,
    val address: String,
    val priority: Priority,
    val status: WorkOrderStatus,
    /** true cuando el cambio local aún no se ha propagado al backend (offline-first). */
    val pendingSync: Boolean = false,
    val scheduledAtEpochMs: Long
)

enum class Priority { LOW, MEDIUM, HIGH, URGENT }

enum class WorkOrderStatus { ASSIGNED, IN_PROGRESS, ON_HOLD, COMPLETED, CANCELLED }
