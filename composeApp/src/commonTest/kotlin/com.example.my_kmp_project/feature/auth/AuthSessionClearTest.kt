package com.example.my_kmp_project.feature.auth

import com.example.my_kmp_project.core.router.MainTab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * AuthGate / session-clear unit tests (no platform AccountSessionStore / Context required).
 * Full AccountFacade login persistence is covered by device/instrumentation smoke.
 */
class AuthSessionClearTest {

    @Test
    fun requires_auth_for_chat_and_community() {
        assertTrue(AuthGate.requiresAuth(MainTab.Chat))
        assertTrue(AuthGate.requiresAuth(MainTab.Community))
        assertFalse(AuthGate.requiresAuth(MainTab.Home))
        assertFalse(AuthGate.requiresAuth(MainTab.Mine))
    }

    @Test
    fun consume_pending_returns_once() {
        AuthGate.clearPending()
        AuthGate.rememberPending(MainTab.Community)
        assertEquals(MainTab.Community, AuthGate.consumePending())
        assertNull(AuthGate.consumePending())
    }

    @Test
    fun logout_clears_pending_gate() {
        AuthGate.rememberPending(MainTab.Chat)
        AuthGate.clearPending()
        assertNull(AuthGate.pendingTab)
        AuthSessionState.clearLocal()
        assertFalse(AuthSessionState.isLoggedIn)
    }

    @Test
    fun force_logout_clears_session_so_protected_tabs_require_login() {
        // Mirrors AppShell TokenExpiredHandler local effects without AccountFacade
        // (AccountFacade needs AndroidAccountContext — covered by device smoke).
        AuthGate.rememberPending(MainTab.Community)
        AuthSessionState.clearLocal()
        AuthGate.clearPending()
        assertFalse(AuthSessionState.isLoggedIn)
        assertNull(AuthGate.pendingTab)
        assertTrue(AuthGate.requiresAuth(MainTab.Chat))
        assertTrue(AuthGate.requiresAuth(MainTab.Community))
    }

    @Test
    fun map_auth_failure_matches_flutter_copy() {
        assertEquals("密码错误", mapAuthFailure(10002, null))
        assertEquals("账号未注册，请先注册", mapAuthFailure(10003, null))
        assertEquals("验证码错误或已失效", mapAuthFailure(null, "验证码已失效"))
        assertEquals("该邮箱已注册", mapAuthFailure(null, "用户已存在"))
        assertTrue(mapAuthFailure(null, "Connection refused").contains("无法连接服务端"))
    }

    @Test
    fun phone_and_greeting_helpers() {
        assertTrue(AuthPhoneUtils.isValidChinaMobile("13800138000"))
        assertFalse(AuthPhoneUtils.isValidChinaMobile("12345"))
        assertEquals("+8613800138000", AuthPhoneUtils.toE164China("13800138000"))
        assertEquals("早上好，欢迎使用i车商", authGreetingForHour(9))
        assertEquals("下午好，欢迎使用i车商", authGreetingForHour(15))
        assertEquals("晚上好，欢迎使用i车商", authGreetingForHour(20))
        assertTrue(isAuthBusinessSuccess(0))
        assertTrue(isAuthBusinessSuccess(200))
        assertFalse(isAuthBusinessSuccess(10002))
    }
}
