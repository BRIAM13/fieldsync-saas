package com.corporacionronceros.fieldsync

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Punto de entrada de Hilt para toda la app.
 *
 * Implementa [Configuration.Provider] para entregar a WorkManager el [HiltWorkerFactory],
 * de modo que los workers (p. ej. SyncWorkOrdersWorker) reciban sus dependencias por Hilt.
 * Requiere desactivar el inicializador por defecto de WorkManager en el manifiesto.
 */
@HiltAndroidApp
class FieldSyncApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
