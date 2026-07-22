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
 * Guarda el access token + refresh token, y los datos de sesión (nombre, email, empresa)
 * usados por el drawer de navegación, de forma **persistente** con Jetpack DataStore — la
 * sesión sobrevive a reinicios de la app. Mantiene además una copia en memoria para lecturas
 * síncronas (nav gate, cabecera de peticiones, drawer).
 */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ACCESS = stringPreferencesKey("access_token")
        val REFRESH = stringPreferencesKey("refresh_token")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val COMPANY_NAME = stringPreferencesKey("company_name")
    }

    @Volatile var accessToken: String? = null
        private set

    @Volatile var refreshToken: String? = null
        private set

    @Volatile var userName: String? = null
        private set

    @Volatile var userEmail: String? = null
        private set

    @Volatile var companyName: String? = null
        private set

    init {
        // Hidrata la caché desde disco al arrancar (una sola lectura bloqueante).
        runBlocking {
            val prefs = context.dataStore.data.first()
            accessToken = prefs[Keys.ACCESS]
            refreshToken = prefs[Keys.REFRESH]
            userName = prefs[Keys.USER_NAME]
            userEmail = prefs[Keys.USER_EMAIL]
            companyName = prefs[Keys.COMPANY_NAME]
        }
    }

    val isLoggedIn: Boolean get() = accessToken != null

    /** Login completo: tokens + datos de sesión para el drawer. */
    suspend fun saveSession(
        access: String,
        refresh: String,
        userName: String,
        userEmail: String,
        companyName: String
    ) {
        this.accessToken = access
        this.refreshToken = refresh
        this.userName = userName
        this.userEmail = userEmail
        this.companyName = companyName
        context.dataStore.edit {
            it[Keys.ACCESS] = access
            it[Keys.REFRESH] = refresh
            it[Keys.USER_NAME] = userName
            it[Keys.USER_EMAIL] = userEmail
            it[Keys.COMPANY_NAME] = companyName
        }
    }

    /** Solo renueva los tokens (usado por el refresh automático) — no toca los datos de sesión. */
    suspend fun saveTokens(access: String, refresh: String) {
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
        userName = null
        userEmail = null
        companyName = null
        context.dataStore.edit { it.clear() }
    }
}
