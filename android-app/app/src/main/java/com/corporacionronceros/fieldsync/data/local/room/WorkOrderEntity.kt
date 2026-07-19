package com.corporacionronceros.fieldsync.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Representación persistida de una orden de trabajo (tabla local de Room). */
@Entity(tableName = "work_orders")
data class WorkOrderEntity(
    @PrimaryKey val id: String,
    val title: String,
    val customerName: String,
    val address: String,
    val priority: String,
    val status: String,
    val pendingSync: Boolean,
    val scheduledAtEpochMs: Long
)
