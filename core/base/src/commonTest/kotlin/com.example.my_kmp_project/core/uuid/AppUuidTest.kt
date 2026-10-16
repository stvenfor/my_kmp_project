package com.example.my_kmp_project.core.uuid

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppUuidTest {

    private val canonicalRegex =
        Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

    @Test
    fun random_isNonBlankCanonicalLowercase() {
        val value = AppUuid.random()
        assertTrue(value.isNotBlank())
        assertTrue(canonicalRegex.matches(value), "unexpected shape: $value")
    }

    @Test
    fun random_roundTripsThroughParse() {
        val generated = AppUuid.random()
        val parsed = AppUuid.parseOrNull(generated)
        assertNotNull(parsed)
        assertEquals(generated, parsed)
    }

    @Test
    fun successiveRandom_differ() {
        assertNotEquals(AppUuid.random(), AppUuid.random())
    }

    @Test
    fun parseOrNull_rejectsInvalid() {
        assertNull(AppUuid.parseOrNull(""))
        assertNull(AppUuid.parseOrNull("   "))
        assertNull(AppUuid.parseOrNull("not-a-uuid"))
    }
}
