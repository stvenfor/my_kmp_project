package com.example.my_kmp_project.feature.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * `classifyAuthHttp` decision matrix — port of Flutter
 * `commons/wys_network/test/forbidden_status_test.dart` (d7876a1).
 */
class AuthHttpDecisionTest {

    // ---- forceLogout：业务码 10021 / 10022 ----

    @Test
    fun session_replaced_code_forces_logout() {
        assertEquals(
            AuthHttpDecision.ForceLogout,
            classifyAuthHttp(code = AuthBizCode.SESSION_REPLACED),
        )
    }

    @Test
    fun session_invalid_code_forces_logout() {
        assertEquals(
            AuthHttpDecision.ForceLogout,
            classifyAuthHttp(code = AuthBizCode.SESSION_INVALID),
        )
    }

    @Test
    fun legacy_message_without_code_forces_logout() {
        assertEquals(
            AuthHttpDecision.ForceLogout,
            classifyAuthHttp(code = null, message = "账号已在其他设备登录"),
        )
        assertEquals(
            AuthHttpDecision.ForceLogout,
            classifyAuthHttp(code = null, message = "会话无效"),
        )
    }

    @Test
    fun message_fallback_ignored_when_code_present() {
        // 有 code 时文案兜底不生效（非 forceLogout 码 + 互踢文案 → none）
        assertEquals(
            AuthHttpDecision.None,
            classifyAuthHttp(code = 10024, message = "其他设备登录"),
        )
    }

    // ---- forbidden：仅 HTTP 403 ----

    @Test
    fun http_403_is_forbidden_not_logout() {
        assertEquals(AuthHttpDecision.Forbidden, classifyAuthHttp(statusCode = 403))
        assertFalse(isAuthHttpHardFailure(statusCode = 403))
    }

    @Test
    fun code_10003_with_403_still_forbidden_but_keeps_register_copy() {
        // 403 → Forbidden（勿用 code==10003 反向判定 403；10003 与「账号未注册」共用）。
        // 消息层仍优先注册语义文案。
        assertEquals(
            AuthHttpDecision.Forbidden,
            classifyAuthHttp(statusCode = 403, code = 10003),
        )
        assertEquals(
            "账号未注册，请先注册",
            mapAuthFailure(code = 10003, message = null, statusCode = 403),
        )
    }

    @Test
    fun forbidden_map_keeps_register_copy_when_code_10003() {
        assertEquals("账号未注册，请先注册", mapAuthFailure(code = 10003, message = null, statusCode = 403))
        assertEquals("账号未注册，请先注册", mapAuthFailure(code = null, message = "账号未注册", statusCode = 403))
    }

    @Test
    fun forbidden_map_without_register_semantics() {
        assertEquals("暂无权限", mapAuthFailure(code = null, message = null, statusCode = 403))
    }

    // ---- tryRefresh：10023 / HTTP 401 / 过期文案兜底 ----

    @Test
    fun token_invalid_code_tries_refresh() {
        assertEquals(
            AuthHttpDecision.TryRefresh,
            classifyAuthHttp(code = AuthBizCode.TOKEN_INVALID),
        )
        assertTrue(isAuthHttpHardFailure(code = AuthBizCode.TOKEN_INVALID))
    }

    @Test
    fun http_401_tries_refresh() {
        assertEquals(AuthHttpDecision.TryRefresh, classifyAuthHttp(statusCode = 401))
        assertTrue(isAuthHttpHardFailure(statusCode = 401))
    }

    @Test
    fun expired_token_message_fallback() {
        for (msg in listOf(
            "token 无效",
            "token 已过期",
            "JWT verify failed",
            "jwt expired",
            "token expired",
            "Token expired",
            "未提供 Authorization header",
            "未授权",
        )) {
            assertEquals(AuthHttpDecision.TryRefresh, classifyAuthHttp(code = null, message = msg), msg)
        }
    }

    // ---- none ----

    @Test
    fun other_codes_are_none() {
        assertEquals(AuthHttpDecision.None, classifyAuthHttp(code = 10002))
        assertEquals(AuthHttpDecision.None, classifyAuthHttp(code = 10024))
        assertEquals(AuthHttpDecision.None, classifyAuthHttp(statusCode = 500))
        assertEquals(AuthHttpDecision.None, classifyAuthHttp(statusCode = 200, code = 0))
        assertEquals(AuthHttpDecision.None, classifyAuthHttp(message = "任意错误"))
    }

    @Test
    fun hard_failure_only_force_logout_or_try_refresh() {
        assertFalse(isAuthHttpHardFailure(statusCode = 403))
        assertFalse(isAuthHttpHardFailure(code = 10002))
        assertFalse(isAuthHttpHardFailure(code = 10024))
        assertTrue(isAuthHttpHardFailure(code = 10021))
        assertTrue(isAuthHttpHardFailure(code = 10022))
        assertTrue(isAuthHttpHardFailure(code = 10023))
        assertTrue(isAuthHttpHardFailure(statusCode = 401))
    }

    // ---- biz code predicates ----

    @Test
    fun biz_code_predicates() {
        assertTrue(AuthBizCode.isForceLogout(10021))
        assertTrue(AuthBizCode.isForceLogout(10022))
        assertFalse(AuthBizCode.isForceLogout(10023))
        assertTrue(AuthBizCode.isSessionReplaced(10021))
        assertFalse(AuthBizCode.isSessionReplaced(10022))
        assertTrue(AuthBizCode.isSessionGone(10023))
        assertTrue(AuthBizCode.isSessionGone(10021))
        assertFalse(AuthBizCode.isSessionGone(10002))
        assertTrue(AuthBizCode.isTryRefresh(10023))
        assertFalse(AuthBizCode.isTryRefresh(10021))
    }

    // ---- AuthFailure typed copy (Flutter auth_failure.dart) ----

    @Test
    fun auth_failure_typed_copy() {
        assertEquals("密码错误", AuthFailure.InvalidCredentials().message)
        assertEquals("账号未注册，请先注册", AuthFailure.AccountNotRegistered().message)
        assertEquals(
            "账号已在其他设备登录，请重新登录",
            AuthFailure.SessionReplaced().message,
        )
        assertEquals("会话无效，请重新登录", AuthFailure.SessionInvalid().message)
        assertEquals("登录已失效，请重新登录", AuthFailure.SessionCleared().message)
        assertEquals("验证码错误或已失效", AuthFailure.InvalidOtp().message)
        assertEquals("密码至少 6 位", AuthFailure.WeakPassword().message)
        assertEquals("网络异常，请稍后重试", AuthFailure.NetworkAuth().message)
    }

    @Test
    fun map_failure_routes_session_decisions() {
        assertEquals(
            "账号已在其他设备登录，请重新登录",
            mapAuthFailure(code = 10021, message = "其他设备登录"),
        )
        assertEquals("会话无效，请重新登录", mapAuthFailure(code = 10022, message = null))
        assertEquals("登录已失效，请重新登录", mapAuthFailure(code = 10023, message = null))
        assertEquals("登录已失效，请重新登录", mapAuthFailure(code = null, message = null, statusCode = 401))
    }

    @Test
    fun legacy_failure_copy_regression() {
        assertEquals("密码错误", mapAuthFailure(10002, null))
        assertEquals("验证码错误或已失效", mapAuthFailure(null, "验证码已失效"))
        assertEquals("该邮箱已注册", mapAuthFailure(null, "用户已存在"))
    }
}
