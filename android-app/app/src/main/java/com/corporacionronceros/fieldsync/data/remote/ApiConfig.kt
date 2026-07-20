package com.corporacionronceros.fieldsync.data.remote

/**
 * Configuración del endpoint del backend.
 *
 * `10.0.2.2` es el alias del `localhost` de la máquina anfitriona **desde el emulador de Android**.
 * En un dispositivo físico se cambia por la IP LAN del servidor (p. ej. http://192.168.1.x:8080).
 */
object ApiConfig {
    const val BASE_URL = "http://10.0.2.2:8080"
}
