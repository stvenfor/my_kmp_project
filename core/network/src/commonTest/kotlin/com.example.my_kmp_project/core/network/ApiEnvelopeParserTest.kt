package com.example.my_kmp_project.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class ApiEnvelopeParserTest {

    @Test
    fun success_parses_data() {
        val raw = """{"code":200,"message":"ok","data":"hello"}"""
        val resp = ApiEnvelopeParser.parse(raw, parseData = { it?.jsonPrimitive?.contentOrNull })
        assertEquals(NetworkCodes.OK, resp.code)
        assertTrue(resp.isSuccess)
        assertEquals("hello", resp.data)
    }

    @Test
    fun bff_code_zero_is_success() {
        // Go BFF `/api/v1` 成功码是 0（Dart result_model.dart:28），不是遗留表的 200。
        val raw = """{"code":0,"message":"success","data":{"token":"abc"}}"""

        @kotlinx.serialization.Serializable
        data class Payload(val token: String = "")

        val resp = ApiEnvelopeParser.parse(raw, parseData = { decodeEnvelopeData<Payload>(it) })
        assertEquals(NetworkCodes.BFF_OK, resp.code)
        assertTrue(resp.isSuccess)
        assertEquals("abc", resp.data?.token)
    }

    @Test
    fun invalid_json_returns_passthrough() {
        val resp = ApiEnvelopeParser.parse("not-json", parseData = { null })
        assertEquals(NetworkCodes.PASSTHROUGH, resp.code)
        assertTrue(!resp.isSuccess)
        assertEquals("响应格式错误", resp.message)
        assertNull(resp.data)
    }

    @Test
    fun missing_code_is_not_success() {
        // 缺 code 的合法 JSON 也不能算成功（Dart `code ?? -1`）。
        val resp = ApiEnvelopeParser.parse("""{"message":"hi","data":1}""", parseData = { null })
        assertEquals(NetworkCodes.PASSTHROUGH, resp.code)
        assertTrue(!resp.isSuccess)
    }

    @Test
    fun expire_token_invokes_handler() {
        var called = false
        val raw = """{"code":401,"message":"expired","data":null}"""
        val resp = ApiEnvelopeParser.parse(
            raw = raw,
            parseData = { null },
            onTokenExpired = TokenExpiredHandler { called = true },
        )
        assertEquals(NetworkCodes.EXPIRE_TOKEN, resp.code)
        assertTrue(called)
    }

    @Test
    fun busy_invokes_business_handler() {
        var called = false
        val raw = """{"code":429,"message":"busy","data":null}"""
        val resp = ApiEnvelopeParser.parse(
            raw = raw,
            parseData = { null },
            businessHandlers = NetworkBusinessHandlers(
                onBusy = { called = true },
            ),
        )
        assertEquals(NetworkCodes.BUSY, resp.code)
        assertTrue(called)
    }

    @Test
    fun http_401_oauth_body_maps_to_expire_token() {
        var called = false
        val raw = """{"error":"token为空","error_description":"token为空"}"""
        val resp = ApiEnvelopeParser.parse(
            raw = raw,
            parseData = { null },
            onTokenExpired = TokenExpiredHandler { called = true },
            httpStatus = 401,
        )
        assertEquals(NetworkCodes.EXPIRE_TOKEN, resp.code)
        assertEquals("token为空", resp.message)
        assertTrue(called)
    }

    @Test
    fun string_code_200_parses_ok() {
        val raw = """{"code":"200","message":"ok","data":null}"""
        val resp = ApiEnvelopeParser.parse(raw, parseData = { null })
        assertEquals(NetworkCodes.OK, resp.code)
        assertTrue(resp.isSuccess)
    }

    // ---- Flutter `forbidden_status_test.dart` parity (d7876a1) ----

    @Test
    fun http_403_invokes_forbidden_not_token_expired() {
        var forbiddenCalls = 0
        var expiredCalls = 0
        val raw = """{"message":"无权限访问"}"""
        val resp = ApiEnvelopeParser.parse(
            raw = raw,
            parseData = { null },
            onTokenExpired = TokenExpiredHandler { expiredCalls++ },
            businessHandlers = NetworkBusinessHandlers(onForbidden = { forbiddenCalls++ }),
            httpStatus = NetworkCodes.FORBIDDEN,
        )
        assertEquals(NetworkCodes.FORBIDDEN, resp.code)
        assertEquals(NetworkCodes.FORBIDDEN, resp.httpStatus)
        assertEquals(1, forbiddenCalls)
        assertEquals(0, expiredCalls)
    }

    @Test
    fun business_code_403_invokes_forbidden_not_token_expired() {
        var forbiddenCalls = 0
        var expiredCalls = 0
        val raw = """{"code":403,"message":"无权限","data":null}"""
        val resp = ApiEnvelopeParser.parse(
            raw = raw,
            parseData = { null },
            onTokenExpired = TokenExpiredHandler { expiredCalls++ },
            businessHandlers = NetworkBusinessHandlers(onForbidden = { forbiddenCalls++ }),
        )
        assertEquals(NetworkCodes.FORBIDDEN, resp.code)
        assertEquals(1, forbiddenCalls)
        assertEquals(0, expiredCalls)
    }

    @Test
    fun forbidden_is_not_token_expired_semantics() {
        // 403 必须与 401 语义互斥：isSuccess=false，但不走 TokenExpired 分支。
        val raw = """{"code":403,"message":"无权限","data":null}"""
        val resp = ApiEnvelopeParser.parse(raw, parseData = { null })
        assertEquals(NetworkCodes.FORBIDDEN, resp.code)
        assertTrue(!resp.isSuccess)
    }

    @Test
    fun http_status_propagates_into_response() {
        val raw = """{"code":500,"message":"boom","data":null}"""
        val resp = ApiEnvelopeParser.parse(raw, parseData = { null }, httpStatus = 500)
        assertEquals(500, resp.code)
        assertEquals(500, resp.httpStatus)
    }

    @Test
    fun typed_object_data_via_decodeEnvelopeData() {
        @kotlinx.serialization.Serializable
        data class Sample(val token: String = "", val nested: Int = 0)

        val raw = """{"code":200,"message":"ok","data":{"token":"abc","nested":1}}"""
        val resp = ApiEnvelopeParser.parse(raw, parseData = { decodeEnvelopeData<Sample>(it) })
        assertEquals(NetworkCodes.OK, resp.code)
        assertEquals("abc", resp.data?.token)
        assertEquals(1, resp.data?.nested)
    }
}
