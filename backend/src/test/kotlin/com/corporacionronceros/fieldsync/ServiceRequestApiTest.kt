package com.corporacionronceros.fieldsync

import com.corporacionronceros.fieldsync.model.CompanySummary
import com.corporacionronceros.fieldsync.model.CreateUserRequest
import com.corporacionronceros.fieldsync.model.CustomerAuthResponse
import com.corporacionronceros.fieldsync.model.CustomerLoginRequest
import com.corporacionronceros.fieldsync.model.CustomerRegisterRequest
import com.corporacionronceros.fieldsync.model.Priority
import com.corporacionronceros.fieldsync.model.ServiceRequestCreate
import com.corporacionronceros.fieldsync.model.UserRole
import com.corporacionronceros.fieldsync.model.WorkOrder
import com.corporacionronceros.fieldsync.repository.SeedData
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
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

/** Tests de integración del flujo de clientes: registro, RBAC entre proveedores JWT, aislamiento. */
class ServiceRequestApiTest {

    private fun ApplicationTestBuilder.jsonClient(): HttpClient =
        createClient { install(ContentNegotiation) { json() } }

    private suspend fun HttpClient.registerCustomer(
        companyId: String,
        email: String,
        name: String = "Cliente Demo"
    ): CustomerAuthResponse = post("/customer/auth/register") {
        contentType(ContentType.Application.Json)
        setBody(CustomerRegisterRequest(companyId, name, email, phone = null, password = "secret123"))
    }.body()

    @Test
    fun `GET api companies is public and lists the demo company`() = testApplication {
        application { module() }
        val client = jsonClient()
        val companies: List<CompanySummary> = client.get("/api/companies").body()
        assertTrue(companies.any { it.id == SeedData.DEMO_COMPANY_ID })
    }

    @Test
    fun `customer token cannot authenticate on staff-only endpoints`() = testApplication {
        application { module() }
        val client = jsonClient()
        val customer = client.registerCustomer(SeedData.DEMO_COMPANY_ID, "cliente1@demo.dev")

        // El token de cliente no lleva `userId`: nunca autentica bajo AUTH_JWT (staff). En una
        // ruta "desnuda" (sin authorize() anidado) eso se traduce en 401 directo. En una ruta
        // con guard de rol anidado (aquí, authorize(ADMIN)) el guard corre igual y responde 403
        // (rol nulo ∉ roles permitidos) — en ambos casos el resultado es un rechazo total, la
        // ruta nunca ejecuta su lógica de negocio con datos de otro tenant.
        val tryStaffRoute = client.post("/api/users") {
            bearerAuth(customer.token)
            contentType(ContentType.Application.Json)
            setBody(CreateUserRequest("Intruso", "intruso@demo.dev", "secret123", UserRole.TECHNICIAN))
        }
        assertEquals(HttpStatusCode.Forbidden, tryStaffRoute.status)

        val tryWorkOrders = client.get("/api/work-orders") { bearerAuth(customer.token) }
        assertEquals(HttpStatusCode.Unauthorized, tryWorkOrders.status)
    }

    @Test
    fun `staff token cannot authenticate on customer-only endpoints`() = testApplication {
        application { module() }
        val client = jsonClient()
        val staffLogin: com.corporacionronceros.fieldsync.model.AuthResponse = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(com.corporacionronceros.fieldsync.model.LoginRequest(
                SeedData.DEMO_ADMIN_EMAIL, SeedData.DEMO_ADMIN_PASSWORD
            ))
        }.body()

        // /api/service-requests/mine está bajo authorizeCustomer() (guard anidado, igual patrón
        // que authorize(ADMIN) en /api/users) → 403, no 401 (ver comentario del test anterior).
        val tryCustomerRoute = client.get("/api/service-requests/mine") { bearerAuth(staffLogin.token) }
        assertEquals(HttpStatusCode.Forbidden, tryCustomerRoute.status)
    }

    @Test
    fun `customer can create and list their own service requests`() = testApplication {
        application { module() }
        val client = jsonClient()
        val customer = client.registerCustomer(SeedData.DEMO_COMPANY_ID, "cliente2@demo.dev")

        val created: WorkOrder = client.post("/api/service-requests") {
            bearerAuth(customer.token)
            contentType(ContentType.Application.Json)
            setBody(ServiceRequestCreate("Fuga de agua", "Av. Siempre Viva 123", Priority.HIGH, -12.05, -77.03))
        }.body()
        assertEquals(customer.customer.id, created.customerId)
        assertEquals(-12.05, created.location?.lat)

        val mine: List<WorkOrder> = client.get("/api/service-requests/mine") { bearerAuth(customer.token) }.body()
        assertTrue(mine.any { it.id == created.id })
    }

    @Test
    fun `a customer cannot see another customer's requests, even in the same company`() = testApplication {
        application { module() }
        val client = jsonClient()
        val alice = client.registerCustomer(SeedData.DEMO_COMPANY_ID, "alice@demo.dev")
        val bob = client.registerCustomer(SeedData.DEMO_COMPANY_ID, "bob@demo.dev")

        client.post("/api/service-requests") {
            bearerAuth(alice.token)
            contentType(ContentType.Application.Json)
            setBody(ServiceRequestCreate("Orden de Alice", "Calle 1", Priority.LOW, -12.0, -77.0))
        }

        val bobsRequests: List<WorkOrder> =
            client.get("/api/service-requests/mine") { bearerAuth(bob.token) }.body()
        assertTrue(bobsRequests.isEmpty())
    }

    @Test
    fun `a customer request appears in staff GET api work-orders with location set`() = testApplication {
        application { module() }
        val client = jsonClient()
        val customer = client.registerCustomer(SeedData.DEMO_COMPANY_ID, "cliente3@demo.dev")

        val created: WorkOrder = client.post("/api/service-requests") {
            bearerAuth(customer.token)
            contentType(ContentType.Application.Json)
            setBody(ServiceRequestCreate("Instalación", "Jr. Test 456", Priority.MEDIUM, -12.06, -77.04))
        }.body()

        val staffLogin: com.corporacionronceros.fieldsync.model.AuthResponse = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(com.corporacionronceros.fieldsync.model.LoginRequest(
                SeedData.DEMO_ADMIN_EMAIL, SeedData.DEMO_ADMIN_PASSWORD
            ))
        }.body()

        val staffOrders: List<WorkOrder> =
            client.get("/api/work-orders") { bearerAuth(staffLogin.token) }.body()
        val fromStaffView = staffOrders.single { it.id == created.id }
        assertEquals(-12.06, fromStaffView.location?.lat)
    }

    @Test
    fun `registering a customer under an unknown company fails`() = testApplication {
        application { module() }
        val client = jsonClient()
        val res = client.post("/customer/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(CustomerRegisterRequest("no-existe", "X", "x@demo.dev", null, "secret123"))
        }
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun `customer login works after register`() = testApplication {
        application { module() }
        val client = jsonClient()
        client.registerCustomer(SeedData.DEMO_COMPANY_ID, "loginme@demo.dev")

        val login: CustomerAuthResponse = client.post("/customer/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(CustomerLoginRequest("loginme@demo.dev", "secret123"))
        }.body()
        assertEquals("loginme@demo.dev", login.customer.email)
    }
}
