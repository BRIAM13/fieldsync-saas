package com.corporacionronceros.fieldsync.data.sync

/**
 * Abstracción para encolar la sincronización en segundo plano. El ViewModel depende de
 * esta interfaz; la implementación real usa WorkManager. En tests se sustituye por un fake.
 */
interface SyncScheduler {
    fun requestSync()
}
