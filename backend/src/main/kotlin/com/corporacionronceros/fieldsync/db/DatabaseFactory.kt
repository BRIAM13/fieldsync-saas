package com.corporacionronceros.fieldsync.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI

/**
 * Inicializa la conexión a Postgres (Hikari + Exposed) desde variables de entorno.
 *
 * Acepta tanto una `DATABASE_URL` estilo proveedor (`postgres://user:pass@host:port/db`,
 * como la exponen Neon / Supabase / Render) como una `jdbc:postgresql://...` con
 * `DB_USER` / `DB_PASSWORD` aparte. Si no hay `DATABASE_URL`, devuelve false y el
 * arranque cae al repositorio en memoria (modo desarrollo sin DB).
 */
object DatabaseFactory {

    fun init(): Boolean {
        val raw = System.getenv("DATABASE_URL")?.takeIf { it.isNotBlank() } ?: return false
        val creds = parse(raw)

        val hikari = HikariConfig().apply {
            jdbcUrl = creds.jdbcUrl
            driverClassName = "org.postgresql.Driver"
            username = creds.user
            password = creds.password
            maximumPoolSize = System.getenv("DB_POOL_SIZE")?.toIntOrNull() ?: 5
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        Database.connect(HikariDataSource(hikari))
        transaction {
            SchemaUtils.create(CompaniesTable, UsersTable, WorkOrdersTable, TechniciansTable)
        }
        return true
    }

    private data class Creds(val jdbcUrl: String, val user: String?, val password: String?)

    private fun parse(raw: String): Creds {
        if (raw.startsWith("jdbc:")) {
            // jdbc:postgresql://host:port/db — usuario/clave por variables aparte
            return Creds(raw, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"))
        }
        // postgres:// o postgresql:// (formato de los proveedores)
        val uri = URI(raw)
        val userInfo = uri.userInfo?.split(":", limit = 2)
        val user = userInfo?.getOrNull(0) ?: System.getenv("DB_USER")
        val password = userInfo?.getOrNull(1) ?: System.getenv("DB_PASSWORD")
        val port = if (uri.port != -1) uri.port else 5432
        val query = uri.query?.let { "?$it" } ?: "?sslmode=require"
        val jdbc = "jdbc:postgresql://${uri.host}:$port${uri.path}$query"
        return Creds(jdbc, user, password)
    }
}
