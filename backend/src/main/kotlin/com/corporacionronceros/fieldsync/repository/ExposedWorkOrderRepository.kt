package com.corporacionronceros.fieldsync.repository

import com.corporacionronceros.fieldsync.db.TechniciansTable
import com.corporacionronceros.fieldsync.db.WorkOrdersTable
import com.corporacionronceros.fieldsync.model.GeoPoint
import com.corporacionronceros.fieldsync.model.PendingChange
import com.corporacionronceros.fieldsync.model.Priority
import com.corporacionronceros.fieldsync.model.Technician
import com.corporacionronceros.fieldsync.model.WorkOrder
import com.corporacionronceros.fieldsync.model.WorkOrderStatus
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

/**
 * Implementación de [WorkOrderRepository] respaldada en **Postgres vía Exposed**.
 *
 * Cada operación corre en una transacción suspendida (`newSuspendedTransaction` sobre
 * `Dispatchers.IO`), integrándose con las coroutines de Ktor. Implementa la MISMA interfaz
 * que la versión en memoria — cambiar de una a otra no toca las rutas.
 */
class ExposedWorkOrderRepository : WorkOrderRepository {

    /** Inserta los datos semilla solo si las tablas están vacías (arranque idempotente). */
    suspend fun seedIfEmpty() = dbQuery {
        if (WorkOrdersTable.selectAll().empty()) {
            SeedData.technicians().forEach { t ->
                TechniciansTable.insert {
                    it[id] = t.id
                    it[name] = t.name
                    it[lat] = t.location.lat
                    it[lng] = t.location.lng
                    it[available] = t.available
                }
            }
            SeedData.orders().forEach { insertOrder(it) }
        }
    }

    override suspend fun all(): List<WorkOrder> =
        dbQuery { WorkOrdersTable.selectAll().map { it.toWorkOrder() } }

    override suspend fun byId(id: String): WorkOrder? = dbQuery {
        WorkOrdersTable.selectAll().where { WorkOrdersTable.id eq id }.singleOrNull()?.toWorkOrder()
    }

    override suspend fun technicians(): List<Technician> =
        dbQuery { TechniciansTable.selectAll().map { it.toTechnician() } }

    override suspend fun updateStatus(id: String, status: WorkOrderStatus): WorkOrder? = dbQuery {
        val changed = WorkOrdersTable.update({ WorkOrdersTable.id eq id }) {
            it[WorkOrdersTable.status] = status.name
        }
        if (changed == 0) null else fetch(id)
    }

    override suspend fun assign(id: String, technicianId: String): WorkOrder? = dbQuery {
        val techExists = TechniciansTable.selectAll()
            .where { TechniciansTable.id eq technicianId }.any()
        if (!techExists) return@dbQuery null

        val changed = WorkOrdersTable.update({ WorkOrdersTable.id eq id }) {
            it[assignedTechnicianId] = technicianId
            it[status] = WorkOrderStatus.ASSIGNED.name
        }
        if (changed == 0) null else fetch(id)
    }

    override suspend fun applyPending(changes: List<PendingChange>): Pair<Int, List<String>> = dbQuery {
        var applied = 0
        val rejected = mutableListOf<String>()
        for (change in changes) {
            val changed = WorkOrdersTable.update({ WorkOrdersTable.id eq change.id }) {
                it[status] = change.status.name
            }
            if (changed == 0) rejected += change.id else applied++
        }
        applied to rejected
    }

    // ---- helpers (se ejecutan dentro de una transacción) ----

    private fun fetch(id: String): WorkOrder? =
        WorkOrdersTable.selectAll().where { WorkOrdersTable.id eq id }.singleOrNull()?.toWorkOrder()

    private fun insertOrder(o: WorkOrder) {
        WorkOrdersTable.insert {
            it[id] = o.id
            it[title] = o.title
            it[customerName] = o.customerName
            it[address] = o.address
            it[priority] = o.priority.name
            it[status] = o.status.name
            it[scheduledAtEpochMs] = o.scheduledAtEpochMs
            it[lat] = o.location?.lat
            it[lng] = o.location?.lng
            it[assignedTechnicianId] = o.assignedTechnicianId
        }
    }

    private fun ResultRow.toWorkOrder(): WorkOrder {
        val lat = this[WorkOrdersTable.lat]
        val lng = this[WorkOrdersTable.lng]
        return WorkOrder(
            id = this[WorkOrdersTable.id],
            title = this[WorkOrdersTable.title],
            customerName = this[WorkOrdersTable.customerName],
            address = this[WorkOrdersTable.address],
            priority = Priority.valueOf(this[WorkOrdersTable.priority]),
            status = WorkOrderStatus.valueOf(this[WorkOrdersTable.status]),
            scheduledAtEpochMs = this[WorkOrdersTable.scheduledAtEpochMs],
            location = if (lat != null && lng != null) GeoPoint(lat, lng) else null,
            assignedTechnicianId = this[WorkOrdersTable.assignedTechnicianId]
        )
    }

    private fun ResultRow.toTechnician(): Technician = Technician(
        id = this[TechniciansTable.id],
        name = this[TechniciansTable.name],
        location = GeoPoint(this[TechniciansTable.lat], this[TechniciansTable.lng]),
        available = this[TechniciansTable.available]
    )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
