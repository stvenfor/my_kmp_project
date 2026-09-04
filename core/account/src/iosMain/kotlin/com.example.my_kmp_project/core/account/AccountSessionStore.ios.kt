package com.example.my_kmp_project.core.account

import com.russhwolf.settings.NSUserDefaultsSettings
import platform.Foundation.NSUserDefaults

public actual fun createAccountSessionStore(): AccountSessionStore {
    val defaults = NSUserDefaults.standardUserDefaults
    return SettingsAccountSessionStore(NSUserDefaultsSettings(defaults))
}
