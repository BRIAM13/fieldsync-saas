package com.corporacionronceros.fieldsync.repository

import com.corporacionronceros.fieldsync.db.CompaniesTable
import com.corporacionronceros.fieldsync.db.RefreshTokensTable
import com.corporacionronceros.fieldsync.db.UsersTable
import com.corporacionronceros.fieldsync.model.Company
import com.corporacionronceros.fieldsync.model.User
import com.corporacionronceros.fieldsync.model.UserRole
import com.corporacionronceros.fieldsync.security.PasswordHasher
import com.corporacionronceros.fieldsync.security.RefreshTokenGenerator
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

/** Empresas + usuarios respaldados en Postgres (Exposed). */
class ExposedAuthRepository : AuthRepository {

    /** Siembra la empresa demo + sus usuarios (admin/dispatcher/técnico) si no existen. */
    suspend fun seedIfEmpty() = dbQuery {
        if (CompaniesTable.selectAll().empty()) {
            val company = SeedData.company()
            CompaniesTable.insert {
                it[id] = company.id
                it[name] = company.name
            }
            SeedData.users().forEach { seed ->
                UsersTable.insert {
                    it[id] = seed.user.id
                    it[companyId] = seed.user.companyId
                    it[email] = seed.user.email.lowercase()
                    it[name] = seed.user.name
                    it[role] = seed.user.role.name
                    it[passwordHash] = PasswordHasher.hash(seed.password)
                }
            }
        }
    }

    override suspend fun createUser(
        companyId: String,
        name: String,
        email: String,
        passwordHash: String,
        role: UserRole
    ): User? = dbQuery {
        val exists = UsersTable.selectAll().where { UsersTable.email eq email.lowercase() }.any()
        if (exists) return@dbQuery null
        val user = User(UUID.randomUUID().toString(), companyId, email.lowercase(), name, role)
        UsersTable.insert {
            it[id] = user.id
            it[UsersTable.companyId] = user.companyId
            it[UsersTable.email] = user.email
            it[UsersTable.name] = user.name
            it[UsersTable.role] = user.role.name
            it[UsersTable.passwordHash] = passwordHash
        }
        user
    }

    override suspend fun emailExists(email: String): Boolean = dbQuery {
        UsersTable.selectAll().where { UsersTable.email eq email.lowercase() }.any()
    }

    override suspend fun findByEmail(email: String): UserRecord? = dbQuery {
        UsersTable.selectAll().where { UsersTable.email eq email.lowercase() }
            .singleOrNull()?.toUserRecord()
    }

    override suspend fun companyById(id: String): Company? = dbQuery {
        CompaniesTable.selectAll().where { CompaniesTable.id eq id }.singleOrNull()
            ?.let { Company(it[CompaniesTable.id], it[CompaniesTable.name]) }
    }

    override suspend fun createCompanyWithAdmin(
        companyName: String,
        name: String,
        email: String,
        passwordHash: String
    ): AuthResult = dbQuery {
        val company = Company(UUID.randomUUID().toString(), companyName)
        CompaniesTable.insert {
            it[id] = company.id
            it[CompaniesTable.name] = company.name
        }
        val user = User(
            id = UUID.randomUUID().toString(),
            companyId = company.id,
            email = email.lowercase(),
            name = name,
            role = UserRole.ADMIN
        )
        UsersTable.insert {
            it[id] = user.id
            it[companyId] = user.companyId
            it[UsersTable.email] = user.email
            it[UsersTable.name] = user.name
            it[role] = user.role.name
            it[UsersTable.passwordHash] = passwordHash
        }
        AuthResult(company, user)
    }

    override suspend fun createRefreshToken(userId: String, expiresAtEpochMs: Long): String = dbQuery {
        val token = RefreshTokenGenerator.generate()
        RefreshTokensTable.insert {
            it[RefreshTokensTable.token] = token
            it[RefreshTokensTable.userId] = userId
            it[RefreshTokensTable.expiresAtEpochMs] = expiresAtEpochMs
        }
        token
    }

    override suspend fun consumeRefreshToken(token: String): User? = dbQuery {
        val row = RefreshTokensTable.selectAll()
            .where { RefreshTokensTable.token eq token }.singleOrNull() ?: return@dbQuery null
        RefreshTokensTable.deleteWhere { RefreshTokensTable.token eq token } // rotación
        if (row[RefreshTokensTable.expiresAtEpochMs] < System.currentTimeMillis()) return@dbQuery null
        val userId = row[RefreshTokensTable.userId]
        UsersTable.selectAll().where { UsersTable.id eq userId }.singleOrNull()?.toUserRecord()?.user
    }

    override suspend fun revokeRefreshToken(token: String) {
        dbQuery { RefreshTokensTable.deleteWhere { RefreshTokensTable.token eq token } }
    }

    private fun ResultRow.toUserRecord(): UserRecord = UserRecord(
        user = User(
            id = this[UsersTable.id],
            companyId = this[UsersTable.companyId],
            email = this[UsersTable.email],
            name = this[UsersTable.name],
            role = UserRole.valueOf(this[UsersTable.role])
        ),
        passwordHash = this[UsersTable.passwordHash]
    )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
