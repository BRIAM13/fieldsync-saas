package com.corporacionronceros.fieldsync.data.remote

/**
 * Configuración del endpoint del backend.
 *
 * En **dispositivo físico por USB**: usa `127.0.0.1` + `adb reverse tcp:8080 tcp:8080`
 * (reenvía el puerto del teléfono al `localhost` de la PC — no depende de la red Wi-Fi).
 * En el **emulador**: cambia a `http://10.0.2.2:8080` (su alias del localhost del host).
 * En dispositivo físico sin cable: usa la IP LAN de la PC, p. ej. `http://192.168.1.x:8080`.
 */
object ApiConfig {
    const val BASE_URL = "http://127.0.0.1:8080"
}
