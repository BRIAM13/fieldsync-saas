package com.corporacionronceros.fieldsync.di

import android.content.Context
import androidx.room.Room
import com.corporacionronceros.fieldsync.data.auth.TokenStore
import com.corporacionronceros.fieldsync.data.connectivity.NetworkMonitor
import com.corporacionronceros.fieldsync.data.connectivity.NetworkMonitorImpl
import com.corporacionronceros.fieldsync.data.local.room.FieldSyncDatabase
import com.corporacionronceros.fieldsync.data.local.room.WorkOrderDao
import com.corporacionronceros.fieldsync.data.repository.AuthRepositoryImpl
import com.corporacionronceros.fieldsync.data.repository.WorkOrderRepositoryImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
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
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        // Adjunta el JWT en cada petición (se evalúa por request, token dinámico).
        install(DefaultRequest) {
            tokenStore.token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
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
