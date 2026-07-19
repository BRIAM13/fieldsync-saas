package com.corporacionronceros.fieldsync.domain.usecase

import com.corporacionronceros.fieldsync.domain.model.WorkOrderStatus
import com.corporacionronceros.fieldsync.domain.repository.WorkOrderRepository
import javax.inject.Inject

/**
 * Cambia el estado de una orden. Escritura local inmediata (offline-first):
 * el cambio queda marcado como pendiente y WorkManager lo sincroniza después.
 */
class UpdateWorkOrderStatusUseCase @Inject constructor(
    private val repository: WorkOrderRepository
) {
    suspend operator fun invoke(id: String, status: WorkOrderStatus) {
        repository.updateStatus(id, status)
    }
}
