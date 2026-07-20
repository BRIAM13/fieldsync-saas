package com.corporacionronceros.fieldsync.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "fieldsync_auth")

/**
 * Guarda el access token + refresh token de forma **persistente** con Jetpack DataStore,
 * de modo que la sesión sobrevive a reinicios de la app. Mantiene además una copia en memoria
 * para lecturas síncronas (nav gate, cabecera de peticiones).
 */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ACCESS = stringPreferencesKey("access_token")
        val REFRESH = stringPreferencesKey("refresh_token")
    }

    @Volatile var accessToken: String? = null
        private set

    @Volatile var refreshToken: String? = null
        private set

    init {
        // Hidrata la caché desde disco al arrancar (una sola lectura bloqueante).
        runBlocking {
            val prefs = context.dataStore.data.first()
            accessToken = prefs[Keys.ACCESS]
            refreshToken = prefs[Keys.REFRESH]
        }
    }

    val isLoggedIn: Boolean get() = accessToken != null

    suspend fun save(access: String, refresh: String) {
        accessToken = access
        refreshToken = refresh
        context.dataStore.edit {
            it[Keys.ACCESS] = access
            it[Keys.REFRESH] = refresh
        }
    }

    suspend fun clear() {
        accessToken = null
        refreshToken = null
        context.dataStore.edit { it.clear() }
    }
}
