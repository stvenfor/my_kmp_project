package com.example.my_kmp_project.core.uuid

import kotlin.random.Random

/**
 * ohosX64 stub: no CPF `uuid-ohosx64` klib.
 * Not product SoT — use ohosArm64 (CPF) for acceptance.
 */
actual object AppUuid {
    private val uuidRegex =
        Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    actual fun random(): String {
        val bytes = ByteArray(16).also { Random.nextBytes(it) }
        // RFC 4122 version 4 + variant 10xx
        bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x40).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()
        return formatUuid(bytes)
    }

    actual fun parseOrNull(value: String): String? {
        val trimmed = value.trim()
        if (!uuidRegex.matches(trimmed)) return null
        return trimmed.lowercase()
    }

    private fun formatUuid(bytes: ByteArray): String {
        require(bytes.size == 16)
        val hex = "0123456789abcdef"
        val chars = CharArray(36)
        var ci = 0
        fun writeByte(b: Byte) {
            val v = b.toInt() and 0xff
            chars[ci++] = hex[v ushr 4]
            chars[ci++] = hex[v and 0x0f]
        }
        for (i in 0 until 4) writeByte(bytes[i])
        chars[ci++] = '-'
        for (i in 4 until 6) writeByte(bytes[i])
        chars[ci++] = '-'
        for (i in 6 until 8) writeByte(bytes[i])
        chars[ci++] = '-'
        for (i in 8 until 10) writeByte(bytes[i])
        chars[ci++] = '-'
        for (i in 10 until 16) writeByte(bytes[i])
        return chars.concatToString()
    }
}
