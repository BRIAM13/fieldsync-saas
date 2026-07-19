package com.corporacionronceros.fieldsync.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WorkOrderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FieldSyncDatabase : RoomDatabase() {
    abstract fun workOrderDao(): WorkOrderDao

    companion object {
        const val NAME = "fieldsync.db"
    }
}
