package com.corporacionronceros.fieldsync.domain.repository

import com.corporacionronceros.fieldsync.domain.model.WorkOrder
import com.corporacionronceros.fieldsync.domain.model.WorkOrderStatus
import kotlinx.coroutines.flow.Flow

/**
 * Contrato del repositorio definido en el dominio (inversión de dependencias).
 *
 * La capa de datos lo implementa; el dominio y la presentación dependen solo
 * de esta abstracción, nunca de Room ni de Retrofit.
 */
interface WorkOrderRepository {

    /** Stream reactivo respaldado por Room: la UI reacciona a cambios locales sin polling. */
    fun observeWorkOrders(): Flow<List<WorkOrder>>

    /** Descarga del backend y persiste localmente (offline-first). */
    suspend fun refreshFromRemote(): Result<Unit>

    /** Actualiza el estado localmente y lo marca como pendiente de sincronización. */
    suspend fun updateStatus(id: String, status: WorkOrderStatus)

    /** Envía al backend los cambios locales pendientes. */
    suspend fun syncPendingChanges(): Result<Int>
}
