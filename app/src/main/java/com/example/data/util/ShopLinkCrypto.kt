package com.example.data.util

import android.os.Build
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale

/**
 * Cryptographic and formatting utilities for zero-PII multi-device shop linking.
 *
 * Shop links are keyed by SHA-256(normalizedLowercaseEmail + ":" + sixDigitCode), so neither
 * plaintext emails nor raw 6-digit codes are stored in the /shopLinks lookup collection.
 */
object ShopLinkCrypto {
    const val LINK_CODE_TTL_MILLIS: Long = 15L * 60L * 1000L // 15 minutes
    const val MAX_DEVICE_NAME_LENGTH: Int = 80

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun normalizeEmail(raw: String): String = raw.trim().lowercase(Locale.ROOT)

    fun isValidEmail(raw: String): Boolean {
        val clean = normalizeEmail(raw)
        return clean.isNotEmpty() && clean.length <= 120 && EMAIL_REGEX.matches(clean)
    }

    fun normalizeSixDigitCode(raw: String): String = raw.filter { it.isDigit() }

    fun isValidSixDigitCode(raw: String): Boolean = normalizeSixDigitCode(raw).length == 6

    fun formatSixDigitCode(raw: String): String {
        val digits = normalizeSixDigitCode(raw)
        return if (digits.length == 6) {
            "${digits.substring(0, 3)} ${digits.substring(3, 6)}"
        } else {
            digits
        }
    }

    fun generateSixDigitCode(random: SecureRandom = SecureRandom()): String {
        return String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000))
    }

    fun computeTokenHash(email: String, code: String): String {
        val normalizedEmail = normalizeEmail(email)
        val normalizedCode = normalizeSixDigitCode(code)
        require(isValidEmail(normalizedEmail)) { "Please enter a valid shop email address." }
        require(normalizedCode.length == 6) { "Please enter a valid 6-digit link code." }

        val input = "$normalizedEmail:$normalizedCode".toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(input)
        val hex = StringBuilder(digest.size * 2)
        for (byte in digest) {
            val v = byte.toInt() and 0xFF
            hex.append(HEX_CHARS[v ushr 4])
            hex.append(HEX_CHARS[v and 0x0F])
        }
        return hex.toString()
    }

    fun resolveDeviceDisplayName(): String {
        val manufacturer = (Build.MANUFACTURER ?: "").trim()
        val model = (Build.MODEL ?: "").trim()
        val combined = when {
            manufacturer.isEmpty() && model.isEmpty() -> "Android Device"
            model.lowercase(Locale.ROOT).startsWith(manufacturer.lowercase(Locale.ROOT)) -> model
            manufacturer.isEmpty() -> model
            model.isEmpty() -> manufacturer
            else -> "$manufacturer $model"
        }
        return combined
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            .take(MAX_DEVICE_NAME_LENGTH)
            .ifBlank { "Android Device" }
    }

    fun formatRemainingTime(remainingMillis: Long): String {
        val safeSeconds = maxOf(0L, remainingMillis / 1000L)
        val minutes = safeSeconds / 60L
        val seconds = safeSeconds % 60L
        return String.format(Locale.ROOT, "%dm %02ds", minutes, seconds)
    }

    private const val HEX_CHARS = "0123456789abcdef"
}
