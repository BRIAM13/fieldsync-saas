val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    id("io.ktor.plugin") version "2.3.12"
    application
}

group = "com.corporacionronceros.fieldsync"
version = "0.1.0"

application {
    mainClass.set("com.corporacionronceros.fieldsync.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor server core + Netty engine
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")

    // JSON (kotlinx.serialization) + content negotiation
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")

    // Cross-origin (para que el panel Angular consuma la API) + WebSockets (tiempo real)
    implementation("io.ktor:ktor-server-cors-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-websockets-jvm:$ktor_version")

    // Observabilidad + manejo de errores
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")

    // Tests
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktor_version")
    testImplementation("io.ktor:ktor-client-content-negotiation-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}

kotlin {
    jvmToolchain(17)
}
