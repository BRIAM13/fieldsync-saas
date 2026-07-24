package com.corporacionronceros.fieldsync.repository

import com.corporacionronceros.fieldsync.db.CustomerRefreshTokensTable
import com.corporacionronceros.fieldsync.db.CustomersTable
import com.corporacionronceros.fieldsync.model.Customer
import com.corporacionronceros.fieldsync.security.RefreshTokenGenerator
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

/** Clientes + sus refresh tokens respaldados en Postgres (Exposed). */
class ExposedCustomerRepository : CustomerRepository {

    override suspend fun emailExists(email: String): Boolean = dbQuery {
        CustomersTable.selectAll().where { CustomersTable.email eq email.lowercase() }.any()
    }

    override suspend fun findByEmail(email: String): CustomerRecord? = dbQuery {
        CustomersTable.selectAll().where { CustomersTable.email eq email.lowercase() }
            .singleOrNull()?.toCustomerRecord()
    }

    override suspend fun findById(id: String): Customer? = dbQuery {
        CustomersTable.selectAll().where { CustomersTable.id eq id }
            .singleOrNull()?.toCustomerRecord()?.customer
    }

    override suspend fun createCustomer(
        companyId: String,
        name: String,
        email: String,
        phone: String?,
        passwordHash: String
    ): Customer? = dbQuery {
        val exists = CustomersTable.selectAll().where { CustomersTable.email eq email.lowercase() }.any()
        if (exists) return@dbQuery null
        val customer = Customer(UUID.randomUUID().toString(), companyId, name, email.lowercase(), phone)
        CustomersTable.insert {
            it[id] = customer.id
            it[CustomersTable.companyId] = customer.companyId
            it[CustomersTable.name] = customer.name
            it[CustomersTable.email] = customer.email
            it[CustomersTable.phone] = customer.phone
            it[CustomersTable.passwordHash] = passwordHash
        }
        customer
    }

    override suspend fun createRefreshToken(customerId: String, expiresAtEpochMs: Long): String = dbQuery {
        val token = RefreshTokenGenerator.generate()
        CustomerRefreshTokensTable.insert {
            it[CustomerRefreshTokensTable.token] = token
            it[CustomerRefreshTokensTable.customerId] = customerId
            it[CustomerRefreshTokensTable.expiresAtEpochMs] = expiresAtEpochMs
        }
        token
    }

    override suspend fun consumeRefreshToken(token: String): Customer? = dbQuery {
        val row = CustomerRefreshTokensTable.selectAll()
            .where { CustomerRefreshTokensTable.token eq token }.singleOrNull() ?: return@dbQuery null
        CustomerRefreshTokensTable.deleteWhere { CustomerRefreshTokensTable.token eq token } // rotación
        if (row[CustomerRefreshTokensTable.expiresAtEpochMs] < System.currentTimeMillis()) return@dbQuery null
        val customerId = row[CustomerRefreshTokensTable.customerId]
        CustomersTable.selectAll().where { CustomersTable.id eq customerId }
            .singleOrNull()?.toCustomerRecord()?.customer
    }

    override suspend fun revokeRefreshToken(token: String) {
        dbQuery { CustomerRefreshTokensTable.deleteWhere { CustomerRefreshTokensTable.token eq token } }
    }

    private fun ResultRow.toCustomerRecord(): CustomerRecord = CustomerRecord(
        customer = Customer(
            id = this[CustomersTable.id],
            companyId = this[CustomersTable.companyId],
            name = this[CustomersTable.name],
            email = this[CustomersTable.email],
            phone = this[CustomersTable.phone]
        ),
        passwordHash = this[CustomersTable.passwordHash]
    )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
