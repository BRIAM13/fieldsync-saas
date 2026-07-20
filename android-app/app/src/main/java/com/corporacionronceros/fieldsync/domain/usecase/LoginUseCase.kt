package com.corporacionronceros.fieldsync.domain.usecase

import com.corporacionronceros.fieldsync.domain.repository.AuthRepository
import javax.inject.Inject

/** Inicia sesión con email + contraseña. */
class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> =
        repository.login(email, password)
}
