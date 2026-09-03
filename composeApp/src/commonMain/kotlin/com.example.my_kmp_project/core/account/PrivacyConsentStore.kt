package com.example.my_kmp_project.core.account

/**
 * Persists privacy-policy acceptance across process relaunch.
 *
 * Android / iOS: multiplatform-settings (`SharedPreferences` / `NSUserDefaults`).
 * OHOS: file-backed actual (library has no ohosArm64 variant).
 */
internal interface PrivacyConsentStore {
    fun isAccepted(): Boolean
    fun setAccepted(accepted: Boolean)
}

internal expect fun createPrivacyConsentStore(): PrivacyConsentStore

internal const val PRIVACY_ACCEPTED_KEY = "demo_privacy_accepted"
