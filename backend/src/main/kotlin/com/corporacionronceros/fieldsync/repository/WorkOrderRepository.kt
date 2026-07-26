package com.corporacionronceros.fieldsync.repository

import com.corporacionronceros.fieldsync.model.GeoPoint
import com.corporacionronceros.fieldsync.model.PendingChange
import com.corporacionronceros.fieldsync.model.ServiceRequestCreate
import com.corporacionronceros.fieldsync.model.Technician
import com.corporacionronceros.fieldsync.model.WorkOrder
import com.corporacionronceros.fieldsync.model.WorkOrderStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Puerto de persistencia de órdenes, **con conciencia de tenant**: toda operación recibe el
 * `companyId` del portador del token, de modo que una empresa nunca ve ni modifica datos de otra.
 * Definido como interfaz para poder cambiar la impl (memoria ↔ Postgres) sin tocar las rutas.
 */
interface WorkOrderRepository {
    suspend fun all(companyId: String): List<WorkOrder>
    suspend fun byId(companyId: String, id: String): WorkOrder?
    suspend fun updateStatus(companyId: String, id: String, status: WorkOrderStatus): WorkOrder?
    suspend fun assign(companyId: String, id: String, technicianId: String): WorkOrder?
    suspend fun technicians(companyId: String): List<Technician>
    suspend fun applyPending(companyId: String, changes: List<PendingChange>): Pair<Int, List<String>>

    /** Un cliente solicita un servicio: crea la orden en su propia empresa, sin asignar. */
    suspend fun create(
        companyId: String,
        customerId: String,
        customerName: String,
        req: ServiceRequestCreate
    ): WorkOrder

    /** Solicitudes de UN cliente dentro de su empresa (para que trackee las suyas). */
    suspend fun byCustomer(companyId: String, customerId: String): List<WorkOrder>
}

/** Id de 32 caracteres (cabe exacto en la columna `varchar(32)`) para órdenes creadas en vivo. */
internal fun newWorkOrderId(): String = UUID.randomUUID().toString().replace("-", "")

/** Implementación en memoria, segura para concurrencia con un Mutex de coroutines. */
class InMemoryWorkOrderRepository : WorkOrderRepository {

    private val mutex = Mutex()
    private val orders = linkedMapOf<String, Pair<String, WorkOrder>>() // id -> (companyId, order)
    private val techs = mutableListOf<Pair<String, Technician>>()        // (companyId, technician)

    init {
        SeedData.technicians().forEach { techs += SeedData.DEMO_COMPANY_ID to it }
        SeedData.orders().forEach { orders[it.id] = SeedData.DEMO_COMPANY_ID to it }
    }

    /** Resuelve `assignedTechnicianName` para mostrar, sin necesidad de un FK real. */
    private fun WorkOrder.withTechName(companyId: String): WorkOrder {
        val techName = assignedTechnicianId?.let { techId ->
            techs.find { it.first == companyId && it.second.id == techId }?.second?.name
        }
        return copy(assignedTechnicianName = techName)
    }

    override suspend fun all(companyId: String): List<WorkOrder> = mutex.withLock {
        orders.values.filter { it.first == companyId }.map { it.second.withTechName(companyId) }
    }

    override suspend fun byId(companyId: String, id: String): WorkOrder? = mutex.withLock {
        orders[id]?.takeIf { it.first == companyId }?.second?.withTechName(companyId)
    }

    override suspend fun technicians(companyId: String): List<Technician> = mutex.withLock {
        techs.filter { it.first == companyId }.map { it.second }
    }

    override suspend fun updateStatus(companyId: String, id: String, status: WorkOrderStatus): WorkOrder? =
        mutex.withLock {
            val entry = orders[id]?.takeIf { it.first == companyId } ?: return@withLock null
            val updated = entry.second.copy(status = status)
            orders[id] = companyId to updated
            updated.withTechName(companyId)
        }

    override suspend fun assign(companyId: String, id: String, technicianId: String): WorkOrder? =
        mutex.withLock {
            val entry = orders[id]?.takeIf { it.first == companyId } ?: return@withLock null
            val techInCompany = techs.any { it.first == companyId && it.second.id == technicianId }
            if (!techInCompany) return@withLock null
            val updated = entry.second.copy(
                assignedTechnicianId = technicianId,
                status = WorkOrderStatus.ASSIGNED
            )
            orders[id] = companyId to updated
            updated.withTechName(companyId)
        }

    override suspend fun applyPending(companyId: String, changes: List<PendingChange>): Pair<Int, List<String>> =
        mutex.withLock {
            var applied = 0
            val rejected = mutableListOf<String>()
            for (change in changes) {
                val entry = orders[change.id]?.takeIf { it.first == companyId }
                if (entry == null) {
                    rejected += change.id
                } else {
                    orders[change.id] = companyId to entry.second.copy(status = change.status)
                    applied++
                }
            }
            applied to rejected
        }

    override suspend fun create(
        companyId: String,
        customerId: String,
        customerName: String,
        req: ServiceRequestCreate
    ): WorkOrder = mutex.withLock {
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
        orders[order.id] = companyId to order
        order
    }

    override suspend fun byCustomer(companyId: String, customerId: String): List<WorkOrder> = mutex.withLock {
        orders.values.filter { it.first == companyId && it.second.customerId == customerId }
            .map { it.second.withTechName(companyId) }
    }
}
