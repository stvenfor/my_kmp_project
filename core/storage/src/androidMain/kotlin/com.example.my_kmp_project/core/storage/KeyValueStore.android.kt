package com.example.my_kmp_project.core.storage

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings

/**
 * Android actual: one SharedPreferences file per store name, MODE_PRIVATE.
 *
 * Store names are the pre-existing file names (demo_account, home_favorites), so values
 * already on a device stay readable.
 */
public actual fun createKeyValueStore(name: String): KeyValueStore {
    val prefs = AndroidAppContext.requireContext()
        .getSharedPreferences(name, Context.MODE_PRIVATE)
    return SettingsKeyValueStore(SharedPreferencesSettings(prefs))
}
