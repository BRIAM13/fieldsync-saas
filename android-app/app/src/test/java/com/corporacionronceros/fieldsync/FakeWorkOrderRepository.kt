package com.corporacionronceros.fieldsync

import com.corporacionronceros.fieldsync.domain.model.Priority
import com.corporacionronceros.fieldsync.domain.model.WorkOrder
import com.corporacionronceros.fieldsync.domain.model.WorkOrderStatus
import com.corporacionronceros.fieldsync.domain.repository.WorkOrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Repositorio falso, en memoria, para tests de dominio y presentación (sin Room ni red). */
class FakeWorkOrderRepository(
    initial: List<WorkOrder> = emptyList()
) : WorkOrderRepository {

    private val orders = MutableStateFlow(initial)
    var syncResult: Result<Int> = Result.success(0)
    var refreshResult: Result<Unit> = Result.success(Unit)

    override fun observeWorkOrders(): Flow<List<WorkOrder>> = orders

    override fun observeWorkOrder(id: String): Flow<WorkOrder?> =
        orders.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun refreshFromRemote(): Result<Unit> = refreshResult

    override suspend fun updateStatus(id: String, status: WorkOrderStatus) {
        orders.value = orders.value.map {
            if (it.id == id) it.copy(status = status, pendingSync = true) else it
        }
    }

    override suspend fun syncPendingChanges(): Result<Int> = syncResult

    companion object {
        fun order(
            id: String,
            priority: Priority = Priority.MEDIUM,
            status: WorkOrderStatus = WorkOrderStatus.ASSIGNED,
            pendingSync: Boolean = false,
            scheduledAt: Long = 0L
        ) = WorkOrder(
            id = id, title = "Orden $id", customerName = "Cliente", address = "Dir",
            priority = priority, status = status, pendingSync = pendingSync,
            scheduledAtEpochMs = scheduledAt
        )
    }
}
