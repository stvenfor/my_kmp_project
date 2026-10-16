package com.example.my_kmp_project.core.storage

import com.russhwolf.settings.Settings

/**
 * [KeyValueStore] over multiplatform-settings. Used by the Android, iOS and ohosArm64
 * actuals; the library type stays inside this module.
 *
 * Lists go through [StringListValues] rather than a native string-set, so all three ends
 * persist exactly one item order and one empty-list representation.
 */
internal class SettingsKeyValueStore(
    private val settings: Settings,
) : KeyValueStore {
    override fun getString(key: String): String? = settings.getStringOrNull(key)

    override fun putString(key: String, value: String) {
        settings.putString(key, value)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        settings.getBoolean(key, defaultValue)

    override fun putBoolean(key: String, value: Boolean) {
        settings.putBoolean(key, value)
    }

    override fun getStringList(key: String): List<String>? =
        StringListValues.decode(settings.getStringOrNull(key))

    override fun putStringList(key: String, values: List<String>) {
        settings.putString(key, StringListValues.encode(values))
    }

    override fun remove(key: String) {
        settings.remove(key)
    }
}
