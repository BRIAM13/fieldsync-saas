package com.corporacionronceros.fieldsync.data.remote

/**
 * Configuración del endpoint del backend.
 *
 * Apunta al backend en producción (Render + Aiven) — funciona desde cualquier dispositivo
 * o emulador sin configuración adicional.
 *
 * Para desarrollo local contra un backend en tu máquina (`gradle run` en `backend/`):
 * - **Dispositivo físico por USB**: `http://127.0.0.1:8080` + `adb reverse tcp:8080 tcp:8080`.
 * - **Emulador**: `http://10.0.2.2:8080` (su alias del localhost del host).
 * - **Dispositivo físico sin cable**: la IP LAN de tu PC, p. ej. `http://192.168.1.x:8080`.
 */
object ApiConfig {
    const val BASE_URL = "https://fieldsync-backend-cipm.onrender.com"
}
