package com.corporacionronceros.fieldsync.presentation.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.corporacionronceros.fieldsync.data.connectivity.NetworkMonitor
import com.corporacionronceros.fieldsync.data.sync.SyncScheduler
import com.corporacionronceros.fieldsync.domain.repository.WorkOrderRepository
import com.corporacionronceros.fieldsync.domain.usecase.ObserveWorkOrdersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel MVVM: no conoce a la vista. Expone un único StateFlow inmutable
 * y colecta el Flow del dominio con asincronía estructurada (viewModelScope).
 *
 * Además observa la conectividad: cuando la red vuelve, encola una sincronización
 * en segundo plano vía WorkManager (offline-first, sin intervención del usuario).
 */
@HiltViewModel
class TasksViewModel @Inject constructor(
    private val observeWorkOrders: ObserveWorkOrdersUseCase,
    private val repository: WorkOrderRepository,
    private val syncScheduler: SyncScheduler,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState(isLoading = true))
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    init {
        observeOrders()
        observeConnectivity()
        refresh()
    }

    private fun observeOrders() {
        viewModelScope.launch {
            observeWorkOrders().collect { orders ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        orders = orders,
                        pendingSyncCount = orders.count { o -> o.pendingSync }
                    )
                }
            }
        }
    }

    /** Al recuperar conexión (drop del estado inicial), dispara la sincronización diferida. */
    private fun observeConnectivity() {
        viewModelScope.launch {
            networkMonitor.isOnline
                .drop(1)
                .filter { online -> online }
                .collect { syncScheduler.requestSync() }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.refreshFromRemote()
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /** Sincronización manual: encola el trabajo en WorkManager (respeta la restricción de red). */
    fun syncNow() {
        syncScheduler.requestSync()
    }
}
