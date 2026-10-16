package com.example.my_kmp_project.core.storage

/**
 * Wire format for booleans on backends that have no native boolean slot (the ohosX64 file
 * fallback) and for the in-memory store. The Settings backends use a real boolean slot, so
 * values written before this module existed still read correctly there.
 */
internal object BooleanValues {
    const val TRUE: String = "1"

    fun encode(value: Boolean): String = if (value) TRUE else "0"

    fun decode(raw: String?, defaultValue: Boolean): Boolean = when (raw) {
        null -> defaultValue
        TRUE, "true" -> true
        "0", "false" -> false
        else -> defaultValue
    }
}

/**
 * Volatile [KeyValueStore] used by commonTest (and by callers that must not crash when a
 * platform store cannot be created). Data is dropped as soon as the instance is gone.
 *
 * Not thread-safe by design: it exists to exercise the contract, not to back production
 * state.
 */
public class InMemoryKeyValueStore : KeyValueStore {
    private val values = mutableMapOf<String, String>()

    override fun getString(key: String): String? = values[key]

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        BooleanValues.decode(values[key], defaultValue)

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = BooleanValues.encode(value)
    }

    override fun getStringList(key: String): List<String>? = StringListValues.decode(values[key])

    override fun putStringList(key: String, values: List<String>) {
        this.values[key] = StringListValues.encode(values)
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}
