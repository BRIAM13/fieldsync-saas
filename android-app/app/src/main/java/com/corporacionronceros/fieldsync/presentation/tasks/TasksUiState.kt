package com.corporacionronceros.fieldsync.presentation.tasks

import com.corporacionronceros.fieldsync.domain.model.WorkOrder

/** Estado inmutable de la pantalla de tareas (patrón unidireccional / UDF). */
data class TasksUiState(
    val isLoading: Boolean = false,
    val orders: List<WorkOrder> = emptyList(),
    val pendingSyncCount: Int = 0,
    val errorMessage: String? = null
)
