package com.vx7.khatapro.core.utils

import java.security.MessageDigest
import java.util.UUID

object SecurityUtils {
    private const val SALT = "KhataPro_Secure_Salt_2026"

    fun hashPassword(password: String): String {
        val input = "$SALT:$password"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, storedHash: String): Boolean {
        val calculated = hashPassword(password)
        return calculated.equals(storedHash, ignoreCase = true)
    }

    fun getOrCreateDeviceId(): String {
        return "KP-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
    }
}
