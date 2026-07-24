package com.corporacionronceros.fieldsync.security

import java.util.concurrent.ConcurrentHashMap

/**
 * Limitador de tasa en memoria (ventana deslizante), sin dependencias externas. Suficiente para
 * una única instancia (Render free tier): se reinicia si el proceso se reinicia, lo cual es
 * aceptable para este propósito (frenar ráfagas de registro, no un WAF completo).
 */
object RateLimiter {
    private val hits = ConcurrentHashMap<String, MutableList<Long>>()

    /** true si la petición identificada por [key] cabe dentro de [max] intentos en [windowMs]. */
    fun allow(key: String, max: Int, windowMs: Long): Boolean {
        val now = System.currentTimeMillis()
        val timestamps = hits.computeIfAbsent(key) { mutableListOf() }
        synchronized(timestamps) {
            timestamps.removeAll { it < now - windowMs }
            if (timestamps.size >= max) return false
            timestamps.add(now)
            return true
        }
    }

    /**
     * Limpia todos los contadores. Se llama al arrancar `Application.module()`: en producción
     * es un no-op relevante (arranque limpio del proceso); en tests, como cada test invoca
     * `module()` de nuevo dentro de su propio `testApplication {}`, esto asegura que el estado
     * de este singleton no se filtre entre tests (evita que un test consuma sin querer el cupo
     * de otro).
     */
    fun reset() = hits.clear()
}
