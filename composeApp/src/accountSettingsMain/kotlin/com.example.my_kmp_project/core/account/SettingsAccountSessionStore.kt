package com.example.my_kmp_project.core.account

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

/**
 * Shared [AccountSessionStore] backed by multiplatform-settings.
 * Used by Android / iOS actuals (not linked on OHOS).
 */
internal class SettingsAccountSessionStore(
    private val settings: Settings,
) : AccountSessionStore {
    override fun readUserJson(): String? =
        settings.getStringOrNull(ACCOUNT_USER_JSON_KEY)

    override fun writeUserJson(json: String?) {
        if (json.isNullOrBlank()) {
            settings.remove(ACCOUNT_USER_JSON_KEY)
        } else {
            settings[ACCOUNT_USER_JSON_KEY] = json
        }
    }

    override fun clear() {
        settings.remove(ACCOUNT_USER_JSON_KEY)
    }
}

/** SharedPreferences / NSUserDefaults file name (Android prefs name). */
internal const val ACCOUNT_SETTINGS_NAME = "demo_account"
