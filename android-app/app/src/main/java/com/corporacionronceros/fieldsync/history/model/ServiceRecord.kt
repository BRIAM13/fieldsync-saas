package com.corporacionronceros.fieldsync.history.model

/** Modelo del historial de servicios ya completados por el técnico. */
data class ServiceRecord(
    val id: String,
    val title: String,
    val completedOn: String,
    val customerName: String
)
