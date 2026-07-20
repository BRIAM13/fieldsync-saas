package com.corporacionronceros.fieldsync.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class LoginRequestDto(val email: String, val password: String)

@Serializable
data class UserDto(
    val id: String,
    val companyId: String,
    val email: String,
    val name: String,
    val role: String
)

@Serializable
data class CompanyDto(val id: String, val name: String)

@Serializable
data class AuthResponseDto(val token: String, val user: UserDto, val company: CompanyDto)

/** Cliente de las rutas de autenticación del backend. */
@Singleton
class AuthApi @Inject constructor(
    private val client: HttpClient
) {
    suspend fun login(email: String, password: String): AuthResponseDto =
        client.post("${ApiConfig.BASE_URL}/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequestDto(email, password))
        }.body()
}
