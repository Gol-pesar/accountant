package com.example.security

import android.util.Base64
import com.example.domain.JalaliCalendar
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object SecurityCrypto {
    private const val ITERATIONS = 65_536
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    val PRESET_SECURITY_QUESTIONS = listOf(
        "نام اولین معلم شما چیست؟",
        "نام شهر محل تولد شما چیست؟",
        "نام بهترین دوست دوران کودکی شما چیست؟",
        "نام اولین مدرسه شما چه بود؟",
        "مدل اولین گوشی یا خودروی شما چیست؟"
    )

    fun normalizeAnswer(raw: String): String {
        return JalaliCalendar.toLatinDigits(raw)
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('\u200C', ' ')
            .trim()
            .replace(Regex("\\s+"), " ")
            .lowercase()
    }

    fun hashSecret(secret: String): String {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        val derived = deriveKey(secret, salt)
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashB64 = Base64.encodeToString(derived, Base64.NO_WRAP)
        return "$saltB64:$hashB64"
    }

    fun verifySecret(candidate: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 2) return false
        return runCatching {
            val salt = Base64.decode(parts[0], Base64.NO_WRAP)
            val expected = Base64.decode(parts[1], Base64.NO_WRAP)
            val actual = deriveKey(candidate, salt)
            MessageDigest.isEqual(expected, actual)
        }.getOrDefault(false)
    }

    private fun deriveKey(secret: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(secret.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
