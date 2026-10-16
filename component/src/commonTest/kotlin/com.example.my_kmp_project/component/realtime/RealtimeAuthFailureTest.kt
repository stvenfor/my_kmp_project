package com.example.my_kmp_project.component.realtime

import com.example.my_kmp_project.core.network.NetworkError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 换票 / 建连的鉴权硬失败判定 —— 真值 Dart
 * `components/realtime/lib/client/realtime_auth_failure.dart:7-20`（d7876a1）
 * + `commons/auth/lib/.../auth_http_decision.dart:21-71`（`classifyAuthHttp` / `isAuthHttpHardFailure`），
 * 以及 Dart 自己的单测 `components/realtime/test/realtime_auth_failure_test.dart`。
 *
 * 结论与 Dart 一致：**401 ⇒ 硬失败（停止退避循环）；403 ⇒ 非硬失败**。
 * 业务码：10021 互踢 / 10022 会话无效 ⇒ 硬失败；10023 token 无效 ⇒ 硬失败（可先静默续期）。
 *
 * 分层：组件层不得 import `feature/`，所以这里自带一份等价规则；
 * 与 `feature/auth/AuthHttpDecisionTest` 的双向一致性由本文件与那份测试共同守住。
 */
class RealtimeAuthFailureTest {

    // ── 纯规则面：RealtimeAuthHttpRules ────────────────────────────────────────

    @Test
    fun bizCodesMatchTheAuthLaneConstants() {
        assertEquals(10021, RealtimeAuthHttpRules.SESSION_REPLACED)
        assertEquals(10022, RealtimeAuthHttpRules.SESSION_INVALID)
        assertEquals(10023, RealtimeAuthHttpRules.TOKEN_INVALID)
    }

    @Test
    fun forceLogoutCoversBothKickCodesOnly() {
        assertTrue(RealtimeAuthHttpRules.isForceLogout(10021))
        assertTrue(RealtimeAuthHttpRules.isForceLogout(10022))
        assertFalse(RealtimeAuthHttpRules.isForceLogout(10023))
        assertFalse(RealtimeAuthHttpRules.isForceLogout(null))
    }

    @Test
    fun tryRefreshCoversTokenInvalidOnly() {
        assertTrue(RealtimeAuthHttpRules.isTryRefresh(10023))
        assertFalse(RealtimeAuthHttpRules.isTryRefresh(10021))
    }

    @Test
    fun kickCodesAreHardFailuresEvenWithoutAnHttpStatus() {
        // Dart 测试组 classifyAuthHttp：10021/10022 → forceLogout ⇒ isAuthHttpHardFailure true。
        assertTrue(RealtimeAuthHttpRules.isHardFailure(code = 10021))
        assertTrue(RealtimeAuthHttpRules.isHardFailure(code = 10022))
    }

    @Test
    fun http401IsHardFailureAndHttp403IsNot() {
        // Dart：`expect(isRealtimeAuthHardFailure(HttpRequestException(message:'token 无效', statusCode:401)), isTrue)`
        // 与 `expect(... statusCode:403 ..., isFalse)`。
        assertTrue(RealtimeAuthHttpRules.isHardFailure(statusCode = 401, message = "token 无效"))
        assertFalse(RealtimeAuthHttpRules.isHardFailure(statusCode = 403, message = "无权限"))
    }

    @Test
    fun tokenInvalidCodeIsAHardFailure() {
        assertTrue(RealtimeAuthHttpRules.isHardFailure(code = 10023))
    }

    @Test
    fun forbiddenWinsOverTheMessageHints() {
        // Dart `isAuthHttpHardFailure`：403 直接 return false，文案里的 expired 关键词不参与判断。
        assertFalse(
            RealtimeAuthHttpRules.isHardFailure(statusCode = 403, message = "token 已过期，请重新登录"),
        )
    }

    @Test
    fun messageFallbackOnlyAppliesWhenThereIsNoBusinessCode() {
        // Dart 的 `code == null` 门控：有码就按码判，不再看文案。
        assertTrue(RealtimeAuthHttpRules.isHardFailure(message = "JWT has expired"))
        assertTrue(RealtimeAuthHttpRules.isHardFailure(message = "账号已在其他设备登录"))
        assertTrue(RealtimeAuthHttpRules.isHardFailure(message = "会话无效"))
        assertTrue(RealtimeAuthHttpRules.isHardFailure(message = "未提供 Authorization"))
        assertFalse(RealtimeAuthHttpRules.isHardFailure(message = "服务器繁忙"))
        // 有码但码不属鉴权类 ⇒ 不再被文案带跑。
        assertFalse(RealtimeAuthHttpRules.isHardFailure(code = 50000, message = "token expired"))
    }

