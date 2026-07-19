package com.corporacionronceros.fieldsync

import app.cash.turbine.test
import com.corporacionronceros.fieldsync.FakeWorkOrderRepository.Companion.order
import com.corporacionronceros.fieldsync.data.connectivity.NetworkMonitor
import com.corporacionronceros.fieldsync.data.sync.SyncScheduler
import com.corporacionronceros.fieldsync.domain.model.Priority
import com.corporacionronceros.fieldsync.domain.usecase.ObserveWorkOrdersUseCase
import com.corporacionronceros.fieldsync.presentation.tasks.TasksViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Fake del scheduler: cuenta cuántas veces se solicitó una sincronización. */
private class FakeSyncScheduler : SyncScheduler {
    var syncRequests = 0
    override fun requestSync() { syncRequests++ }
}

/** Fake del monitor de red: emite el estado de conexión que el test controle. */
private class FakeNetworkMonitor(initial: Boolean) : NetworkMonitor {
    val flow = MutableStateFlow(initial)
    override val isOnline: Flow<Boolean> = flow
}

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private fun buildViewModel(
        repo: FakeWorkOrderRepository,
        scheduler: SyncScheduler = FakeSyncScheduler(),
        monitor: NetworkMonitor = FakeNetworkMonitor(initial = true)
    ) = TasksViewModel(ObserveWorkOrdersUseCase(repo), repo, scheduler, monitor)

    @Test
    fun `uiState exposes sorted orders and pending sync count`() = runTest {
        val repo = FakeWorkOrderRepository(
            listOf(
                order("a", priority = Priority.LOW, pendingSync = true),
                order("b", priority = Priority.URGENT)
            )
        )
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            val state = awaitItem()
            // urgent ordena primero; una orden queda pendiente de sincronizar
            assertEquals(listOf("b", "a"), state.orders.map { it.id })
            assertEquals(1, state.pendingSyncCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `regaining connectivity schedules a background sync`() = runTest {
        val repo = FakeWorkOrderRepository()
        val scheduler = FakeSyncScheduler()
        val monitor = FakeNetworkMonitor(initial = true)
        buildViewModel(repo, scheduler, monitor)

        // El estado inicial online se descarta (drop 1). Perder y recuperar la red dispara sync.
        monitor.flow.value = false
        monitor.flow.value = true

        assertEquals(1, scheduler.syncRequests)
    }
}
