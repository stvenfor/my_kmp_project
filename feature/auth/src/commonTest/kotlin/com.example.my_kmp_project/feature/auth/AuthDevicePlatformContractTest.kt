package com.example.my_kmp_project.feature.auth

import com.example.my_kmp_project.core.network.DemoRequestHeaders
import com.example.my_kmp_project.core.network.NetworkConfig
import com.example.my_kmp_project.core.network.NetworkJson
import com.example.my_kmp_project.core.network.platformClientType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B1：三端 body.platform 口径（Go 后端 `user_request.go:5-53` 的
 * `binding:"required,oneof=android ios"`）。
 */
class AuthDevicePlatformContractTest {

    private lateinit var savedClientType: String

    @BeforeTest fun save() { savedClientType = NetworkConfig.clientType }

    @AfterTest fun restore() { NetworkConfig.clientType = savedClientType }

    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.content

    @Test
    fun platform_for_body_maps_three_targets_to_android_or_ios() {
        assertEquals("android", AuthDeviceContext.platformForBody("android"))
        assertEquals("ios", AuthDeviceContext.platformForBody("ios"))
        assertEquals("ios", AuthDeviceContext.platformForBody("iphone"))
        assertEquals("ios", AuthDeviceContext.platformForBody("ipad"))
        assertEquals("android", AuthDeviceContext.platformForBody("ohos"))
        assertEquals("android", AuthDeviceContext.platformForBody("harmony"))
        assertEquals("android", AuthDeviceContext.platformForBody("HarmonyOS"))
        assertEquals("android", AuthDeviceContext.platformForBody("unknown"))
    }

    @Test
    fun body_platform_only_ever_lands_on_android_or_ios() {
        // 后端 oneof=android ios：任何 Client-Type 真实值映射出的 body.platform 都只能落在这两个值。
        for (clientType in listOf("android", "ios", "ohos", "harmony", "iphone", "ipad", "unknown", "")) {
            assertTrue(
                AuthDeviceContext.platformForBody(clientType) in setOf("android", "ios"),
                "clientType=$clientType",
            )
        }
    }

    @Test
    fun platform_delegates_to_platform_for_body_with_real_client_type() {
        // platform() 只把真实 Client-Type 交给纯映射，不再自己写一套 when。
        assertEquals(AuthDeviceContext.platformForBody(platformClientType()), AuthDeviceContext.platform())
    }

    @Test
    fun client_type_header_and_body_platform_are_independent() {
        // OHOS 真机：Client-Type 头用真实值 ohos；body.platform 因后端 oneof 映射成 android。
        NetworkConfig.clientType = "ohos"
        val headers = DemoRequestHeaders.forUrl("https://api.example.com/api/v1/user/login")
        assertEquals("ohos", headers["Client-Type"])
        assertEquals("android", AuthDeviceContext.platformForBody("ohos"))
        assertTrue(headers["Client-Type"] != AuthDeviceContext.platformForBody("ohos"))
        // body 映射不改写 Client-Type 全局值。
        AuthDeviceContext.platformForBody("ohos")
        assertEquals("ohos", NetworkConfig.clientType)
    }

    @Test
    fun huawei_login_default_platform_uses_device_context_not_hardcoded_android() {
        val request = AuthHuaweiLoginRequest(code = "hw-code", deviceId = "dev-1")
        assertEquals(AuthDeviceContext.platform(), request.platform)
        assertTrue(request.platform in setOf("android", "ios"))
    }

    @Test
    fun huawei_login_body_uses_snake_case_device_id_and_mapped_platform() {
        val body = NetworkJson.parseToJsonElement(
            NetworkJson.encodeToString(
                AuthHuaweiLoginRequest.serializer(),
                AuthHuaweiLoginRequest(code = "hw-code", deviceId = "dev-1"),
            ),
        ) as JsonObject
        assertEquals(setOf("code", "device_id", "platform"), body.keys)
        assertEquals("hw-code", body.text("code"))
        assertEquals("dev-1", body.text("device_id"))
        assertTrue(body.text("platform") in setOf("android", "ios"))
    }
}
