package com.corporacionronceros.fieldsync.repository

import com.corporacionronceros.fieldsync.db.TechniciansTable
import com.corporacionronceros.fieldsync.db.WorkOrdersTable
import com.corporacionronceros.fieldsync.model.GeoPoint
import com.corporacionronceros.fieldsync.model.PendingChange
import com.corporacionronceros.fieldsync.model.Priority
import com.corporacionronceros.fieldsync.model.ServiceRequestCreate
import com.corporacionronceros.fieldsync.model.Specialty
import com.corporacionronceros.fieldsync.model.Technician
import com.corporacionronceros.fieldsync.model.WorkOrder
import com.corporacionronceros.fieldsync.model.WorkOrderStatus
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

/**
 * Implementación de [WorkOrderRepository] respaldada en **Postgres vía Exposed**, con
 * aislamiento por tenant: cada consulta y escritura filtra por `companyId`.
 */
class ExposedWorkOrderRepository : WorkOrderRepository {

    /** Siembra órdenes + técnicos de la empresa demo si aún no hay órdenes. */
    suspend fun seedIfEmpty() = dbQuery {
        if (WorkOrdersTable.selectAll().empty()) {
            SeedData.technicians().forEach { t ->
                TechniciansTable.insert {
                    it[id] = t.id
                    it[companyId] = SeedData.DEMO_COMPANY_ID
                    it[name] = t.name
                    it[lat] = t.location.lat
                    it[lng] = t.location.lng
                    it[available] = t.available
                    it[specialty] = t.specialty.name
                }
            }
            SeedData.orders().forEach { insertOrder(SeedData.DEMO_COMPANY_ID, it) }
        }
    }

    /**
     * Completa `specialty` para los técnicos demo sembrados ANTES de que existiera esa
     * columna (ya en producción): `seedIfEmpty()` no vuelve a correr porque `work_orders`
     * ya no está vacía, así que sin esto los 3 técnicos quedarían con `specialty = null`
     * para siempre. Cada `update` solo toca la fila si aún tiene `specialty IS NULL`, así
     * que reejecutarlo en cada arranque es un no-op una vez aplicado.
     */
    suspend fun backfillTechnicianSpecialties() = dbQuery {
        val bySeedId = SeedData.technicians().associateBy { it.id }
        bySeedId.values.forEach { t ->
            TechniciansTable.update({
                (TechniciansTable.id eq t.id) and (TechniciansTable.specialty.isNull())
            }) {
                it[specialty] = t.specialty.name
            }
        }
    }

    override suspend fun all(companyId: String): List<WorkOrder> = dbQuery {
        val techNames = technicianNamesById(companyId)
        WorkOrdersTable.selectAll().where { WorkOrdersTable.companyId eq companyId }
            .map { it.toWorkOrder(techNames[it[WorkOrdersTable.assignedTechnicianId]]) }
    }

    override suspend fun byId(companyId: String, id: String): WorkOrder? = dbQuery {
        fetch(companyId, id)
    }

    override suspend fun technicians(companyId: String): List<Technician> = dbQuery {
        TechniciansTable.selectAll().where { TechniciansTable.companyId eq companyId }
            .map { it.toTechnician() }
    }

    override suspend fun updateStatus(companyId: String, id: String, status: WorkOrderStatus): WorkOrder? = dbQuery {
        val changed = WorkOrdersTable.update({
            (WorkOrdersTable.id eq id) and (WorkOrdersTable.companyId eq companyId)
        }) {
            it[WorkOrdersTable.status] = status.name
        }
        if (changed == 0) null else fetch(companyId, id)
    }

    override suspend fun assign(companyId: String, id: String, technicianId: String): WorkOrder? = dbQuery {
        val techInCompany = TechniciansTable.selectAll().where {
            (TechniciansTable.id eq technicianId) and (TechniciansTable.companyId eq companyId)
        }.any()
        if (!techInCompany) return@dbQuery null

        val changed = WorkOrdersTable.update({
            (WorkOrdersTable.id eq id) and (WorkOrdersTable.companyId eq companyId)
        }) {
            it[assignedTechnicianId] = technicianId
            it[status] = WorkOrderStatus.ASSIGNED.name
        }
        if (changed == 0) null else fetch(companyId, id)
    }

