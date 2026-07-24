package com.corporacionronceros.fieldsync.security

/** Dominios de correo desechable/temporal conocidos — bloquea el registro con ellos. */
object DisposableEmailBlocklist {
    private val domains = setOf(
        "mailinator.com", "10minutemail.com", "guerrillamail.com", "yopmail.com",
        "tempmail.com", "trashmail.com", "throwawaymail.com", "dispostable.com",
        "getnada.com", "sharklasers.com", "fakeinbox.com", "maildrop.cc"
    )

    fun isDisposable(email: String): Boolean {
        val domain = email.substringAfter('@', "").lowercase()
        return domain in domains
    }
}
