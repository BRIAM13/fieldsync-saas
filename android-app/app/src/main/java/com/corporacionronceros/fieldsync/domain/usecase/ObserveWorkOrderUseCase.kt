package com.corporacionronceros.fieldsync.domain.usecase

import com.corporacionronceros.fieldsync.domain.model.WorkOrder
import com.corporacionronceros.fieldsync.domain.repository.WorkOrderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Observa una única orden de trabajo por id (pantalla de detalle). */
class ObserveWorkOrderUseCase @Inject constructor(
    private val repository: WorkOrderRepository
) {
    operator fun invoke(id: String): Flow<WorkOrder?> = repository.observeWorkOrder(id)
}
