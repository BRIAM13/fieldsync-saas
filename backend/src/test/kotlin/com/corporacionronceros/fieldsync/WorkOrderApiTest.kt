package com.corporacionronceros.fieldsync

import com.corporacionronceros.fieldsync.model.AssignmentRequest
import com.corporacionronceros.fieldsync.model.AuthResponse
import com.corporacionronceros.fieldsync.model.CreateUserRequest
import com.corporacionronceros.fieldsync.model.LoginRequest
import com.corporacionronceros.fieldsync.model.RefreshRequest
import com.corporacionronceros.fieldsync.model.RegisterRequest
import com.corporacionronceros.fieldsync.model.StatusUpdateRequest
import com.corporacionronceros.fieldsync.model.User
import com.corporacionronceros.fieldsync.model.UserRole
import com.corporacionronceros.fieldsync.model.WorkOrder
import com.corporacionronceros.fieldsync.model.WorkOrderStatus
import com.corporacionronceros.fieldsync.repository.SeedData
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.head
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

    private suspend fun HttpClient.loginDemo(): String = loginAs(SeedData.DEMO_ADMIN_EMAIL)

    /** Los usuarios semilla (admin/dispatcher/técnico) comparten la contraseña demo. */
    private suspend fun HttpClient.loginAs(email: String): String {
        val res: AuthResponse = post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, SeedData.DEMO_ADMIN_PASSWORD))
        }.body()
        return res.token
    }

    @Test
    fun `health is public`() = testApplication {
        application { module() }
        assertEquals(HttpStatusCode.OK, client.get("/health").status)
    }

    @Test
    fun `HEAD health does not 405 (monitores de uptime como UptimeRobot lo usan por defecto)`() =
        testApplication {
            application { module() }
            assertEquals(HttpStatusCode.OK, client.head("/health").status)
        }

    @Test
    fun `health reports in-memory db when no DATABASE_URL is set`() = testApplication {
        application { module() }
        val client = jsonClient()
        val body: Map<String, String> = client.get("/health").body()
        // Sin DATABASE_URL el arranque cae al repositorio en memoria (ver Application.module);
        // el health check no debe fingir una conexión a Postgres que no existe.
        assertEquals("in-memory", body["db"])
        assertEquals("ok", body["status"])
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
    fun `refresh issues a new access token and rotates the refresh token`() = testApplication {
        application { module() }
        val client = jsonClient()

        val login: AuthResponse = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(SeedData.DEMO_ADMIN_EMAIL, SeedData.DEMO_ADMIN_PASSWORD))
        }.body()

        // Renovar con el refresh token → nuevo access token que autoriza la API.
        val refreshed: AuthResponse = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(login.refreshToken))
        }.body()
        assertEquals(HttpStatusCode.OK,
            client.get("/api/work-orders") { bearerAuth(refreshed.token) }.status)

        // El refresh token viejo ya no sirve (rotación).
        assertEquals(HttpStatusCode.Unauthorized, client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(login.refreshToken))
        }.status)
    }

    @Test
    fun `technician cannot assign orders but dispatcher can (RBAC)`() = testApplication {
        application { module() }
        val client = jsonClient()
        val techToken = client.loginAs(SeedData.DEMO_TECH_EMAIL)
        val dispToken = client.loginAs(SeedData.DEMO_DISPATCHER_EMAIL)

        val techTry = client.patch("/api/work-orders/WO-1042/assignment") {
            bearerAuth(techToken)
            contentType(ContentType.Application.Json)
            setBody(AssignmentRequest("T-01"))
        }
        assertEquals(HttpStatusCode.Forbidden, techTry.status)

        val dispTry = client.patch("/api/work-orders/WO-1042/assignment") {
            bearerAuth(dispToken)
            contentType(ContentType.Application.Json)
            setBody(AssignmentRequest("T-01"))
        }
        assertEquals(HttpStatusCode.OK, dispTry.status)
    }

    @Test
    fun `only admin can create users (RBAC)`() = testApplication {
        application { module() }
        val client = jsonClient()
        val techToken = client.loginAs(SeedData.DEMO_TECH_EMAIL)
        val adminToken = client.loginAs(SeedData.DEMO_ADMIN_EMAIL)

        val body = CreateUserRequest("Nuevo", "nuevo@demo.dev", "secret123", UserRole.TECHNICIAN)

        val techTry = client.post("/api/users") {
            bearerAuth(techToken)
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.Forbidden, techTry.status)

        val adminTry = client.post("/api/users") {
            bearerAuth(adminToken)
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.Created, adminTry.status)
    }

    @Test
    fun `admin can list users of their company, technician cannot (RBAC)`() = testApplication {
        application { module() }
        val client = jsonClient()
        val adminToken = client.loginAs(SeedData.DEMO_ADMIN_EMAIL)
        val techToken = client.loginAs(SeedData.DEMO_TECH_EMAIL)

        val techTry = client.get("/api/users") { bearerAuth(techToken) }
        assertEquals(HttpStatusCode.Forbidden, techTry.status)

        val users: List<User> = client.get("/api/users") { bearerAuth(adminToken) }.body()
        // Los 3 usuarios semilla (admin/dispatcher/técnico) de la empresa demo.
        assertTrue(users.any { it.email == SeedData.DEMO_ADMIN_EMAIL })
        assertTrue(users.any { it.email == SeedData.DEMO_DISPATCHER_EMAIL })
        assertTrue(users.any { it.email == SeedData.DEMO_TECH_EMAIL })
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
