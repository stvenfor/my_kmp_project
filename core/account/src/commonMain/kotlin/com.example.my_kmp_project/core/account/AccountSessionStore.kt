package com.example.my_kmp_project.core.account

/**
 * Platform key-value session store.
 *
 * Android / iOS: multiplatform-settings (`SharedPreferences` / `NSUserDefaults`).
 * OHOS: file-backed actual (library has no ohosArm64 variant).
 */
public interface AccountSessionStore {
    fun readUserJson(): String?
    fun writeUserJson(json: String?)
    fun clear()
}

public expect fun createAccountSessionStore(): AccountSessionStore

public const val ACCOUNT_USER_JSON_KEY = "demo_account_user_json"
