package com.corporacionronceros.fieldsync.data.auth

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarda el JWT de la sesión en memoria. Para producción se persistiría cifrado
 * (DataStore + EncryptedSharedPreferences); aquí se mantiene simple a propósito.
 */
@Singleton
class TokenStore @Inject constructor() {

    @Volatile
    var token: String? = null
        private set

    fun save(value: String) { token = value }

    fun clear() { token = null }

    val isLoggedIn: Boolean get() = token != null
}
