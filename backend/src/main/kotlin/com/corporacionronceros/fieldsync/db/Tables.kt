package com.corporacionronceros.fieldsync.db

import org.jetbrains.exposed.sql.Table

/** Esquema Exposed (DSL tipado de Kotlin). Se traduce a DDL de Postgres. */

object WorkOrdersTable : Table("work_orders") {
    val id = varchar("id", 32)
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
    val name = varchar("name", 255)
    val lat = double("lat")
    val lng = double("lng")
    val available = bool("available")
    override val primaryKey = PrimaryKey(id)
}
