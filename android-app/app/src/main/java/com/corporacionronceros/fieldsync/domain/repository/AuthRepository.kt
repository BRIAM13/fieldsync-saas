package com.corporacionronceros.fieldsync.domain.repository

/** Contrato de autenticación (el dominio no conoce Ktor ni el TokenStore concreto). */
interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>
    fun isLoggedIn(): Boolean
    fun logout()
}
