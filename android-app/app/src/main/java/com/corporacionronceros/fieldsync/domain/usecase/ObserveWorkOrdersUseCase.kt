package com.corporacionronceros.fieldsync.domain.usecase

import com.corporacionronceros.fieldsync.domain.model.Priority
import com.corporacionronceros.fieldsync.domain.model.WorkOrder
import com.corporacionronceros.fieldsync.domain.repository.WorkOrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Regla de negocio: el técnico ve primero lo urgente y lo más próximo en agenda.
 * Ordena por prioridad (desc) y luego por hora programada (asc).
 */
class ObserveWorkOrdersUseCase @Inject constructor(
    private val repository: WorkOrderRepository
) {
    operator fun invoke(): Flow<List<WorkOrder>> =
        repository.observeWorkOrders().map { orders ->
            orders.sortedWith(
                compareByDescending<WorkOrder> { it.priority.weight() }
                    .thenBy { it.scheduledAtEpochMs }
            )
        }

    private fun Priority.weight(): Int = when (this) {
        Priority.URGENT -> 3
        Priority.HIGH -> 2
        Priority.MEDIUM -> 1
        Priority.LOW -> 0
    }
}
