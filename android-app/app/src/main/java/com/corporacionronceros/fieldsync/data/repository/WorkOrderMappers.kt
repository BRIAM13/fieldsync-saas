package com.corporacionronceros.fieldsync.data.repository

import com.corporacionronceros.fieldsync.data.local.room.WorkOrderEntity
import com.corporacionronceros.fieldsync.data.remote.WorkOrderDto
import com.corporacionronceros.fieldsync.domain.model.Priority
import com.corporacionronceros.fieldsync.domain.model.WorkOrder
import com.corporacionronceros.fieldsync.domain.model.WorkOrderStatus

/** Mapeadores entre las tres representaciones: DTO ↔ Entity ↔ modelo de dominio. */

fun WorkOrderDto.toEntity(pendingSync: Boolean = false) = WorkOrderEntity(
    id = id,
    title = title,
    customerName = customerName,
    address = address,
    priority = priority,
    status = status,
    pendingSync = pendingSync,
    scheduledAtEpochMs = scheduledAtEpochMs
)

fun WorkOrderEntity.toDomain() = WorkOrder(
    id = id,
    title = title,
    customerName = customerName,
    address = address,
    priority = runCatching { Priority.valueOf(priority) }.getOrDefault(Priority.MEDIUM),
    status = runCatching { WorkOrderStatus.valueOf(status) }.getOrDefault(WorkOrderStatus.ASSIGNED),
    pendingSync = pendingSync,
    scheduledAtEpochMs = scheduledAtEpochMs
)
