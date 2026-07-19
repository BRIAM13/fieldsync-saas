package com.corporacionronceros.fieldsync.presentation.detail

import com.corporacionronceros.fieldsync.domain.model.WorkOrder

/** Estado inmutable de la pantalla de detalle de una orden. */
data class TaskDetailUiState(
    val isLoading: Boolean = true,
    val order: WorkOrder? = null,
    val notFound: Boolean = false
)
