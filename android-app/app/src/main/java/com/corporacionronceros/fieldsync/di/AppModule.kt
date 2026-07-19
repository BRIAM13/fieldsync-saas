package com.corporacionronceros.fieldsync.di

import android.content.Context
import androidx.room.Room
import com.corporacionronceros.fieldsync.data.local.room.FieldSyncDatabase
import com.corporacionronceros.fieldsync.data.local.room.WorkOrderDao
import com.corporacionronceros.fieldsync.data.remote.WorkOrderApi
import com.corporacionronceros.fieldsync.data.repository.WorkOrderRepositoryImpl
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
    fun provideWorkOrderApi(): WorkOrderApi = WorkOrderApi()
}

/** Enlaza la interfaz del dominio con su implementación de datos (inversión de dependencias). */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWorkOrderRepository(impl: WorkOrderRepositoryImpl): WorkOrderRepository
}
