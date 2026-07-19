package com.corporacionronceros.fieldsync.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.corporacionronceros.fieldsync.domain.model.WorkOrderStatus
import com.corporacionronceros.fieldsync.domain.usecase.ObserveWorkOrderUseCase
import com.corporacionronceros.fieldsync.domain.usecase.UpdateWorkOrderStatusUseCase
import com.corporacionronceros.fieldsync.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel de la pantalla de detalle. Lee el id de la orden del SavedStateHandle
 * (argumento de navegación) y observa esa orden reactivamente desde Room.
 */
@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeWorkOrder: ObserveWorkOrderUseCase,
    private val updateStatus: UpdateWorkOrderStatusUseCase
) : ViewModel() {

    private val orderId: String =
        checkNotNull(savedStateHandle[Screen.TaskDetail.ARG_ORDER_ID]) {
            "TaskDetail requiere el argumento ${Screen.TaskDetail.ARG_ORDER_ID}"
        }

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeWorkOrder(orderId).collect { order ->
                _uiState.update {
                    it.copy(isLoading = false, order = order, notFound = order == null)
                }
            }
        }
    }

    /** Cambia el estado (escritura local inmediata; se sincroniza en segundo plano). */
    fun onStatusChange(status: WorkOrderStatus) {
        viewModelScope.launch { updateStatus(orderId, status) }
    }
}
