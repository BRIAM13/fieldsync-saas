package com.corporacionronceros.fieldsync.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.corporacionronceros.fieldsync.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val loggedIn: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            loginUseCase(email, password)
                .onSuccess { _uiState.update { it.copy(isLoading = false, loggedIn = true) } }
                .onFailure { e ->
                    val message = when {
                        e is ClientRequestException && e.response.status.value == 401 ->
                            "Credenciales inválidas"
                        e is IOException ->
                            "No se pudo conectar al servidor (${e.message}). ¿Está corriendo el backend?"
                        else -> "Error inesperado: ${e.message}"
                    }
                    _uiState.update { it.copy(isLoading = false, error = message) }
                }
        }
    }
}
