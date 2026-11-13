package com.example.my_kmp_project.feature.auth

import com.example.my_kmp_project.core.network.ApiEnvelope
import com.example.my_kmp_project.core.network.DemoRequestHeaders
import com.example.my_kmp_project.core.network.NetworkConfig
import com.example.my_kmp_project.core.network.NetworkJson
import com.example.my_kmp_project.core.network.dataOrNull
import com.example.my_kmp_project.core.network.decodeEnvelopeData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * B5：锁死 4 项已对齐事实，确保 B1–B4 没有回归：
 * 会话三件套绑定、logout Server-Confirmed、session_id 回填、snake_case 请求体。
 */
class AuthAlignmentRegressionTest {

    private lateinit var savedToken: String
    private lateinit var savedSessionId: String
    private lateinit var savedDeviceId: String

    @BeforeTest fun save() {
        savedToken = NetworkConfig.accessToken
        savedSessionId = NetworkConfig.sessionId
        savedDeviceId = NetworkConfig.deviceId
    }

    @AfterTest fun restore() {
        NetworkConfig.accessToken = savedToken
        NetworkConfig.sessionId = savedSessionId
        NetworkConfig.deviceId = savedDeviceId
    }

    private inline fun <reified T> dataOf(raw: String): T? =
        decodeEnvelopeData<T>(NetworkJson.decodeFromString(ApiEnvelope.serializer(), raw).dataOrNull())

    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.content

    private inline fun <reified T> bodyOf(value: T): JsonObject =
        NetworkJson.parseToJsonElement(NetworkJson.encodeToString(value)) as JsonObject

    // 1) 会话三件套绑定 -------------------------------------------------------

    @Test
    fun session_trio_headers_are_injected_together() {
        NetworkConfig.accessToken = "AT"
        NetworkConfig.sessionId = "SID"
        NetworkConfig.deviceId = "DEV"
        val headers = DemoRequestHeaders.forUrl("https://api.example.com/api/v1/user/logout")
        assertEquals("Bearer AT", headers["Authorization"])
        assertEquals("SID", headers[DemoRequestHeaders.HEADER_SESSION_ID])
        assertEquals("DEV", headers[DemoRequestHeaders.HEADER_DEVICE_ID])
        assertEquals("X-Session-ID", DemoRequestHeaders.HEADER_SESSION_ID)
        assertEquals("X-Device-ID", DemoRequestHeaders.HEADER_DEVICE_ID)
    }

    // 2) logout Server-Confirmed ---------------------------------------------

    @Test
    fun logout_only_clears_when_session_is_confirmed_gone() {
        assertTrue(
            AuthRepository.isLogoutSessionGone(
                AuthHttpFailure("未登录", code = AuthBizCode.UNAUTHORIZED),
            ),
        )
        assertTrue(
            AuthRepository.isLogoutSessionGone(
                AuthHttpFailure("会话无效", code = AuthBizCode.SESSION_INVALID),
            ),
        )
        assertTrue(AuthRepository.isLogoutSessionGone(AuthHttpFailure("token 已过期")))
        // 网络等失败：保留本地会话，可重试。
        assertFalse(AuthRepository.isLogoutSessionGone(AuthHttpFailure("无法连接服务端")))
        assertFalse(AuthRepository.isLogoutSessionGone(IllegalStateException("超时")))
    }

    // 3) session_id 回填 -----------------------------------------------------

    @Test
    fun login_and_refresh_results_backfill_session_id() {
        val login = dataOf<AuthLoginData>(
            """{"code":0,"data":{"token":"AT","refresh_token":"RT","session_id":"SID","user":{"id":"u-1"}}}""",
        )!!
        assertEquals("SID", login.sessionId)
        assertEquals("SID", login.toPayload().sessionId)

        val refresh = dataOf<AuthRefreshData>(
            """{"code":0,"data":{"token":"AT2","refresh_token":"RT2","session_id":"SID2"}}""",
        )!!
        assertEquals("SID2", refresh.sessionId)

        val register = dataOf<AuthRegisterData>(
            """{"code":0,"data":{"token":"AT","session_id":"SID3","user":{"id":"u-2"}}}""",
        )!!
        assertEquals("SID3", register.toPayload().sessionId)
    }

    // 4) snake_case 请求体 ---------------------------------------------------

    @Test
    fun auth_request_bodies_keep_snake_case_wire_names() {
        val login = bodyOf(
            AuthPasswordLoginRequest(username = "a", password = "p", deviceId = "d", platform = "android"),
        )
        assertEquals(setOf("username", "password", "device_id", "platform"), login.keys)

        val refresh = bodyOf(AuthRefreshRequest.of("RT", "d", "SID", "ios"))
        assertEquals(setOf("refresh_token", "device_id", "session_id", "platform"), refresh.keys)
        assertEquals("RT", refresh.text("refresh_token"))

        // B1 改了默认值，仍须保证 wire 名与「只取默认 platform」不回归。
        val huawei = bodyOf(AuthHuaweiLoginRequest(code = "c", deviceId = "d"))
        assertEquals(setOf("code", "device_id", "platform"), huawei.keys)
        assertEquals("d", huawei.text("device_id"))
    }
}
