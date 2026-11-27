package com.example.my_kmp_project.component.auth

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * 第 0 批：平台取码能力注册表的行为。
 *
 * 内置适配器在三端均为 null（尚未接 SDK），因此这里只验证显式注册项：
 * 注册后可查、注销后消失、同 id 覆盖、清理后回到空。
 */
class PlatformLoginCapabilitiesTest {

    @AfterTest
    fun tearDown() {
        PlatformLoginCapabilities.clear()
    }

    @Test
    fun registeredProviderIsReturnedByMethodId() {
        val provider = fakeProvider("wechat")

        PlatformLoginCapabilities.register(provider)

        assertSame(provider, PlatformLoginCapabilities.providerOrNull("wechat"))
        assertNull(PlatformLoginCapabilities.providerOrNull("qq"))
    }

    @Test
    fun unregisterRemovesProvider() {
        PlatformLoginCapabilities.register(fakeProvider("qq"))

        PlatformLoginCapabilities.unregister("qq")

        assertNull(PlatformLoginCapabilities.providerOrNull("qq"))
    }

    @Test
    fun registeringSameMethodReplacesPreviousProvider() {
        val first = fakeProvider("wechat")
        val second = fakeProvider("wechat")

        PlatformLoginCapabilities.register(first)
        PlatformLoginCapabilities.register(second)

        assertSame(second, PlatformLoginCapabilities.providerOrNull("wechat"))
    }

    @Test
    fun clearDropsAllExplicitRegistrations() {
        PlatformLoginCapabilities.register(fakeProvider("wechat"))
        PlatformLoginCapabilities.register(fakeProvider("qq"))

        PlatformLoginCapabilities.clear()

        assertNull(PlatformLoginCapabilities.providerOrNull("wechat"))
        assertNull(PlatformLoginCapabilities.providerOrNull("qq"))
    }

    private fun fakeProvider(methodId: String): AuthorizationCodeProvider =
        object : AuthorizationCodeProvider {
            override val methodId: String = methodId

            override suspend fun requestCode(): AuthorizationCodeResult =
                AuthorizationCodeResult.Success("code-$methodId")
        }
}
