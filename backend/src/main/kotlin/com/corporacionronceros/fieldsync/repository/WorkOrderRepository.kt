package com.corporacionronceros.fieldsync.repository

import com.corporacionronceros.fieldsync.model.PendingChange
import com.corporacionronceros.fieldsync.model.Technician
import com.corporacionronceros.fieldsync.model.WorkOrder
import com.corporacionronceros.fieldsync.model.WorkOrderStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Puerto de persistencia de órdenes. Definido como interfaz para poder sustituir la
 * implementación en memoria por una respaldada en base de datos (Exposed + Postgres)
 * sin tocar las rutas.
 */
interface WorkOrderRepository {
    suspend fun all(): List<WorkOrder>
    suspend fun byId(id: String): WorkOrder?
    suspend fun updateStatus(id: String, status: WorkOrderStatus): WorkOrder?
    suspend fun assign(id: String, technicianId: String): WorkOrder?
    suspend fun technicians(): List<Technician>
    suspend fun applyPending(changes: List<PendingChange>): Pair<Int, List<String>>
}

/** Implementación en memoria, segura para concurrencia con un Mutex de coroutines. */
class InMemoryWorkOrderRepository : WorkOrderRepository {

    private val mutex = Mutex()
    private val store = linkedMapOf<String, WorkOrder>()
    private val techs = SeedData.technicians()

    init {
        SeedData.orders().forEach { store[it.id] = it }
    }

    override suspend fun all(): List<WorkOrder> = mutex.withLock { store.values.toList() }

    override suspend fun byId(id: String): WorkOrder? = mutex.withLock { store[id] }

    override suspend fun technicians(): List<Technician> = techs

    override suspend fun updateStatus(id: String, status: WorkOrderStatus): WorkOrder? =
        mutex.withLock {
            val current = store[id] ?: return@withLock null
            val updated = current.copy(status = status)
            store[id] = updated
            updated
        }

    override suspend fun assign(id: String, technicianId: String): WorkOrder? =
        mutex.withLock {
            val current = store[id] ?: return@withLock null
            if (techs.none { it.id == technicianId }) return@withLock null
            val updated = current.copy(
                assignedTechnicianId = technicianId,
                status = WorkOrderStatus.ASSIGNED
            )
            store[id] = updated
            updated
        }

    /** Aplica en bloque los cambios pendientes; devuelve (aplicados, ids rechazados). */
    override suspend fun applyPending(changes: List<PendingChange>): Pair<Int, List<String>> =
        mutex.withLock {
            var applied = 0
            val rejected = mutableListOf<String>()
            for (change in changes) {
                val current = store[change.id]
                if (current == null) {
                    rejected += change.id
                } else {
                    store[change.id] = current.copy(status = change.status)
                    applied++
                }
            }
            applied to rejected
        }
}
