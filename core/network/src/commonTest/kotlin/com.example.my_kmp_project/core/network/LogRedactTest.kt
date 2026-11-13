package com.example.my_kmp_project.core.network

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/** A7：`redact()` 移除会话头与 token 字面值，且不误伤非敏感日志。 */
class LogRedactTest {

    private lateinit var savedToken: String
    private lateinit var savedSessionId: String
    private lateinit var savedDeviceId: String

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
    fun redact_removes_session_headers_and_token_literals() {
        NetworkConfig.accessToken = "jwt-secret-token"
        NetworkConfig.sessionId = "sid-secret-value"
        NetworkConfig.deviceId = "did-secret-value"
        val log = "REQUEST: https://api.example.com/ws\n" +
            "COMMON HEADERS\n" +
            "-> Authorization: Bearer jwt-secret-token\n" +
            "-> X-Session-ID: sid-secret-value\n" +
            "-> X-Device-ID: did-secret-value\n" +
            "-> Accept: application/json\n" +
            "BODY: token=jwt-secret-token&sid=sid-secret-value"
        val out = redact(log)
        assertTrue(!out.contains("jwt-secret-token"), out)
        assertTrue(!out.contains("sid-secret-value"), out)
        assertTrue(!out.contains("did-secret-value"), out)
        assertTrue(out.contains("[REDACTED]"), out)
        // 非敏感头原样保留。
        assertTrue(out.contains("-> Accept: application/json"), out)
        // 头名保留、值脱敏。
        assertTrue(out.contains("Authorization: [REDACTED]"), out)
    }

    @Test
    fun redact_scrubs_header_values_even_when_secrets_are_blank() {
        NetworkConfig.accessToken = ""
        NetworkConfig.sessionId = ""
        NetworkConfig.deviceId = ""
        val log = "-> Authorization: Bearer anything\n-> X-Session-ID: whatever\n-> X-Device-ID: xyz"
        val out = redact(log)
        assertTrue(!out.contains("Bearer anything"), out)
        assertTrue(!out.contains("whatever"), out)
        assertTrue(!out.contains("xyz"), out)
        assertTrue(out.contains("Authorization: [REDACTED]"), out)
    }

    @Test
    fun redact_keeps_non_sensitive_lines_unchanged() {
        NetworkConfig.accessToken = "tok"
        NetworkConfig.sessionId = ""
        NetworkConfig.deviceId = ""
        val line = "GET https://api.example.com/ping -> 200 OK"
        assertTrue(redact(line) == line, redact(line))
    }
}