    override suspend fun applyPending(companyId: String, changes: List<PendingChange>): Pair<Int, List<String>> = dbQuery {
        var applied = 0
        val rejected = mutableListOf<String>()
        for (change in changes) {
            val changed = WorkOrdersTable.update({
                (WorkOrdersTable.id eq change.id) and (WorkOrdersTable.companyId eq companyId)
            }) {
                it[status] = change.status.name
            }
            if (changed == 0) rejected += change.id else applied++
        }
        applied to rejected
    }

    override suspend fun create(
        companyId: String,
        customerId: String,
        customerName: String,
        req: ServiceRequestCreate
    ): WorkOrder = dbQuery {
        val order = WorkOrder(
            id = newWorkOrderId(),
            title = req.title,
            customerName = customerName,
            address = req.address,
            priority = req.priority,
            status = WorkOrderStatus.UNASSIGNED,
            scheduledAtEpochMs = System.currentTimeMillis(),
            location = GeoPoint(req.lat, req.lng),
            customerId = customerId
        )
        WorkOrdersTable.insert {
            it[id] = order.id
            it[WorkOrdersTable.companyId] = companyId
            it[title] = order.title
            it[WorkOrdersTable.customerName] = order.customerName
            it[address] = order.address
            it[priority] = order.priority.name
            it[status] = order.status.name
            it[scheduledAtEpochMs] = order.scheduledAtEpochMs
            it[lat] = order.location?.lat
            it[lng] = order.location?.lng
            it[WorkOrdersTable.customerId] = customerId
        }
        order
    }

    override suspend fun byCustomer(companyId: String, customerId: String): List<WorkOrder> = dbQuery {
        val techNames = technicianNamesById(companyId)
        WorkOrdersTable.selectAll().where {
            (WorkOrdersTable.companyId eq companyId) and (WorkOrdersTable.customerId eq customerId)
        }.map { it.toWorkOrder(techNames[it[WorkOrdersTable.assignedTechnicianId]]) }
    }

    // ---- helpers (dentro de una transacción) ----

    private fun technicianNamesById(companyId: String): Map<String, String> =
        TechniciansTable.selectAll().where { TechniciansTable.companyId eq companyId }
            .associate { it[TechniciansTable.id] to it[TechniciansTable.name] }

    private fun fetch(companyId: String, id: String): WorkOrder? {
        val row = WorkOrdersTable.selectAll().where {
            (WorkOrdersTable.id eq id) and (WorkOrdersTable.companyId eq companyId)
        }.singleOrNull() ?: return null
        val techId = row[WorkOrdersTable.assignedTechnicianId]
        val techName = techId?.let {
            TechniciansTable.selectAll().where {
                (TechniciansTable.id eq techId) and (TechniciansTable.companyId eq companyId)
            }.singleOrNull()?.get(TechniciansTable.name)
        }
        return row.toWorkOrder(techName)
    }

    private fun insertOrder(companyId: String, o: WorkOrder) {
        WorkOrdersTable.insert {
            it[id] = o.id
            it[WorkOrdersTable.companyId] = companyId
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

    private fun ResultRow.toWorkOrder(technicianName: String? = null): WorkOrder {
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
            assignedTechnicianId = this[WorkOrdersTable.assignedTechnicianId],
            assignedTechnicianName = technicianName,
            customerId = this[WorkOrdersTable.customerId]
        )
    }

    private fun ResultRow.toTechnician(): Technician = Technician(
        id = this[TechniciansTable.id],
        name = this[TechniciansTable.name],
        location = GeoPoint(this[TechniciansTable.lat], this[TechniciansTable.lng]),
        available = this[TechniciansTable.available],
        specialty = this[TechniciansTable.specialty]?.let { Specialty.valueOf(it) } ?: Specialty.GENERAL
    )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
