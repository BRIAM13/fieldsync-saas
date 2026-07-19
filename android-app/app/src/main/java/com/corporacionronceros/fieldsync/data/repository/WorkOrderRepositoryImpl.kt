package com.corporacionronceros.fieldsync.data.repository

import com.corporacionronceros.fieldsync.data.local.room.WorkOrderDao
import com.corporacionronceros.fieldsync.data.remote.WorkOrderApi
import com.corporacionronceros.fieldsync.domain.model.WorkOrder
import com.corporacionronceros.fieldsync.domain.model.WorkOrderStatus
import com.corporacionronceros.fieldsync.domain.repository.WorkOrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación offline-first:
 *  - La UI SIEMPRE lee desde Room (fuente de verdad local) vía Flow.
 *  - La red solo alimenta la base local y sincroniza cambios pendientes.
 *  - Los escritos son locales primero (marcados pendingSync) y se empujan luego.
 */
@Singleton
class WorkOrderRepositoryImpl @Inject constructor(
    private val dao: WorkOrderDao,
    private val api: WorkOrderApi
) : WorkOrderRepository {

    override fun observeWorkOrders(): Flow<List<WorkOrder>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun refreshFromRemote(): Result<Unit> = runCatching {
        val remote = api.fetchWorkOrders()
        dao.upsertAll(remote.map { it.toEntity() })
    }

    override suspend fun updateStatus(id: String, status: WorkOrderStatus) {
        // Escritura local inmediata → la UI se actualiza al instante aunque no haya red.
        dao.updateStatus(id, status.name)
    }

    override suspend fun syncPendingChanges(): Result<Int> = runCatching {
        val pending = dao.getPending()
        var synced = 0
        for (order in pending) {
            if (api.pushStatus(order.id, order.status)) {
                dao.markSynced(order.id)
                synced++
            }
        }
        synced
    }
}
