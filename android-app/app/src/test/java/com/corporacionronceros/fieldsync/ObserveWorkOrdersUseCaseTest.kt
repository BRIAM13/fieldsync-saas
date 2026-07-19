package com.corporacionronceros.fieldsync

import app.cash.turbine.test
import com.corporacionronceros.fieldsync.domain.model.Priority
import com.corporacionronceros.fieldsync.domain.model.WorkOrder
import com.corporacionronceros.fieldsync.domain.model.WorkOrderStatus
import com.corporacionronceros.fieldsync.domain.repository.WorkOrderRepository
import com.corporacionronceros.fieldsync.domain.usecase.ObserveWorkOrdersUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Gracias a Clean Architecture podemos testear la regla de negocio (ordenamiento)
 * con un repositorio falso, sin Android ni Room. Turbine verifica el Flow.
 */
class ObserveWorkOrdersUseCaseTest {

    private fun order(id: String, priority: Priority, at: Long) = WorkOrder(
        id = id, title = "t", customerName = "c", address = "a",
        priority = priority, status = WorkOrderStatus.ASSIGNED, scheduledAtEpochMs = at
    )

    private val fakeRepo = object : WorkOrderRepository {
        override fun observeWorkOrders(): Flow<List<WorkOrder>> = flowOf(
            listOf(
                order("low", Priority.LOW, 100),
                order("urgent", Priority.URGENT, 500),
                order("high", Priority.HIGH, 300)
            )
        )
        override suspend fun refreshFromRemote() = Result.success(Unit)
        override suspend fun updateStatus(id: String, status: WorkOrderStatus) = Unit
        override suspend fun syncPendingChanges() = Result.success(0)
    }

    @Test
    fun `orders are sorted by priority descending`() = runTest {
        ObserveWorkOrdersUseCase(fakeRepo)().test {
            val result = awaitItem()
            assertEquals(listOf("urgent", "high", "low"), result.map { it.id })
            awaitComplete()
        }
    }
}
