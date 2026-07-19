package com.corporacionronceros.fieldsync.data.remote

import kotlinx.coroutines.delay

/** DTO tal como lo devolvería el backend REST. */
data class WorkOrderDto(
    val id: String,
    val title: String,
    val customer: String,
    val address: String,
    val priority: String,
    val status: String,
    val scheduledAt: Long
)

/**
 * API remota SIMULADA. En producción sería una interfaz Retrofit con `suspend fun`.
 * Aquí devolvemos datos de ejemplo tras una latencia falsa para poder ejecutar la app
 * sin backend y demostrar el flujo offline-first de punta a punta.
 */
class WorkOrderApi {

    suspend fun fetchWorkOrders(): List<WorkOrderDto> {
        delay(600) // simula latencia de red
        return sampleOrders
    }

    /** Devuelve true si el backend aceptó el cambio. */
    suspend fun pushStatus(id: String, status: String): Boolean {
        delay(300)
        return true
    }

    private val sampleOrders = listOf(
        WorkOrderDto("WO-1042", "Fuga en tubería principal", "Ferretería El Sol",
            "Av. Los Álamos 234", "URGENT", "ASSIGNED", System.currentTimeMillis() + 3_600_000),
        WorkOrderDto("WO-1043", "Instalación de tablero eléctrico", "Condominio Las Palmas",
            "Jr. Independencia 87", "HIGH", "ASSIGNED", System.currentTimeMillis() + 7_200_000),
        WorkOrderDto("WO-1044", "Mantenimiento de calentador", "Sra. Quispe",
            "Calle Lima 12", "MEDIUM", "ASSIGNED", System.currentTimeMillis() + 14_400_000),
    )
}
