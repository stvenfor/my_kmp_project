package com.example.my_kmp_project.core.account

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

/**
 * Shared [PrivacyConsentStore] backed by multiplatform-settings.
 * Used by Android / iOS actuals (not linked on OHOS).
 */
public class SettingsPrivacyConsentStore(
    private val settings: Settings,
) : PrivacyConsentStore {
    override fun isAccepted(): Boolean =
        settings.getBoolean(PRIVACY_ACCEPTED_KEY, defaultValue = false)

    override fun setAccepted(accepted: Boolean) {
        if (accepted) {
            settings[PRIVACY_ACCEPTED_KEY] = true
        } else {
            settings.remove(PRIVACY_ACCEPTED_KEY)
        }
    }
}
