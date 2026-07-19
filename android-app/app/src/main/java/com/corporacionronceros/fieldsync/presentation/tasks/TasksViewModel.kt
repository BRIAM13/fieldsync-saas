package com.corporacionronceros.fieldsync.presentation.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.corporacionronceros.fieldsync.domain.model.WorkOrderStatus
import com.corporacionronceros.fieldsync.domain.repository.WorkOrderRepository
import com.corporacionronceros.fieldsync.domain.usecase.ObserveWorkOrdersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel MVVM: no conoce a la vista. Expone un único StateFlow inmutable
 * y colecta el Flow del dominio con asincronía estructurada (viewModelScope).
 */
@HiltViewModel
class TasksViewModel @Inject constructor(
    private val observeWorkOrders: ObserveWorkOrdersUseCase,
    private val repository: WorkOrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState(isLoading = true))
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    init {
        observeOrders()
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

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.refreshFromRemote()
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onStatusChange(id: String, status: WorkOrderStatus) {
        viewModelScope.launch { repository.updateStatus(id, status) }
    }

    fun syncNow() {
        viewModelScope.launch { repository.syncPendingChanges() }
    }
}
