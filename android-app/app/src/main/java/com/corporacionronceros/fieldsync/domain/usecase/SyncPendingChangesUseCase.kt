package com.corporacionronceros.fieldsync.domain.usecase

import com.corporacionronceros.fieldsync.domain.repository.WorkOrderRepository
import javax.inject.Inject

/**
 * Empuja al backend los cambios locales pendientes. Lo invoca tanto la UI
 * (botón "sincronizar") como el worker de WorkManager al recuperar conexión.
 *
 * @return número de órdenes sincronizadas, o el error envuelto en Result.
 */
class SyncPendingChangesUseCase @Inject constructor(
    private val repository: WorkOrderRepository
) {
    suspend operator fun invoke(): Result<Int> = repository.syncPendingChanges()
}
