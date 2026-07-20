package com.corporacionronceros.fieldsync.repository

import com.corporacionronceros.fieldsync.model.GeoPoint
import com.corporacionronceros.fieldsync.model.Priority
import com.corporacionronceros.fieldsync.model.Technician
import com.corporacionronceros.fieldsync.model.WorkOrder
import com.corporacionronceros.fieldsync.model.WorkOrderStatus

/** Datos de arranque compartidos por las implementaciones en memoria y en Postgres. */
object SeedData {

    fun orders(): List<WorkOrder> {
        val now = System.currentTimeMillis()
        return listOf(
            WorkOrder("WO-1042", "Fuga en tubería principal", "Ferretería El Sol",
                "Av. Los Álamos 234", Priority.URGENT, WorkOrderStatus.UNASSIGNED,
                now + 3_600_000, GeoPoint(-12.050, -77.040)),
            WorkOrder("WO-1043", "Instalación de tablero eléctrico", "Condominio Las Palmas",
                "Jr. Independencia 87", Priority.HIGH, WorkOrderStatus.UNASSIGNED,
                now + 7_200_000, GeoPoint(-12.080, -77.020)),
            WorkOrder("WO-1044", "Mantenimiento de calentador", "Sra. Quispe",
                "Calle Lima 12", Priority.MEDIUM, WorkOrderStatus.UNASSIGNED,
                now + 14_400_000, GeoPoint(-12.100, -77.000)),
        )
    }

    fun technicians(): List<Technician> = listOf(
        Technician("T-01", "Carlos Ramírez", GeoPoint(-12.046, -77.043), available = true),
        Technician("T-02", "Lucía Fernández", GeoPoint(-12.089, -77.021), available = true),
        Technician("T-03", "Miguel Torres", GeoPoint(-12.112, -76.998), available = false),
    )
}
