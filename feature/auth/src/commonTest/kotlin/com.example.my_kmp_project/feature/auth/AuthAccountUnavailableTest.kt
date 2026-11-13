package com.example.my_kmp_project.feature.auth

import com.example.my_kmp_project.core.network.AuthRefreshDecision
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * B3：403「账号不可用」→ 清会话回登录；普通 403 → 不登出。
 * 后端 `require_active_account.go:29-31` 用 `BackendError(c, 403, "账号不可用")`，
 * 其业务码 10003 与「账号未注册」共用，故只认 message、不按 code 判定。
 */
class AuthAccountUnavailableTest {

    @Test
    fun http_403_with_account_unavailable_message_is_account_unavailable() {
        assertEquals(
            AuthHttpDecision.AccountUnavailable,
            classifyAuthHttp(statusCode = 403, code = 10003, message = "账号不可用"),
        )
        // message 含关键词即可（后端可能带前后缀）。
        assertEquals(
            AuthHttpDecision.AccountUnavailable,
            classifyAuthHttp(statusCode = 403, message = "账号不可用，请联系管理员"),
        )
    }

    @Test
    fun plain_http_403_without_message_stays_forbidden() {
        assertEquals(AuthHttpDecision.Forbidden, classifyAuthHttp(statusCode = 403))
        assertEquals(AuthHttpDecision.Forbidden, classifyAuthHttp(statusCode = 403, code = 10003))
        assertEquals(AuthHttpDecision.Forbidden, classifyAuthHttp(statusCode = 403, message = "无权限"))
        assertEquals(AuthHttpDecision.Forbidden, classifyAuthHttp(statusCode = 403, message = "账号未注册"))
    }

    @Test
    fun code_10003_alone_never_implies_logout() {
        // 10003 = 账号未注册 / CodeForbidden 共用；不带 403 + 指定文案时不得清会话。
        assertEquals(AuthHttpDecision.None, classifyAuthHttp(code = 10003))
        assertEquals(
            AuthHttpDecision.Forbidden,
            classifyAuthHttp(statusCode = 403, code = 10003, message = "请先注册"),
        )
    }

    @Test
    fun account_unavailable_is_a_hard_failure_but_forbidden_is_not() {
        assertTrue(isAuthHttpHardFailure(statusCode = 403, message = "账号不可用"))
        assertFalse(isAuthHttpHardFailure(statusCode = 403))
        assertFalse(isAuthHttpHardFailure(statusCode = 403, message = "无权限"))
    }

    @Test
    fun refresh_policy_maps_account_unavailable_to_force_logout() {
        assertEquals(
            AuthRefreshDecision.ForceLogout,
            AuthTokenRefresh.policy.decide(403, 10003, "账号不可用"),
        )
        assertEquals(
            AuthRefreshDecision.PassThrough,
            AuthTokenRefresh.policy.decide(403, 10003, "无权限"),
        )
    }

    @Test
    fun repository_clears_session_for_account_unavailable_but_not_plain_403() {
        val unavailable = AuthHttpFailure("账号不可用", code = 10003, statusCode = 403)
        val forbidden = AuthHttpFailure("暂无权限", code = 10003, statusCode = 403)
        val network = AuthHttpFailure("无法连接服务端")
        assertTrue(AuthRepository.shouldClearSessionFor(unavailable))
        assertFalse(AuthRepository.shouldClearSessionFor(forbidden))
        assertFalse(AuthRepository.shouldClearSessionFor(network))
    }

    @Test
    fun map_auth_failure_returns_account_unavailable_copy_not_register_copy() {
        // 共用 code=10003，但 message「账号不可用」必须走 AccountUnavailable 分支，
        // 不能落到「账号未注册，请先注册」。
        assertEquals("账号不可用", mapAuthFailure(10003, "账号不可用", 403))
        assertEquals("账号未注册，请先注册", mapAuthFailure(10003, "账号未注册", 403))
    }
}
