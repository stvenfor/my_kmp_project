package com.example.my_kmp_project.component.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * 第 0 批：注册表只做静态注册与稳定排序，不缓存可用性。
 */
class LoginStrategyRegistryTest {

    @Test
    fun descriptorsAreOrderedByOrderThenId() {
        val registry = DefaultLoginStrategyRegistry(
            listOf(
                fake("wechat", order = 30),
                fake("password", order = 10),
                fake("phone_otp", order = 20),
            ),
        )

        assertEquals(
            listOf("password", "phone_otp", "wechat"),
            registry.descriptors().map { it.id },
        )
    }

    @Test
    fun availableFiltersUnavailableButKeepsReasonInDescriptors() {
        val registry = DefaultLoginStrategyRegistry(
            listOf(
                fake("password"),
                fake(
                    id = "wechat",
                    availability = LoginAvailability.Unavailable("当前设备未接入微信 SDK"),
                ),
            ),
        )

        assertEquals(listOf("password"), registry.available().map { it.id })

        val wechat = registry.descriptors().single { it.id == "wechat" }
        assertFalse(wechat.available)
        assertEquals("当前设备未接入微信 SDK", wechat.unavailableReason)
    }

    @Test
    fun findReturnsRegisteredStrategyAndNullForUnknownId() {
        val password = fake("password")
        val registry = DefaultLoginStrategyRegistry(listOf(password))

        assertSame(password, registry.find("password"))
        assertNull(registry.find("qq"))
    }

    @Test
    fun duplicateIdsFailAtConstruction() {
        val error = assertFailsWith<IllegalArgumentException> {
            DefaultLoginStrategyRegistry(listOf(fake("wechat"), fake("wechat")))
        }

        assertTrue(error.message.orEmpty().contains("wechat"))
    }

    @Test
    fun interactiveFlagAndDisplayNameAreExposedToUi() {
        val registry = DefaultLoginStrategyRegistry(
            listOf(fake("wechat", displayName = "微信登录", interactive = true)),
        )

        val descriptor = registry.descriptors().single()
        assertEquals("微信登录", descriptor.displayName)
        assertTrue(descriptor.interactive)
        assertTrue(descriptor.available)
    }

    private fun fake(
        id: String,
        displayName: String = id,
        interactive: Boolean = false,
        order: Int = 100,
        availability: LoginAvailability = LoginAvailability.Available,
    ): LoginStrategy = object : LoginStrategy {
        override val id: String = id
        override val displayName: String = displayName
        override val interactive: Boolean = interactive
        override val order: Int = order

        override fun availability(): LoginAvailability = availability

        override suspend fun login(credentials: LoginCredentials): LoginOutcome =
            LoginOutcome.Failure(LoginError.Unknown("test fake"))
    }
}
