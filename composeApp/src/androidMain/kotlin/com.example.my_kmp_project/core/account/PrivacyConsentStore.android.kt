package com.example.my_kmp_project.core.account

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings

internal actual fun createPrivacyConsentStore(): PrivacyConsentStore {
    val ctx = AndroidAccountContext.applicationContext
        ?: error("Call AndroidAccountContext.install(context) before PrivacyConsentStore use")
    val prefs = ctx.getSharedPreferences(ACCOUNT_SETTINGS_NAME, Context.MODE_PRIVATE)
    return SettingsPrivacyConsentStore(SharedPreferencesSettings(prefs))
}
