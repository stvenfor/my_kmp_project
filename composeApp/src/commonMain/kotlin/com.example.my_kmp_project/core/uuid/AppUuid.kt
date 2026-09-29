package com.example.my_kmp_project.core.uuid

/**
 * Shared UUID facade for feature/core code.
 * Android / iOS / ohosArm64: CPF `com.benasher44:uuid` via uuidCpfMain.
 * ohosX64: documented stub (no published ohosX64 klib).
 *
 * Call sites MUST use this API; do not import `com.benasher44.uuid` from features.
 */
expect object AppUuid {
    /** Random UUID v4 as lowercase 8-4-4-4-12 hex string. */
    fun random(): String

    /** Trim and parse; invalid input returns null; valid returns canonical lowercase string. */
    fun parseOrNull(value: String): String?
}
