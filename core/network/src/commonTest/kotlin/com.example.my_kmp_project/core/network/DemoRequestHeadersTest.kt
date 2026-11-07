package com.example.my_kmp_project.core.network

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 逐请求会话头契约回归测试。
 *
 * 后端 my_go_study 的 `LocalSessionAuth` / `SupabaseSessionAuth`
 * （`app/internal/delivery/http/middleware/{local,supabase}_session_auth.go`）读取
 * `X-Session-ID` / `X-Device-ID` 并交给 `DeviceSessionUsecase.Validate` 做单设备登录校验；
 * `user_handler.go` 的 logout 亦读这两头。缺头会让真机上会话/设备口径与后端不一致。
 */
class DemoRequestHeadersTest {

    private lateinit var savedToken: String
    private lateinit var savedSessionId: String
    private lateinit var savedDeviceId: String

    private val apiUrl = "https://api.xiaomaomain.com/api/v1/user/logout"

    @BeforeTest
    fun save() {
        savedToken = NetworkConfig.accessToken
        savedSessionId = NetworkConfig.sessionId
        savedDeviceId = NetworkConfig.deviceId
    }

    @AfterTest
    fun restore() {
        NetworkConfig.accessToken = savedToken
        NetworkConfig.sessionId = savedSessionId
        NetworkConfig.deviceId = savedDeviceId
    }

    @Test
    fun logged_in_request_carries_all_three_session_headers() {
        NetworkConfig.accessToken = "access-abc"
        NetworkConfig.sessionId = "session-xyz"
        NetworkConfig.deviceId = "device-123"

        val headers = DemoRequestHeaders.forUrl(apiUrl)

        assertEquals("Bearer access-abc", headers["Authorization"])
        assertEquals("session-xyz", headers["X-Session-ID"])
        assertEquals("device-123", headers["X-Device-ID"])
        // 头名与后端 middleware 常量一致，不得自造。
        assertEquals("X-Session-ID", DemoRequestHeaders.HEADER_SESSION_ID)
        assertEquals("X-Device-ID", DemoRequestHeaders.HEADER_DEVICE_ID)
    }

    @Test
    fun logged_out_omits_authorization_and_session_headers() {
        NetworkConfig.accessToken = ""
        NetworkConfig.sessionId = "session-xyz"
        NetworkConfig.deviceId = "device-123"

        val headers = DemoRequestHeaders.forUrl(apiUrl)

        assertNull(headers["Authorization"])
        assertNull(headers["X-Session-ID"])
        assertNull(headers["X-Device-ID"])
        // 非鉴权头不受影响。
        assertEquals(NetworkConfig.clientType, headers["Client-Type"])
    }

    @Test
    fun skip_auth_host_omits_all_three_headers() {
        NetworkConfig.accessToken = "access-abc"
        NetworkConfig.sessionId = "session-xyz"
        NetworkConfig.deviceId = "device-123"

        val headers = DemoRequestHeaders.forUrl("https://cdn.demo.local/countrycode.json")

        assertNull(headers["Authorization"])
        assertNull(headers["X-Session-ID"])
        assertNull(headers["X-Device-ID"])
    }

    @Test
    fun blank_session_or_device_id_is_omitted() {
        NetworkConfig.accessToken = "access-abc"
        NetworkConfig.sessionId = "   "
        NetworkConfig.deviceId = ""

        val headers = DemoRequestHeaders.forUrl(apiUrl)

        assertEquals("Bearer access-abc", headers["Authorization"])
        assertNull(headers["X-Session-ID"])
        assertNull(headers["X-Device-ID"])
    }
}
