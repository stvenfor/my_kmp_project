package com.example.my_kmp_project.core.account

/**
 * Platform key-value session store.
 *
 * Android / iOS: multiplatform-settings (`SharedPreferences` / `NSUserDefaults`).
 * OHOS: file-backed actual (library has no ohosArm64 variant).
 */
internal interface AccountSessionStore {
    fun readUserJson(): String?
    fun writeUserJson(json: String?)
    fun clear()
}

internal expect fun createAccountSessionStore(): AccountSessionStore

internal const val ACCOUNT_USER_JSON_KEY = "demo_account_user_json"
