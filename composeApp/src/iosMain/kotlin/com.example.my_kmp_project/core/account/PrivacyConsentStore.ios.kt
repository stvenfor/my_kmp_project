package com.example.my_kmp_project.core.account

import com.russhwolf.settings.NSUserDefaultsSettings
import platform.Foundation.NSUserDefaults

internal actual fun createPrivacyConsentStore(): PrivacyConsentStore {
    val defaults = NSUserDefaults.standardUserDefaults
    return SettingsPrivacyConsentStore(NSUserDefaultsSettings(defaults))
}
