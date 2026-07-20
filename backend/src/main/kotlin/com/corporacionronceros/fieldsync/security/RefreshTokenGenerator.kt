package com.corporacionronceros.fieldsync.security

import java.security.SecureRandom
import java.util.Base64

/** Genera refresh tokens opacos, aleatorios y difíciles de adivinar (256 bits). */
object RefreshTokenGenerator {
    private val random = SecureRandom()

    fun generate(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
