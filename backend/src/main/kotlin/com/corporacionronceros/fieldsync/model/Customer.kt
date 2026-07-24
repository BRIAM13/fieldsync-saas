package com.corporacionronceros.fieldsync.model

import kotlinx.serialization.Serializable

/** Cliente final: solicita servicios a UNA empresa (tenant) de la que es cliente. */
@Serializable
data class Customer(
    val id: String,
    val companyId: String,
    val name: String,
    val email: String,
    val phone: String? = null
)

/** Registro de cliente: elige la empresa (de la lista pública) a la que pertenece. */
@Serializable
data class CustomerRegisterRequest(
    val companyId: String,
    val name: String,
    val email: String,
    val phone: String? = null,
    val password: String
)

@Serializable
data class CustomerLoginRequest(val email: String, val password: String)

@Serializable
data class CustomerRefreshRequest(val refreshToken: String)

@Serializable
data class CustomerLogoutRequest(val refreshToken: String)

/** Respuesta de /customer/auth/{register,login,refresh}. */
@Serializable
data class CustomerAuthResponse(
    val token: String,
    val refreshToken: String,
    val customer: Customer,
    val company: Company
)

/** Ficha pública de una empresa, para el selector de empresa al registrarse (GET /api/companies). */
@Serializable
data class CompanySummary(val id: String, val name: String)

/** Cuerpo de POST /api/service-requests — el cliente solicita un servicio a su empresa. */
@Serializable
data class ServiceRequestCreate(
    val title: String,
    val address: String,
    val priority: Priority,
    val lat: Double,
    val lng: Double
)
