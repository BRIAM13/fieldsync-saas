package com.corporacionronceros.fieldsync.repository

import com.corporacionronceros.fieldsync.model.Company
import com.corporacionronceros.fieldsync.model.User
import com.corporacionronceros.fieldsync.model.UserRole
import com.corporacionronceros.fieldsync.security.PasswordHasher
import com.corporacionronceros.fieldsync.security.RefreshTokenGenerator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/** Usuario + su hash de contraseña (el hash nunca sale de la capa de datos hacia la API). */
data class UserRecord(val user: User, val passwordHash: String)

/** Resultado de un registro: la empresa creada y su usuario administrador. */
data class AuthResult(val company: Company, val user: User)

/** Almacén de empresas, usuarios y refresh tokens para la autenticación. */
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

    /** Crea un usuario con rol dentro de una empresa (usado por el admin). Null si el email existe. */
    suspend fun createUser(
        companyId: String,
        name: String,
        email: String,
        passwordHash: String,
        role: UserRole
    ): User?

    /** Emite un refresh token para el usuario y lo persiste con su vencimiento. */
    suspend fun createRefreshToken(userId: String, expiresAtEpochMs: Long): String

    /** Valida y **consume** (rota) un refresh token: lo elimina y devuelve su usuario, o null. */
    suspend fun consumeRefreshToken(token: String): User?

    /** Revoca un refresh token (logout). */
    suspend fun revokeRefreshToken(token: String)
}

/** Implementación en memoria; siembra la empresa demo + su admin al construirse. */
class InMemoryAuthRepository : AuthRepository {

    private val mutex = Mutex()
    private val companies = linkedMapOf<String, Company>()
    private val users = linkedMapOf<String, UserRecord>() // key = email en minúsculas
    private val refreshTokens = linkedMapOf<String, Pair<String, Long>>() // token -> (userId, expiresAt)

    init {
        val company = SeedData.company()
        companies[company.id] = company
        SeedData.users().forEach { seed ->
            users[seed.user.email.lowercase()] =
                UserRecord(seed.user, PasswordHasher.hash(seed.password))
        }
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
            email = email.lowercase(),
            name = name,
            role = UserRole.ADMIN
        )
        users[email.lowercase()] = UserRecord(user, passwordHash)
        AuthResult(company, user)
    }

    override suspend fun createUser(
        companyId: String,
        name: String,
        email: String,
        passwordHash: String,
        role: UserRole
    ): User? = mutex.withLock {
        if (users.containsKey(email.lowercase())) return@withLock null
        val user = User(
            id = UUID.randomUUID().toString(),
            companyId = companyId,
            email = email.lowercase(),
            name = name,
            role = role
        )
        users[email.lowercase()] = UserRecord(user, passwordHash)
        user
    }

    override suspend fun createRefreshToken(userId: String, expiresAtEpochMs: Long): String =
        mutex.withLock {
            val token = RefreshTokenGenerator.generate()
            refreshTokens[token] = userId to expiresAtEpochMs
            token
        }

    override suspend fun consumeRefreshToken(token: String): User? = mutex.withLock {
        val entry = refreshTokens.remove(token) ?: return@withLock null
        val (userId, expiresAt) = entry
        if (expiresAt < System.currentTimeMillis()) return@withLock null // expirado
        users.values.firstOrNull { it.user.id == userId }?.user
    }

    override suspend fun revokeRefreshToken(token: String) {
        mutex.withLock { refreshTokens.remove(token) }
    }
}
