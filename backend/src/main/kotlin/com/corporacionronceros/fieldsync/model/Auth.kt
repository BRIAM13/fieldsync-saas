package com.corporacionronceros.fieldsync.model

import kotlinx.serialization.Serializable

/** Empresa = tenant. Todos los datos de negocio están aislados por `companyId`. */
@Serializable
data class Company(val id: String, val name: String)

@Serializable
enum class UserRole { ADMIN, DISPATCHER, TECHNICIAN }

/** Usuario del sistema, siempre perteneciente a una empresa (multi-tenancy). */
@Serializable
data class User(
    val id: String,
    val companyId: String,
    val email: String,
    val name: String,
    val role: UserRole
)

/** Registro: crea una empresa nueva y su usuario administrador en un solo paso. */
@Serializable
data class RegisterRequest(
    val companyName: String,
    val name: String,
    val email: String,
    val password: String
)

@Serializable
data class LoginRequest(val email: String, val password: String)

/** Respuesta de /auth/login y /auth/register: el token + a quién representa. */
@Serializable
data class AuthResponse(val token: String, val user: User, val company: Company)
