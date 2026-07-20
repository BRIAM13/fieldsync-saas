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

/** Renovar el access token con un refresh token válido. */
@Serializable
data class RefreshRequest(val refreshToken: String)

/** Cerrar sesión: revoca el refresh token. */
@Serializable
data class LogoutRequest(val refreshToken: String)

/**
 * Respuesta de /auth/login, /auth/register y /auth/refresh.
 * `token` es el access token (corto); `refreshToken` es el de larga duración (rotado en cada refresh).
 */
@Serializable
data class AuthResponse(
    val token: String,
    val refreshToken: String,
    val user: User,
    val company: Company
)
