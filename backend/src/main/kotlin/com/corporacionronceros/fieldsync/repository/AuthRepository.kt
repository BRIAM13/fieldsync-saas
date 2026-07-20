package com.corporacionronceros.fieldsync.repository

import com.corporacionronceros.fieldsync.model.Company
import com.corporacionronceros.fieldsync.model.User
import com.corporacionronceros.fieldsync.model.UserRole
import com.corporacionronceros.fieldsync.security.PasswordHasher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/** Usuario + su hash de contraseña (el hash nunca sale de la capa de datos hacia la API). */
data class UserRecord(val user: User, val passwordHash: String)

/** Resultado de un registro: la empresa creada y su usuario administrador. */
data class AuthResult(val company: Company, val user: User)

/** Almacén de empresas y usuarios para el flujo de autenticación. */
interface AuthRepository {
    suspend fun emailExists(email: String): Boolean
    suspend fun findByEmail(email: String): UserRecord?
    suspend fun companyById(id: String): Company?
    suspend fun createCompanyWithAdmin(
        companyName: String,
        name: String,
        email: String,
        passwordHash: String
    ): AuthResult
}

/** Implementación en memoria; siembra la empresa demo + su admin al construirse. */
class InMemoryAuthRepository : AuthRepository {

    private val mutex = Mutex()
    private val companies = linkedMapOf<String, Company>()
    private val users = linkedMapOf<String, UserRecord>() // key = email en minúsculas

    init {
        val company = SeedData.company()
        companies[company.id] = company
        val admin = SeedData.adminUser()
        users[admin.email.lowercase()] =
            UserRecord(admin, PasswordHasher.hash(SeedData.DEMO_ADMIN_PASSWORD))
    }

    override suspend fun emailExists(email: String): Boolean =
        mutex.withLock { users.containsKey(email.lowercase()) }

    override suspend fun findByEmail(email: String): UserRecord? =
        mutex.withLock { users[email.lowercase()] }

    override suspend fun companyById(id: String): Company? =
        mutex.withLock { companies[id] }

    override suspend fun createCompanyWithAdmin(
        companyName: String,
        name: String,
        email: String,
        passwordHash: String
    ): AuthResult = mutex.withLock {
        val company = Company(id = UUID.randomUUID().toString(), name = companyName)
        companies[company.id] = company
        val user = User(
            id = UUID.randomUUID().toString(),
            companyId = company.id,
            email = email,
            name = name,
            role = UserRole.ADMIN
        )
        users[email.lowercase()] = UserRecord(user, passwordHash)
        AuthResult(company, user)
    }
}
