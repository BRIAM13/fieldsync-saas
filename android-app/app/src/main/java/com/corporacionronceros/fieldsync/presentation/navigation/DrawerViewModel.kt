package com.corporacionronceros.fieldsync.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.corporacionronceros.fieldsync.data.auth.TokenStore
import com.corporacionronceros.fieldsync.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Datos de sesión para el drawer + acción de cerrar sesión. */
@HiltViewModel
class DrawerViewModel @Inject constructor(
    private val tokenStore: TokenStore,
    private val authRepository: AuthRepository
) : ViewModel() {

    val userName: String get() = tokenStore.userName ?: "Usuario"
    val userEmail: String get() = tokenStore.userEmail ?: ""
    val companyName: String get() = tokenStore.companyName ?: "FieldSync"

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }
}
