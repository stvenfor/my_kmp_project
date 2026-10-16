package com.example.my_kmp_project.core.account

import com.example.my_kmp_project.core.storage.KeyValueStore
import com.example.my_kmp_project.core.storage.createKeyValueStore

/**
 * Session persistence for the logged-in user snapshot.
 *
 * Backed by :core:storage on every target (Android SharedPreferences / iOS NSUserDefaults /
 * OHOS Preferences), so there is one behaviour and one failure mode instead of a per-platform
 * actual each.
 */
public interface AccountSessionStore {
    fun readUserJson(): String?
    fun writeUserJson(json: String?)
    fun clear()
}

/** KV store name: the Android prefs file / OHOS preference file holding account state. */
public const val ACCOUNT_STORE_NAME: String = "demo_account"

public const val ACCOUNT_USER_JSON_KEY: String = "demo_account_user_json"

public fun createAccountSessionStore(): AccountSessionStore =
    KeyValueAccountSessionStore(createKeyValueStore(ACCOUNT_STORE_NAME))

/**
 * Blank and absent are the same state here: an empty payload cannot hydrate a session, so it
 * is stored as a removed key rather than as a string.
 */
internal class KeyValueAccountSessionStore(
    private val store: KeyValueStore,
) : AccountSessionStore {
    override fun readUserJson(): String? = store.getString(ACCOUNT_USER_JSON_KEY)

    override fun writeUserJson(json: String?) {
        if (json.isNullOrBlank()) {
            store.remove(ACCOUNT_USER_JSON_KEY)
        } else {
            store.putString(ACCOUNT_USER_JSON_KEY, json)
        }
    }

    override fun clear() {
        store.remove(ACCOUNT_USER_JSON_KEY)
    }
}
