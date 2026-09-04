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
    fun invalid_json_returns_not_network() {
        val resp = ApiEnvelopeParser.parse("not-json", parseData = { null })
        assertEquals(NetworkCodes.NOT_NETWORK, resp.code)
        assertEquals("响应格式错误", resp.message)
        assertNull(resp.data)
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
}
