@file:Suppress("DEPRECATION")

package com.example.my_kmp_project.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * KMP 客户端与 Go 后端 `my_go_study` 之间「纯双方」的错误码 / 网络协议契约
 * （真相源：`app/internal/delivery/http/dto/response/common.go:12-29`）。
 *
 * 覆盖：A1 五位数码值、A2 isSuccess 收紧、A3 解析器逐码分类动作。
 */
class BackendCodeContractTest {

    // ---- A1：全部五位数码与 Go 常量逐一一致 ----

    @Test
    fun backend_codes_match_go_common_go() {
        assertEquals(0, BackendCodes.SUCCESS)
        assertEquals(10001, BackendCodes.INVALID_PARAMS)
        assertEquals(10002, BackendCodes.UNAUTHORIZED)
        assertEquals(10003, BackendCodes.FORBIDDEN)
        assertEquals(10004, BackendCodes.NOT_FOUND)
        assertEquals(10005, BackendCodes.NOT_IMPLEMENTED)
        assertEquals(10021, BackendCodes.SESSION_REPLACED)
        assertEquals(10022, BackendCodes.SESSION_INVALID)
        assertEquals(10023, BackendCodes.TOKEN_INVALID)
        assertEquals(10024, BackendCodes.INVALID_OTP)
        assertEquals(50000, BackendCodes.INTERNAL_ERROR)
        // 遗留 OK=200 仍保留兼容值，但已标注 @Deprecated。
        assertEquals(200, NetworkCodes.OK)
        assertEquals(0, NetworkCodes.BFF_OK)
    }

    // ---- A2：isSuccess 只认 BFF_OK(0) ----

    @Test
    fun bff_zero_is_success() {
        assertTrue(ApiResponse<Unit>(code = NetworkCodes.BFF_OK).isSuccess)
    }

    @Test
    fun legacy_200_is_not_success() {
        assertFalse(ApiResponse<Unit>(code = 200).isSuccess)
    }

    @Test
    fun passthrough_minus_one_is_not_success() {
        assertFalse(ApiResponse<Unit>(code = NetworkCodes.PASSTHROUGH).isSuccess)
    }

    // ---- A3：解析器 when(code) 逐码分类动作 ----

    @Test
    fun business_codes_map_to_business_without_token_or_forbidden_handlers() {
        listOf(
            BackendCodes.INVALID_PARAMS,
            BackendCodes.UNAUTHORIZED,
            BackendCodes.NOT_FOUND,
            BackendCodes.INVALID_OTP,
            BackendCodes.INTERNAL_ERROR,
        ).forEach { code ->
            var expired = 0
            var forbidden = 0
            val errors = mutableListOf<NetworkError>()
            val resp = ApiEnvelopeParser.parse(
                raw = """{"code":$code,"message":"msg"}""",
                parseData = { null },
                onTokenExpired = TokenExpiredHandler { expired++ },
                businessHandlers = NetworkBusinessHandlers(onForbidden = { forbidden++ }),
                onError = { errors += it },
            )
            assertEquals(code, resp.code, "code=$code")
            assertFalse(resp.isSuccess, "code=$code")
            assertEquals(0, expired, "code=$code 不应触发 onTokenExpired（不登出 / 刷新）")
            assertEquals(0, forbidden, "code=$code 不应触发 onForbidden")
            assertEquals(1, errors.size, "code=$code 应分类一次")
            val err = errors.single()
            assertTrue(err is NetworkError.Business, "code=$code 应为 Business 分类")
            assertTrue(err.code == code, "code=$code 分类应携带原码")
        }
    }

    @Test
    fun code_10005_maps_to_not_implemented_non_retryable() {
        var expired = 0
        var forbidden = 0
        val errors = mutableListOf<NetworkError>()
        val resp = ApiEnvelopeParser.parse(
            raw = """{"code":10005,"message":"能力未接入"}""",
            parseData = { null },
            onTokenExpired = TokenExpiredHandler { expired++ },
            businessHandlers = NetworkBusinessHandlers(onForbidden = { forbidden++ }),
            onError = { errors += it },
        )
        assertEquals(BackendCodes.NOT_IMPLEMENTED, resp.code)
        assertFalse(resp.isSuccess)
        // 不重试 / 不刷新 / 不登出：不触碰任何相关 handler。
        assertEquals(0, expired)
        assertEquals(0, forbidden)
        val err = errors.single()
        assertTrue(err is NetworkError.NotImplemented)
        assertFalse((err as NetworkError.NotImplemented).retryable)
        assertTrue(err.code == BackendCodes.NOT_IMPLEMENTED)
    }

    @Test
    fun session_and_token_codes_are_not_decided_at_parser_layer() {
        // 10021 / 10022 / 10023：本层不重复决策，交 feature/auth 的 AuthRefreshingApiClient 处理。
        listOf(
            BackendCodes.SESSION_REPLACED,
            BackendCodes.SESSION_INVALID,
            BackendCodes.TOKEN_INVALID,
        ).forEach { code ->
            var expired = 0
            var forbidden = 0
            val errors = mutableListOf<NetworkError>()
            val resp = ApiEnvelopeParser.parse(
                raw = """{"code":$code,"message":"msg"}""",
                parseData = { null },
                onTokenExpired = TokenExpiredHandler { expired++ },
                businessHandlers = NetworkBusinessHandlers(onForbidden = { forbidden++ }),
                onError = { errors += it },
            )
            assertEquals(code, resp.code, "code=$code")
            assertFalse(resp.isSuccess, "code=$code")
            assertEquals(0, expired, "code=$code 由 feature/auth 装饰器决策，本层不登出")
            assertEquals(0, forbidden, "code=$code 本层不判无权限")
            assertTrue(errors.isEmpty(), "code=$code 本层不分类、不重复决策")
        }
    }

    @Test
    fun code_10003_is_not_treated_as_forbidden() {
        // 10003 歧义（后端 CodeForbidden / KMP 账号未注册）：只认 HTTP 403，不按 code 判 Forbidden。
        var expired = 0
        var forbidden = 0
        val errors = mutableListOf<NetworkError>()
        val resp = ApiEnvelopeParser.parse(
            raw = """{"code":10003,"message":"无权限"}""",
            parseData = { null },
            onTokenExpired = TokenExpiredHandler { expired++ },
            businessHandlers = NetworkBusinessHandlers(onForbidden = { forbidden++ }),
            onError = { errors += it },
        )
        assertEquals(BackendCodes.FORBIDDEN, resp.code)
        assertFalse(resp.isSuccess)
        assertEquals(0, forbidden, "10003 歧义码不得按 Forbidden 处理（只认 HTTP 403）")
        assertEquals(0, expired)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun only_http_403_triggers_forbidden() {
        // 对照：HTTP 403（无业务 code 体）仍走 Forbidden，与 10003 显式分支互不干扰。
        var forbidden = 0
        val resp = ApiEnvelopeParser.parse(
            raw = """{"message":"无权限访问"}""",
            parseData = { null },
            businessHandlers = NetworkBusinessHandlers(onForbidden = { forbidden++ }),
            httpStatus = 403,
        )
        assertEquals(403, resp.code)
        assertEquals(1, forbidden)
    }

    @Test
    fun not_implemented_declares_non_retryable_semantics() {
        val err = NetworkError.NotImplemented()
        assertFalse(err.retryable, "能力未接入不可重试")
        assertTrue(err.code == BackendCodes.NOT_IMPLEMENTED)
    }
}
