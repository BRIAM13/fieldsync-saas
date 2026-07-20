package com.corporacionronceros.fieldsync.repository

import com.corporacionronceros.fieldsync.model.PendingChange
import com.corporacionronceros.fieldsync.model.Priority
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
    suspend fun applyPending(changes: List<PendingChange>): Pair<Int, List<String>>
}

/** Implementación en memoria, segura para concurrencia con un Mutex de coroutines. */
class InMemoryWorkOrderRepository : WorkOrderRepository {

    private val mutex = Mutex()
    private val store = linkedMapOf<String, WorkOrder>()

    init {
        seed().forEach { store[it.id] = it }
    }

    override suspend fun all(): List<WorkOrder> = mutex.withLock { store.values.toList() }

    override suspend fun byId(id: String): WorkOrder? = mutex.withLock { store[id] }

    override suspend fun updateStatus(id: String, status: WorkOrderStatus): WorkOrder? =
        mutex.withLock {
            val current = store[id] ?: return@withLock null
            val updated = current.copy(status = status)
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

    private fun seed(): List<WorkOrder> {
        val now = System.currentTimeMillis()
        return listOf(
            WorkOrder("WO-1042", "Fuga en tubería principal", "Ferretería El Sol",
                "Av. Los Álamos 234", Priority.URGENT, WorkOrderStatus.ASSIGNED, now + 3_600_000),
            WorkOrder("WO-1043", "Instalación de tablero eléctrico", "Condominio Las Palmas",
                "Jr. Independencia 87", Priority.HIGH, WorkOrderStatus.ASSIGNED, now + 7_200_000),
            WorkOrder("WO-1044", "Mantenimiento de calentador", "Sra. Quispe",
                "Calle Lima 12", Priority.MEDIUM, WorkOrderStatus.ASSIGNED, now + 14_400_000),
        )
    }
}
