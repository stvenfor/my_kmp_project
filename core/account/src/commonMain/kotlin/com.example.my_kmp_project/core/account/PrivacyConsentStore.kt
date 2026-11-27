package com.example.my_kmp_project.core.account

import com.example.my_kmp_project.core.storage.KeyValueStore
import com.example.my_kmp_project.core.storage.createKeyValueStore

/**
 * Persists privacy-policy acceptance across process relaunch.
 *
 * Lives in the same named store as the session ([ACCOUNT_STORE_NAME]): that is where Android
 * already kept it, so the accepted flag survives this refactor.
 */
public interface PrivacyConsentStore {
    fun isAccepted(): Boolean
    fun setAccepted(accepted: Boolean)
}

public const val PRIVACY_ACCEPTED_KEY: String = "demo_privacy_accepted"

public fun createPrivacyConsentStore(): PrivacyConsentStore =
    KeyValuePrivacyConsentStore(createKeyValueStore(ACCOUNT_STORE_NAME))

/** 由装配层注入的 KV 视图创建（DI：同一份存储绑定，不额外开持久化通道）。 */
public fun createPrivacyConsentStore(store: KeyValueStore): PrivacyConsentStore =
    KeyValuePrivacyConsentStore(store)

internal class KeyValuePrivacyConsentStore(
    private val store: KeyValueStore,
) : PrivacyConsentStore {
    override fun isAccepted(): Boolean = store.getBoolean(PRIVACY_ACCEPTED_KEY)

    override fun setAccepted(accepted: Boolean) {
        store.putBoolean(PRIVACY_ACCEPTED_KEY, accepted)
    }
}
