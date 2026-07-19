package com.corporacionronceros.fieldsync

import app.cash.turbine.test
import com.corporacionronceros.fieldsync.FakeWorkOrderRepository.Companion.order
import com.corporacionronceros.fieldsync.domain.model.Priority
import com.corporacionronceros.fieldsync.domain.usecase.ObserveWorkOrdersUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Gracias a Clean Architecture podemos testear la regla de negocio (ordenamiento)
 * con un repositorio falso, sin Android ni Room. Turbine verifica el Flow.
 */
class ObserveWorkOrdersUseCaseTest {

    @Test
    fun `orders are sorted by priority descending then schedule`() = runTest {
        val repo = FakeWorkOrderRepository(
            listOf(
                order("low", priority = Priority.LOW, scheduledAt = 100),
                order("urgent", priority = Priority.URGENT, scheduledAt = 500),
                order("high", priority = Priority.HIGH, scheduledAt = 300)
            )
        )

        ObserveWorkOrdersUseCase(repo)().test {
            val result = awaitItem()
            assertEquals(listOf("urgent", "high", "low"), result.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }
}
