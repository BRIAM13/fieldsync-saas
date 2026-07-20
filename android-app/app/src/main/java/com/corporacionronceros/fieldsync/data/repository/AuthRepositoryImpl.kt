package com.corporacionronceros.fieldsync.data.repository

import com.corporacionronceros.fieldsync.data.auth.TokenStore
import com.corporacionronceros.fieldsync.data.remote.AuthApi
import com.corporacionronceros.fieldsync.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Inicia sesión contra el backend y guarda el JWT en el TokenStore. */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val tokenStore: TokenStore
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val response = api.login(email, password)
        tokenStore.save(response.token)
    }

    override fun isLoggedIn(): Boolean = tokenStore.isLoggedIn

    override fun logout() = tokenStore.clear()
}
