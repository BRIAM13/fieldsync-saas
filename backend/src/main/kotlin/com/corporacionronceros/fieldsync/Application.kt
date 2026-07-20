package com.corporacionronceros.fieldsync

import com.corporacionronceros.fieldsync.plugins.configureCors
import com.corporacionronceros.fieldsync.plugins.configureMonitoring
import com.corporacionronceros.fieldsync.plugins.configureRouting
import com.corporacionronceros.fieldsync.plugins.configureSerialization
import com.corporacionronceros.fieldsync.plugins.configureSockets
import com.corporacionronceros.fieldsync.repository.InMemoryWorkOrderRepository
import com.corporacionronceros.fieldsync.tracking.TrackingService
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

/** Ensamblado de la aplicación: instala plugins e inyecta las dependencias a las rutas. */
fun Application.module() {
    val repository = InMemoryWorkOrderRepository()
    val trackingService = TrackingService()

    configureSerialization()
    configureSockets()
    configureCors()
    configureMonitoring()
    configureRouting(repository, trackingService)
}
