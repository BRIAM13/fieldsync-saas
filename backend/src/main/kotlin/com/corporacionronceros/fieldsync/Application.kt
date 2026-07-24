package com.corporacionronceros.fieldsync

import com.corporacionronceros.fieldsync.db.DatabaseFactory
import com.corporacionronceros.fieldsync.plugins.configureCors
import com.corporacionronceros.fieldsync.plugins.configureMonitoring
import com.corporacionronceros.fieldsync.plugins.configureRouting
import com.corporacionronceros.fieldsync.plugins.configureSecurity
import com.corporacionronceros.fieldsync.plugins.configureSerialization
import com.corporacionronceros.fieldsync.plugins.configureSockets
import com.corporacionronceros.fieldsync.repository.AuthRepository
import com.corporacionronceros.fieldsync.repository.CustomerRepository
import com.corporacionronceros.fieldsync.repository.ExposedAuthRepository
import com.corporacionronceros.fieldsync.repository.ExposedCustomerRepository
import com.corporacionronceros.fieldsync.repository.ExposedWorkOrderRepository
import com.corporacionronceros.fieldsync.repository.InMemoryAuthRepository
import com.corporacionronceros.fieldsync.repository.InMemoryCustomerRepository
import com.corporacionronceros.fieldsync.repository.InMemoryWorkOrderRepository
import com.corporacionronceros.fieldsync.repository.WorkOrderRepository
import com.corporacionronceros.fieldsync.security.JwtService
import com.corporacionronceros.fieldsync.tracking.TrackingService
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking

fun main() {
    // El puerto viene de la variable PORT (convención de los hosts como Render/Fly), o 8080.
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

/** Ensamblado de la aplicación: instala plugins e inyecta las dependencias a las rutas. */
fun Application.module() {
    val jwtService = JwtService.fromEnv()

    // Postgres si hay DATABASE_URL; si no, repositorios en memoria (desarrollo sin DB).
    val authRepository: AuthRepository
    val workOrderRepository: WorkOrderRepository
    val customerRepository: CustomerRepository
    if (DatabaseFactory.init()) {
        log.info("Persistencia: Postgres (Exposed)")
        val auth = ExposedAuthRepository().also { runBlocking { it.seedIfEmpty() } }
        val work = ExposedWorkOrderRepository().also { runBlocking { it.seedIfEmpty() } }
        authRepository = auth
        workOrderRepository = work
        customerRepository = ExposedCustomerRepository()
    } else {
        log.info("Persistencia: en memoria (sin DATABASE_URL)")
        authRepository = InMemoryAuthRepository()
        workOrderRepository = InMemoryWorkOrderRepository()
        customerRepository = InMemoryCustomerRepository()
    }
    val trackingService = TrackingService()

    configureSerialization()
    configureSockets()
    configureCors()
    configureSecurity(jwtService)
    configureMonitoring()
    configureRouting(workOrderRepository, authRepository, customerRepository, trackingService, jwtService)
}
