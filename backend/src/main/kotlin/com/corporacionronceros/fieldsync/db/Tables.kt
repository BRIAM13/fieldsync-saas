package com.corporacionronceros.fieldsync.db

import org.jetbrains.exposed.sql.Table

/** Esquema Exposed (DSL tipado de Kotlin). Se traduce a DDL de Postgres. */

object CompaniesTable : Table("companies") {
    val id = varchar("id", 36)
    val name = varchar("name", 255)
    override val primaryKey = PrimaryKey(id)
}

object UsersTable : Table("users") {
    val id = varchar("id", 36)
    val companyId = varchar("company_id", 36).references(CompaniesTable.id)
    val email = varchar("email", 255).uniqueIndex()
    val name = varchar("name", 255)
    val role = varchar("role", 16)
    val passwordHash = varchar("password_hash", 100)
    override val primaryKey = PrimaryKey(id)
}

object WorkOrdersTable : Table("work_orders") {
    val id = varchar("id", 32)
    val companyId = varchar("company_id", 36).references(CompaniesTable.id).index()
    val title = varchar("title", 255)
    val customerName = varchar("customer_name", 255)
    val address = varchar("address", 255)
    val priority = varchar("priority", 16)
    val status = varchar("status", 16)
    val scheduledAtEpochMs = long("scheduled_at_epoch_ms")
    val lat = double("lat").nullable()
    val lng = double("lng").nullable()
    val assignedTechnicianId = varchar("assigned_technician_id", 32).nullable()
    override val primaryKey = PrimaryKey(id)
}

object TechniciansTable : Table("technicians") {
    val id = varchar("id", 32)
    val companyId = varchar("company_id", 36).references(CompaniesTable.id).index()
    val name = varchar("name", 255)
    val lat = double("lat")
    val lng = double("lng")
    val available = bool("available")
    override val primaryKey = PrimaryKey(id)
}