    @Test
    fun expiredTokenHintsCoverTheDocumentedKeywordSet() {
        for (hint in listOf("token 无效", "token 已过期", "JWT", "jwt", "token expired", "Token expired", "未授权")) {
            assertTrue(RealtimeAuthHttpRules.looksLikeExpiredToken("前缀 $hint 后缀"), hint)
        }
        assertFalse(RealtimeAuthHttpRules.looksLikeExpiredToken("network unreachable"))
    }

    // ── 异常面：DefaultRealtimeAuthHardFailurePolicy ───────────────────────────

    @Test
    fun businessErrorWithKickCodeIsHard() {
        assertTrue(isRealtimeAuthHardFailure(NetworkError.Business(10021, "账号已在其他设备登录")))
        assertTrue(isRealtimeAuthHardFailure(NetworkError.Business(10022, "会话无效")))
        assertTrue(isRealtimeAuthHardFailure(NetworkError.Business(10023, "token 无效")))
    }

    @Test
    fun businessErrorWithOrdinaryCodeIsNotHard() {
        assertFalse(isRealtimeAuthHardFailure(NetworkError.Business(10001, "参数无效")))
    }

    @Test
    fun transportErrorCarryingHttp401IsHardAnd403IsNot() {
        // 换票 401 走 NetworkError.Transport（KtorApiClient.postRaw 的映射）。
        assertTrue(isRealtimeAuthHardFailure(NetworkError.Transport("Unauthorized", httpStatus = 401)))
        assertFalse(isRealtimeAuthHardFailure(NetworkError.Transport("Forbidden", httpStatus = 403)))
        // 5xx 是网络问题，不是鉴权废了 ⇒ 继续退避重连。
        assertFalse(isRealtimeAuthHardFailure(NetworkError.Transport("boom", httpStatus = 503)))
    }

    @Test
    fun tokenExpiredErrorIsAlwaysHard() {
        // Dart 同一路径按 status=401 处理 ⇒ 停止重连。
        assertTrue(isRealtimeAuthHardFailure(NetworkError.TokenExpired()))
    }

    @Test
    fun stateErrorEquivalentLooksAtItsOwnMessageOnlyForTokenOrLogoutWords() {
        // Dart `error is StateError`（realtime_auth_failure.dart:15-18）：
        // `m.contains('token') || m.contains('未登录')`。
        assertTrue(isRealtimeAuthHardFailure(IllegalStateException("缺少 access token，请先登录")))
        assertTrue(isRealtimeAuthHardFailure(IllegalStateException("未登录或 token 为空")))
        assertTrue(isRealtimeAuthHardFailure(IllegalStateException("UserService 未注册；未登录")))
        assertFalse(isRealtimeAuthHardFailure(IllegalStateException("WebSocket not connected")))
    }

    @Test
    fun anyOtherThrowableFallsBackToWholeMessageClassification() {
        // Dart `return isAuthHttpHardFailure(message: error.toString())`（:19）。
        assertTrue(isRealtimeAuthHardFailure(RuntimeException("token 无效")))
        assertFalse(isRealtimeAuthHardFailure(RuntimeException("socket hang up")))
        assertTrue(isRealtimeAuthHardFailure(IllegalArgumentException("HTTP 401 token expired")))
    }

    @Test
    fun realtimeWsErrorsAreNotAuthFailures() {
        // 无 WS 产物 / 票无效 ≠ 会话已废：不能据此停重连（前者另有专门的停止分支）。
        assertFalse(isRealtimeAuthHardFailure(RealtimeErrorProbe.unsupported()))
        assertFalse(isRealtimeAuthHardFailure(RealtimeErrorProbe.ticketInvalid()))
    }

    @Test
    fun anInjectedPolicyOverridesTheDefaultRules() {
        val alwaysHard = RealtimeAuthHardFailurePolicy { true }
        assertTrue(alwaysHard.isHardFailure(RuntimeException("whatever")))
        val neverHard = RealtimeAuthHardFailurePolicy { false }
        assertFalse(neverHard.isHardFailure(NetworkError.Transport("x", httpStatus = 401)))
    }
}

/** 只为了让 sealed 异常在测试里可构造（不引 core:network 之外的依赖）。 */
private object RealtimeErrorProbe {
    fun unsupported(): Throwable =
        com.example.my_kmp_project.core.network.realtime.RealtimeWsError.Unsupported()

    fun ticketInvalid(): Throwable =
        com.example.my_kmp_project.core.network.realtime.RealtimeWsError.TicketInvalid("WS ticket 缺少 wsUrl")
}
