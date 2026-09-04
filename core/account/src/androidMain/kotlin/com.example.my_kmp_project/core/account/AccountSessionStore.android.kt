package com.example.my_kmp_project.core.account

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import kotlin.concurrent.Volatile

public object AndroidAccountContext {
    @Volatile
    var applicationContext: Context? = null

    fun install(context: Context) {
        applicationContext = context.applicationContext
    }
}

public actual fun createAccountSessionStore(): AccountSessionStore {
    val ctx = AndroidAccountContext.applicationContext
        ?: error("Call AndroidAccountContext.install(context) before AccountFacade use")
    val prefs = ctx.getSharedPreferences(ACCOUNT_SETTINGS_NAME, Context.MODE_PRIVATE)
    return SettingsAccountSessionStore(SharedPreferencesSettings(prefs))
}
