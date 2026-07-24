package com.corporacionronceros.fieldsync

import com.corporacionronceros.fieldsync.model.CustomerRegisterRequest
import com.corporacionronceros.fieldsync.model.RegisterRequest
import com.corporacionronceros.fieldsync.repository.SeedData
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
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

/**
 * Antifraude en el registro: honeypot, correos desechables y rate limiting por IP.
 *
 * El rate limiter es un singleton en memoria (vive todo el proceso JVM, no solo la
 * `testApplication` de un test) — cada test manda un `X-Forwarded-For` propio y único para
 * quedar en su propio "cubo" y no contaminarse entre tests ni con el orden de ejecución.
 */
class AntiAbuseTest {

    private fun ApplicationTestBuilder.jsonClient(): HttpClient =
        createClient { install(ContentNegotiation) { json() } }

    @Test
    fun `honeypot lleno rechaza el registro de staff`() = testApplication {
        application { module() }
        val client = jsonClient()
        val res = client.post("/auth/register") {
            header("X-Forwarded-For", "10.0.1.1")
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("Empresa Bot", "Bot", "bot1@demo.dev", "secret123", website = "http://spam.com"))
        }
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun `honeypot lleno rechaza el registro de cliente`() = testApplication {
        application { module() }
        val client = jsonClient()
        val res = client.post("/customer/auth/register") {
            header("X-Forwarded-For", "10.0.1.2")
            contentType(ContentType.Application.Json)
            setBody(CustomerRegisterRequest(
                SeedData.DEMO_COMPANY_ID, "Bot", "bot2@demo.dev", null, "secret123", website = "spam"
            ))
        }
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun `correo desechable rechaza el registro`() = testApplication {
        application { module() }
        val client = jsonClient()
        val res = client.post("/auth/register") {
            header("X-Forwarded-For", "10.0.1.3")
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("Empresa X", "Dueño", "x@mailinator.com", "secret123"))
        }
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun `mas de 5 registros seguidos activa el rate limit`() = testApplication {
        application { module() }
        val client = jsonClient()
        val ip = "10.0.1.4"

        repeat(5) { i ->
            val res = client.post("/auth/register") {
                header("X-Forwarded-For", ip)
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest("Empresa $i", "Dueño", "owner$i.rl4@demo.dev", "secret123"))
            }
            assertEquals(HttpStatusCode.Created, res.status, "intento #$i debería pasar")
        }

        // El 6º intento desde el mismo origen supera el límite (5 por ventana).
        val blocked = client.post("/auth/register") {
            header("X-Forwarded-For", ip)
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("Empresa 6", "Dueño", "owner6.rl4@demo.dev", "secret123"))
        }
        assertEquals(HttpStatusCode.TooManyRequests, blocked.status)
    }

    @Test
    fun `un registro normal sigue funcionando`() = testApplication {
        application { module() }
        val client = jsonClient()
        val res = client.post("/customer/auth/register") {
            header("X-Forwarded-For", "10.0.1.5")
            contentType(ContentType.Application.Json)
            setBody(CustomerRegisterRequest(
                SeedData.DEMO_COMPANY_ID, "Cliente Real", "real@gmail.com", null, "secret123"
            ))
        }
        assertEquals(HttpStatusCode.Created, res.status)
    }
}
