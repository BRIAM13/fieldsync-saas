package com.corporacionronceros.fieldsync.data.connectivity

import kotlinx.coroutines.flow.Flow

/**
 * Abstracción de conectividad. El ViewModel depende de esta interfaz, no de la
 * implementación concreta basada en ConnectivityManager — así se puede sustituir
 * por un fake en tests sin tocar Android.
 */
interface NetworkMonitor {
    /** true cuando hay al menos una red con acceso a Internet. */
    val isOnline: Flow<Boolean>
}
