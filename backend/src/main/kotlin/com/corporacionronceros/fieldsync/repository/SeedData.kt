package com.corporacionronceros.fieldsync.repository

import com.corporacionronceros.fieldsync.model.Company
import com.corporacionronceros.fieldsync.model.GeoPoint
import com.corporacionronceros.fieldsync.model.Priority
import com.corporacionronceros.fieldsync.model.Technician
import com.corporacionronceros.fieldsync.model.User
import com.corporacionronceros.fieldsync.model.UserRole
import com.corporacionronceros.fieldsync.model.WorkOrder
import com.corporacionronceros.fieldsync.model.WorkOrderStatus

/** Datos de arranque compartidos: una empresa demo con su admin, órdenes y técnicos. */
object SeedData {

    const val DEMO_COMPANY_ID = "demo-co"
    const val DEMO_ADMIN_EMAIL = "admin@fieldsync.dev"
    const val DEMO_ADMIN_PASSWORD = "demo1234"

    fun company() = Company(DEMO_COMPANY_ID, "Servicios Demo SAC")

    fun adminUser() = User(
        id = "u-admin",
        companyId = DEMO_COMPANY_ID,
        email = DEMO_ADMIN_EMAIL,
        name = "Admin Demo",
        role = UserRole.ADMIN
    )

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
