package com.corporacionronceros.fieldsync.repository

import com.corporacionronceros.fieldsync.model.Customer
import com.corporacionronceros.fieldsync.security.RefreshTokenGenerator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/** Cliente + su hash de contraseña (el hash nunca sale de la capa de datos hacia la API). */
data class CustomerRecord(val customer: Customer, val passwordHash: String)

/** Almacén de clientes y sus refresh tokens — separado de [AuthRepository] (staff). */
interface CustomerRepository {
    suspend fun emailExists(email: String): Boolean
    suspend fun findByEmail(email: String): CustomerRecord?
    suspend fun findById(id: String): Customer?

    /** Crea un cliente dentro de una empresa. Null si el email ya existe. */
    suspend fun createCustomer(
        companyId: String,
        name: String,
        email: String,
        phone: String?,
        passwordHash: String
    ): Customer?

    suspend fun createRefreshToken(customerId: String, expiresAtEpochMs: Long): String

    /** Valida y **consume** (rota) un refresh token: lo elimina y devuelve su cliente, o null. */
    suspend fun consumeRefreshToken(token: String): Customer?

    suspend fun revokeRefreshToken(token: String)
}

/** Implementación en memoria (modo desarrollo sin DB / tests). */
class InMemoryCustomerRepository : CustomerRepository {

    private val mutex = Mutex()
    private val customers = linkedMapOf<String, CustomerRecord>() // key = email en minúsculas
    private val refreshTokens = linkedMapOf<String, Pair<String, Long>>() // token -> (customerId, expiresAt)

    override suspend fun emailExists(email: String): Boolean =
        mutex.withLock { customers.containsKey(email.lowercase()) }

    override suspend fun findByEmail(email: String): CustomerRecord? =
        mutex.withLock { customers[email.lowercase()] }

    override suspend fun findById(id: String): Customer? =
        mutex.withLock { customers.values.firstOrNull { it.customer.id == id }?.customer }

    override suspend fun createCustomer(
        companyId: String,
        name: String,
        email: String,
        phone: String?,
        passwordHash: String
    ): Customer? = mutex.withLock {
        if (customers.containsKey(email.lowercase())) return@withLock null
        val customer = Customer(
            id = UUID.randomUUID().toString(),
            companyId = companyId,
            email = email.lowercase(),
            name = name,
            phone = phone
        )
        customers[email.lowercase()] = CustomerRecord(customer, passwordHash)
        customer
    }

    override suspend fun createRefreshToken(customerId: String, expiresAtEpochMs: Long): String =
        mutex.withLock {
            val token = RefreshTokenGenerator.generate()
            refreshTokens[token] = customerId to expiresAtEpochMs
            token
        }

    override suspend fun consumeRefreshToken(token: String): Customer? = mutex.withLock {
        val entry = refreshTokens.remove(token) ?: return@withLock null
        val (customerId, expiresAt) = entry
        if (expiresAt < System.currentTimeMillis()) return@withLock null // expirado
        customers.values.firstOrNull { it.customer.id == customerId }?.customer
    }

    override suspend fun revokeRefreshToken(token: String) {
        mutex.withLock { refreshTokens.remove(token) }
    }
}
