package com.corporacionronceros.fieldsync.tracking

import com.corporacionronceros.fieldsync.model.TechnicianLocation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Simula el stream de posición de un técnico acercándose al cliente. En producción
 * las coordenadas vendrían del GPS del dispositivo del técnico (publicadas al backend);
 * aquí interpolamos una ruta y decrementamos el ETA para alimentar la app cliente.
 */
class TrackingService {

    fun trackTechnician(orderId: String): Flow<TechnicianLocation> = flow {
        val startLat = -12.090; val startLng = -77.050
        val endLat = -12.046; val endLng = -77.043
        val totalEta = 18
        val steps = totalEta // una emisión por minuto simulado

        for (i in 0..steps) {
            val t = i.toDouble() / steps
            val eta = totalEta - i
            emit(
                TechnicianLocation(
                    orderId = orderId,
                    technicianId = "T-01",
                    technicianName = "Carlos Ramírez",
                    lat = startLat + (endLat - startLat) * t,
                    lng = startLng + (endLng - startLng) * t,
                    etaMinutes = eta,
                    arrived = eta == 0
                )
            )
            if (i < steps) delay(2000) // 2s por tick (demo); en real, cadencia del GPS
        }
    }
}
