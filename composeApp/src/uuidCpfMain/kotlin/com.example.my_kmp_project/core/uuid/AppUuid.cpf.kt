package com.example.my_kmp_project.core.uuid

import com.benasher44.uuid.uuid4
import com.benasher44.uuid.uuidFrom

/** CPF benasher44 uuid actual for Android / iOS / ohosArm64 (uuidCpfMain). */
actual object AppUuid {
    actual fun random(): String = uuid4().toString()

    actual fun parseOrNull(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        return try {
            uuidFrom(trimmed).toString()
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
