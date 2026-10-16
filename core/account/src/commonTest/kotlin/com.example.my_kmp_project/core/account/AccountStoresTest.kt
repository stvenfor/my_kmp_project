package com.example.my_kmp_project.core.account

import com.example.my_kmp_project.core.storage.InMemoryKeyValueStore
import com.example.my_kmp_project.core.storage.KeyValueStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The session and consent stores used to be one expect/actual per platform, and the OHOS pair
 * wrote to /tmp (lost on reboot). They are now common code over :core:storage, so these tests
 * pin the behaviour that replaced all three implementations -- including the key names, which
 * are what keeps data already on a device readable.
 */
class AccountSessionStoreTest {
    private val backing: KeyValueStore = InMemoryKeyValueStore()
    private val store: AccountSessionStore = KeyValueAccountSessionStore(backing)

    @Test
    fun storesSnapshotUnderTheLegacyKey() {
        store.writeUserJson("""{"token":"a"}""")
        assertEquals("""{"token":"a"}""", backing.getString(ACCOUNT_USER_JSON_KEY))
        assertEquals("""{"token":"a"}""", store.readUserJson())
    }

    @Test
    fun blankSnapshotMeansSignedOut() {
        store.writeUserJson("""{"token":"a"}""")
        store.writeUserJson("")
        assertNull(store.readUserJson())

        store.writeUserJson("""{"token":"a"}""")
        store.writeUserJson(null)
        assertNull(store.readUserJson())
    }

    @Test
    fun clearDropsTheSnapshot() {
        store.writeUserJson("""{"token":"a"}""")
        store.clear()
        assertNull(store.readUserJson())
        assertNull(backing.getString(ACCOUNT_USER_JSON_KEY))
    }
}

class PrivacyConsentStoreTest {
    private val backing: KeyValueStore = InMemoryKeyValueStore()
    private val store: PrivacyConsentStore = KeyValuePrivacyConsentStore(backing)

    @Test
    fun startsNotAccepted() {
        assertFalse(store.isAccepted())
    }

    @Test
    fun acceptanceIsPersistedUnderTheLegacyKey() {
        store.setAccepted(true)
        assertTrue(backing.getBoolean(PRIVACY_ACCEPTED_KEY, defaultValue = false))
        assertTrue(store.isAccepted())
    }

    @Test
    fun withdrawingAcceptanceIsRemembered() {
        store.setAccepted(true)
        store.setAccepted(false)
        assertFalse(store.isAccepted())
    }
}

/** Both account concerns share one store name, the Android prefs file they already used. */
class AccountStoreNameTest {
    @Test
    fun usesTheDemoAccountStore() {
        assertEquals("demo_account", ACCOUNT_STORE_NAME)
    }
}
