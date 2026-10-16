package com.example.my_kmp_project.core.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Contract test for [KeyValueStore], run against the in-memory backend on every target
 * (Android / iOS / OHOS). The platform backends only replace how bytes reach disk, so this
 * pins the behaviour all three must agree on: absent vs empty, list order, and the wire
 * format that keeps already-saved Android values readable.
 */
class KeyValueStoreContractTest {
    private fun store(): KeyValueStore = InMemoryKeyValueStore()

    @Test
    fun absentKeyReadsNull() {
        val store = store()
        assertNull(store.getString("missing"))
        assertNull(store.getStringList("missing"))
    }

    @Test
    fun booleanFallsBackToDefaultWhenAbsent() {
        val store = store()
        assertTrue(store.getBoolean("dark_mode", defaultValue = true))
        store.putBoolean("dark_mode", false)
        assertEquals(false, store.getBoolean("dark_mode", defaultValue = true))
    }

    @Test
    fun stringRoundTripKeepsStructuredPayloads() {
        val store = store()
        val json = """{"nick":"阿龙","note":"line1\nline2"}"""
        store.putString("demo_account_user_json", json)
        assertEquals(json, store.getString("demo_account_user_json"))
    }

    @Test
    fun emptyStringIsNotAbsent() {
        val store = store()
        store.putString("locale", "")
        assertEquals("", store.getString("locale"))
    }

    @Test
    fun removeMakesKeyAbsentAgain() {
        val store = store()
        store.putString("locale", "zh")
        store.remove("locale")
        assertNull(store.getString("locale"))
    }

    @Test
    fun listRoundTripKeepsOrderAndEmpties() {
        val store = store()
        store.putStringList("home_favorite_service_ids_v2", listOf("3", "1", "2"))
        assertEquals(listOf("3", "1", "2"), store.getStringList("home_favorite_service_ids_v2"))

        store.putStringList("function_order", emptyList())
        assertEquals(emptyList(), store.getStringList("function_order"))
    }

    @Test
    fun separatorInsideValueCannotInventItems() {
        val store = store()
        store.putStringList("function_order", listOf("a${StringListValues.SEPARATOR}b", "c"))
        assertEquals(listOf("ab", "c"), store.getStringList("function_order"))
    }

    @Test
    fun listWireFormatMatchesTheLegacyAndroidEncoding() {
        // core.platform used to join with (char) 31 directly in SharedPreferences; the new
        // store must write the same bytes so on-device favorites keep loading.
        val ids = listOf("11", "22")
        assertEquals(
            ids.joinToString(StringListValues.SEPARATOR.toString()),
            StringListValues.encode(ids),
        )
        assertEquals(ids, StringListValues.decode(StringListValues.encode(ids)))
        assertNull(StringListValues.decode(null))
        assertEquals(emptyList(), StringListValues.decode(""))
    }
}
