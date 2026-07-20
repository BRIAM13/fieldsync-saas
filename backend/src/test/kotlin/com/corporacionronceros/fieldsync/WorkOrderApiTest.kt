package com.corporacionronceros.fieldsync

import com.corporacionronceros.fieldsync.model.AssignmentRequest
import com.corporacionronceros.fieldsync.model.StatusUpdateRequest
import com.corporacionronceros.fieldsync.model.Technician
import com.corporacionronceros.fieldsync.model.WorkOrder
import com.corporacionronceros.fieldsync.model.WorkOrderStatus
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests de integración de la API con el test-host de Ktor (sin levantar un puerto real). */
class WorkOrderApiTest {

    @Test
    fun `health responds ok`() = testApplication {
        application { module() }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `lists seeded work orders`() = testApplication {
        application { module() }
        val client = createClient { install(ContentNegotiation) { json() } }

        val orders: List<WorkOrder> = client.get("/api/work-orders").body()
        assertTrue(orders.isNotEmpty())
        assertTrue(orders.any { it.id == "WO-1042" })
    }

    @Test
    fun `patch updates order status`() = testApplication {
        application { module() }
        val client = createClient { install(ContentNegotiation) { json() } }

        val updated: WorkOrder = client.patch("/api/work-orders/WO-1042/status") {
            contentType(ContentType.Application.Json)
            setBody(StatusUpdateRequest(WorkOrderStatus.IN_PROGRESS))
        }.body()

        assertEquals(WorkOrderStatus.IN_PROGRESS, updated.status)
    }

    @Test
    fun `lists technicians`() = testApplication {
        application { module() }
        val client = createClient { install(ContentNegotiation) { json() } }

        val techs: List<Technician> = client.get("/api/technicians").body()
        assertTrue(techs.any { it.id == "T-01" })
    }

    @Test
    fun `assigning an order sets technician and status`() = testApplication {
        application { module() }
        val client = createClient { install(ContentNegotiation) { json() } }

        val updated: WorkOrder = client.patch("/api/work-orders/WO-1043/assignment") {
            contentType(ContentType.Application.Json)
            setBody(AssignmentRequest("T-02"))
        }.body()

        assertEquals("T-02", updated.assignedTechnicianId)
        assertEquals(WorkOrderStatus.ASSIGNED, updated.status)
    }
}
