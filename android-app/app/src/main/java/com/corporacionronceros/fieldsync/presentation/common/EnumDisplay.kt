package com.corporacionronceros.fieldsync.presentation.common

import androidx.compose.ui.graphics.Color
import com.corporacionronceros.fieldsync.domain.model.Priority
import com.corporacionronceros.fieldsync.domain.model.WorkOrderStatus

/**
 * Traducción y color semántico de los enums de dominio para la UI.
 *
 * El dominio y el backend siguen en inglés (es el contrato de la API); esta capa de
 * presentación es la única responsable de mostrarlos en español.
 */
val Priority.displayName: String
    get() = when (this) {
        Priority.LOW -> "Baja"
        Priority.MEDIUM -> "Media"
        Priority.HIGH -> "Alta"
        Priority.URGENT -> "Urgente"
    }

val Priority.color: Color
    get() = when (this) {
        Priority.LOW -> Color(0xFF16A34A)
        Priority.MEDIUM -> Color(0xFFCA8A04)
        Priority.HIGH -> Color(0xFFEA580C)
        Priority.URGENT -> Color(0xFFDC2626)
    }

val WorkOrderStatus.displayName: String
    get() = when (this) {
        WorkOrderStatus.ASSIGNED -> "Asignada"
        WorkOrderStatus.IN_PROGRESS -> "En progreso"
        WorkOrderStatus.ON_HOLD -> "En espera"
        WorkOrderStatus.COMPLETED -> "Completada"
        WorkOrderStatus.CANCELLED -> "Cancelada"
    }

val WorkOrderStatus.color: Color
    get() = when (this) {
        WorkOrderStatus.ASSIGNED -> Color(0xFF2563EB)
        WorkOrderStatus.IN_PROGRESS -> Color(0xFFCA8A04)
        WorkOrderStatus.ON_HOLD -> Color(0xFF9333EA)
        WorkOrderStatus.COMPLETED -> Color(0xFF16A34A)
        WorkOrderStatus.CANCELLED -> Color(0xFFDC2626)
    }
