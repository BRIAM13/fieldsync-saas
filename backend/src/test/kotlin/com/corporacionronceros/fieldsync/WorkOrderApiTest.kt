package com.corporacionronceros.fieldsync

import com.corporacionronceros.fieldsync.model.AuthResponse
import com.corporacionronceros.fieldsync.model.LoginRequest
import com.corporacionronceros.fieldsync.model.RegisterRequest
import com.corporacionronceros.fieldsync.model.StatusUpdateRequest
import com.corporacionronceros.fieldsync.model.WorkOrder
import com.corporacionronceros.fieldsync.model.WorkOrderStatus
import com.corporacionronceros.fieldsync.repository.SeedData
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests de integración con autenticación JWT y aislamiento multi-tenant. */
class WorkOrderApiTest {

    private fun ApplicationTestBuilder.jsonClient(): HttpClient =
        createClient { install(ContentNegotiation) { json() } }

    private suspend fun HttpClient.loginDemo(): String {
        val res: AuthResponse = post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(SeedData.DEMO_ADMIN_EMAIL, SeedData.DEMO_ADMIN_PASSWORD))
        }.body()
        return res.token
    }

    @Test
    fun `health is public`() = testApplication {
        application { module() }
        assertEquals(HttpStatusCode.OK, client.get("/health").status)
    }

    @Test
    fun `api requires a token`() = testApplication {
        application { module() }
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/work-orders").status)
    }

    @Test
    fun `demo admin can log in and list its orders`() = testApplication {
        application { module() }
        val client = jsonClient()
        val token = client.loginDemo()

        val orders: List<WorkOrder> = client.get("/api/work-orders") { bearerAuth(token) }.body()
        assertTrue(orders.any { it.id == "WO-1042" })
    }

    @Test
    fun `patch status works with a token`() = testApplication {
        application { module() }
        val client = jsonClient()
        val token = client.loginDemo()

        val updated: WorkOrder = client.patch("/api/work-orders/WO-1042/status") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(StatusUpdateRequest(WorkOrderStatus.IN_PROGRESS))
        }.body()
        assertEquals(WorkOrderStatus.IN_PROGRESS, updated.status)
    }

    @Test
    fun `a new tenant cannot see another company's orders`() = testApplication {
        application { module() }
        val client = jsonClient()

        // Registrar una empresa nueva → su propio token, sus propios datos (vacíos)
        val signup: AuthResponse = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("Otra Empresa SAC", "Dueño", "owner@otra.dev", "secret123"))
        }.body()

        val theirOrders: List<WorkOrder> =
            client.get("/api/work-orders") { bearerAuth(signup.token) }.body()

        // El tenant nuevo NO ve las órdenes sembradas de la empresa demo.
        assertTrue(theirOrders.none { it.id == "WO-1042" })
        assertTrue(theirOrders.isEmpty())
    }
}
