package com.corporacionronceros.fieldsync.di

import android.content.Context
import androidx.room.Room
import com.corporacionronceros.fieldsync.data.auth.TokenStore
import com.corporacionronceros.fieldsync.data.connectivity.NetworkMonitor
import com.corporacionronceros.fieldsync.data.connectivity.NetworkMonitorImpl
import com.corporacionronceros.fieldsync.data.local.room.FieldSyncDatabase
import com.corporacionronceros.fieldsync.data.local.room.WorkOrderDao
import com.corporacionronceros.fieldsync.data.remote.ApiConfig
import com.corporacionronceros.fieldsync.data.remote.AuthResponseDto
import com.corporacionronceros.fieldsync.data.remote.RefreshRequestDto
import com.corporacionronceros.fieldsync.data.repository.AuthRepositoryImpl
import com.corporacionronceros.fieldsync.data.repository.WorkOrderRepositoryImpl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import com.corporacionronceros.fieldsync.data.sync.SyncScheduler
import com.corporacionronceros.fieldsync.data.sync.SyncSchedulerImpl
import com.corporacionronceros.fieldsync.domain.repository.AuthRepository
import com.corporacionronceros.fieldsync.domain.repository.WorkOrderRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provee las dependencias concretas (Room, API) al grafo de Hilt. */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): FieldSyncDatabase =
        Room.databaseBuilder(ctx, FieldSyncDatabase::class.java, FieldSyncDatabase.NAME).build()

    @Provides
    fun provideWorkOrderDao(db: FieldSyncDatabase): WorkOrderDao = db.workOrderDao()

    @Provides
    @Singleton
    fun provideHttpClient(tokenStore: TokenStore): HttpClient = HttpClient(OkHttp) {
        // Un 4xx/5xx lanza excepción (ClientRequestException/ServerResponseException) en vez
        // de devolver el body de error silenciosamente — permite distinguir 401 de un fallo de red.
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        // Auth Bearer: adjunta el access token y, ante un 401, llama a /auth/refresh
        // con el refresh token y reintenta — todo transparente para el resto de la app.
        install(Auth) {
            bearer {
                loadTokens {
                    val access = tokenStore.accessToken
                    val refresh = tokenStore.refreshToken
                    if (access != null && refresh != null) BearerTokens(access, refresh) else null
                }
                refreshTokens {
                    val refresh = tokenStore.refreshToken ?: return@refreshTokens null
                    val refreshed = runCatching {
                        client.post("${ApiConfig.BASE_URL}/auth/refresh") {
                            markAsRefreshTokenRequest()
                            contentType(ContentType.Application.Json)
                            setBody(RefreshRequestDto(refresh))
                        }.body<AuthResponseDto>()
                    }.getOrNull() ?: return@refreshTokens null
                    tokenStore.saveTokens(refreshed.token, refreshed.refreshToken)
                    BearerTokens(refreshed.token, refreshed.refreshToken)
                }
            }
        }
    }
}

/** Enlaza la interfaz del dominio con su implementación de datos (inversión de dependencias). */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWorkOrderRepository(impl: WorkOrderRepositoryImpl): WorkOrderRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(impl: NetworkMonitorImpl): NetworkMonitor

    @Binds
    @Singleton
    abstract fun bindSyncScheduler(impl: SyncSchedulerImpl): SyncScheduler
}
