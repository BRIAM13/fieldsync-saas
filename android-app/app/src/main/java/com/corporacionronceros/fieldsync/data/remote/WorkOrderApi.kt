package com.corporacionronceros.fieldsync.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/** DTO tal como lo devuelve el backend Ktor (campos alineados con su JSON). */
@Serializable
data class WorkOrderDto(
    val id: String,
    val title: String,
    val customerName: String,
    val address: String,
    val priority: String,
    val status: String,
    val scheduledAtEpochMs: Long
)

@Serializable
private data class StatusUpdateRequest(val status: String)

/**
 * Cliente REST del backend FieldSync, escrito con **Ktor client** (Kotlin end-to-end).
 * Reemplaza al stub simulado: ahora habla de verdad con el servidor.
 */
@Singleton
class WorkOrderApi @Inject constructor(
    private val client: HttpClient
) {
    suspend fun fetchWorkOrders(): List<WorkOrderDto> =
        client.get("${ApiConfig.BASE_URL}/api/work-orders").body()

    /** Empuja un cambio de estado (PATCH). Devuelve true si el backend lo aceptó. */
    suspend fun pushStatus(id: String, status: String): Boolean {
        val response = client.patch("${ApiConfig.BASE_URL}/api/work-orders/$id/status") {
            contentType(ContentType.Application.Json)
            setBody(StatusUpdateRequest(status))
        }
        return response.status.isSuccess()
    }
}
