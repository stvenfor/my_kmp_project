package com.example.my_kmp_project.feature.auth

import com.example.my_kmp_project.component.auth.AuthorizationCodeProvider
import com.example.my_kmp_project.component.auth.AuthorizationCodeResult
import com.example.my_kmp_project.component.auth.LoginAvailability
import com.example.my_kmp_project.component.auth.LoginCredentials
import com.example.my_kmp_project.component.auth.LoginError
import com.example.my_kmp_project.component.auth.LoginOutcome
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 策略模式：密码 / 短信 / 微信 / 华为策略的校验、契约路径与会话载荷映射。
 * 不触碰真实网络：沿用 [FakeAuthApiClient] + 真实 [UserAuthHttpApi]。
 */
class LoginStrategiesTest {

    @Test
    fun password_validation_fails_before_any_request() {
        val fake = FakeAuthApiClient { _, _, _, _ -> error("HTTP should not be called") }
        val strategy = PasswordLoginStrategy(UserAuthHttpApi(fake))

        val empty = runBlocking { strategy.login(LoginCredentials.Password(" ", "123456")) }
        assertEquals("请输入账号", (empty as LoginOutcome.Failure).error.message)

        val badEmail = runBlocking { strategy.login(LoginCredentials.Password("bad@email", "123456")) }
        assertEquals("请输入有效的邮箱", (badEmail as LoginOutcome.Failure).error.message)

        val shortPassword = runBlocking { strategy.login(LoginCredentials.Password("a@b.com", "123")) }
        assertEquals("密码至少 6 位", (shortPassword as LoginOutcome.Failure).error.message)

        assertTrue(fake.calls.isEmpty())
    }

    @Test
    fun password_success_returns_normalized_session_payload() = runBlocking {
        val fake = FakeAuthApiClient { _, path, _, _ ->
            assertEquals(AuthApiPaths.LOGIN, path)
            """
            {"code":0,"message":"success","data":{
              "token":"token-1","refresh_token":"refresh-1","session_id":"session-1",
              "user":{"user_id":"u-1","user_name":"Alice","email":"alice@example.com"}
            }}
            """.trimIndent()
        }

        val outcome = PasswordLoginStrategy(UserAuthHttpApi(fake))
            .login(LoginCredentials.Password("alice@example.com", "123456"))

        val session = (outcome as LoginOutcome.Success).session
        assertEquals("token-1", session.token)
        assertEquals("refresh-1", session.refreshToken)
        assertEquals("session-1", session.sessionId)
        assertEquals("Alice", session.displayName)
        assertEquals("u-1", session.userId)
        assertEquals("alice@example.com", session.email)
        assertTrue(session.deviceId.isNotBlank())
    }

    @Test
    fun password_business_failure_maps_to_login_error() = runBlocking {
        val fake = FakeAuthApiClient { _, _, _, _ ->
            """{"code":10002,"message":"密码错误","data":null}"""
        }

        val outcome = PasswordLoginStrategy(UserAuthHttpApi(fake))
            .login(LoginCredentials.Password("alice@example.com", "123456"))

        val error = (outcome as LoginOutcome.Failure).error
        assertTrue(error is LoginError.Server)
        assertEquals("密码错误", error.message)
        assertEquals(10002, (error as LoginError.Server).bizCode)
    }

    @Test
    fun phone_otp_success_uses_verify_path_and_e164_phone() = runBlocking {
        val fake = FakeAuthApiClient { _, path, _, body ->
            assertEquals(AuthApiPaths.VERIFY_PHONE_OTP, path)
            assertTrue(body.contains("+8613400000000"))
            """
            {"code":0,"message":"success","data":{
              "token":"token-2","refresh_token":"refresh-2","session_id":"session-2",
              "user":{"user_id":"u-2","user_name":"Bob","phone":"+8613400000000"}
            }}
            """.trimIndent()
        }

        val outcome = PhoneOtpLoginStrategy(UserAuthHttpApi(fake))
            .login(LoginCredentials.PhoneOtp("13400000000", "123456"))

        val session = (outcome as LoginOutcome.Success).session
        assertEquals("token-2", session.token)
        assertEquals("Bob", session.displayName)
        assertEquals("+8613400000000", session.phone)
    }

    @Test
    fun phone_otp_send_uses_send_path_and_rejects_invalid_phone() = runBlocking {
        val fake = FakeAuthApiClient { _, path, _, body ->
            assertEquals(AuthApiPaths.SEND_PHONE_OTP, path)
            assertTrue(body.contains("+8613400000000"))
            """{"code":0,"message":"success","data":null}"""
        }
        val strategy = PhoneOtpLoginStrategy(UserAuthHttpApi(fake))

        assertTrue(strategy.sendOtp("13400000000").isSuccess)
        assertTrue(strategy.sendOtp("123").isFailure)
        assertEquals(1, fake.calls.size)
    }

    @Test
    fun huawei_is_unavailable_off_harmony_and_requires_external_code() = runBlocking {
        val fake = FakeAuthApiClient { _, _, _, _ -> error("HTTP should not be called") }
        val android = HuaweiLoginStrategy(UserAuthHttpApi(fake), clientType = "android")
        val harmony = HuaweiLoginStrategy(UserAuthHttpApi(fake), clientType = "ohos")

        assertTrue(android.availability() is com.example.my_kmp_project.component.auth.LoginAvailability.Unavailable)
        assertEquals(
            com.example.my_kmp_project.component.auth.LoginAvailability.Available,
            harmony.availability(),
        )

        val interactive = runBlocking { harmony.login(LoginCredentials.Interactive) }
        assertTrue((interactive as LoginOutcome.Failure).error is LoginError.Unavailable)
        assertTrue(fake.calls.isEmpty())
    }

    @Test
    fun huawei_success_uses_external_device_id_and_default_display() = runBlocking {
        val fake = FakeAuthApiClient { _, path, _, body ->
            assertEquals(AuthApiPaths.HUAWEI_LOGIN, path)
            assertTrue(body.contains("huawei-code"))
            assertTrue(body.contains("kmp-ohos-device"))
            """
            {"code":0,"message":"success","data":{
              "token":"token-3","refresh_token":"refresh-3","session_id":"session-3",
              "user":{"user_id":"","user_name":""}
            }}
            """.trimIndent()
        }

        val outcome = HuaweiLoginStrategy(
            UserAuthHttpApi(fake),
            clientType = "ohos",
        ).login(
            LoginCredentials.ExternalCode(code = "huawei-code", deviceId = "kmp-ohos-device"),
        )

        val session = (outcome as LoginOutcome.Success).session
        assertEquals("token-3", session.token)
        assertEquals("华为用户", session.displayName)
        assertEquals("华为用户", session.userId)
    }

    @Test
    fun wechat_is_unavailable_without_authorization_channel() = runBlocking {
        val fake = FakeAuthApiClient { _, _, _, _ -> error("HTTP should not be called") }
        val strategy = WechatLoginStrategy(UserAuthHttpApi(fake), provider = null)

        assertTrue(strategy.availability() is LoginAvailability.Unavailable)

        val outcome = strategy.login(LoginCredentials.Interactive)
        val error = (outcome as LoginOutcome.Failure).error
        assertTrue(error is LoginError.Unavailable)
        assertEquals(LoginMethodIds.WECHAT, error.methodId)
        assertTrue(fake.calls.isEmpty())
    }

    @Test
    fun wechat_interactive_uses_channel_code_and_wechat_endpoint() = runBlocking {
        val fake = FakeAuthApiClient { _, path, _, body ->
            assertEquals(AuthApiPaths.WECHAT_LOGIN, path)
            assertTrue(body.contains("wx-code"))
            """
            {"code":0,"message":"success","data":{
              "token":"token-4","refresh_token":"refresh-4","session_id":"session-4",
              "user":{"user_id":"u-4","user_name":"Carol"}
            }}
            """.trimIndent()
        }
        val strategy = WechatLoginStrategy(
            api = UserAuthHttpApi(fake),
            provider = FakeAuthorizationCodeProvider(
                AuthorizationCodeResult.Success("wx-code"),
            ),
        )

        assertTrue(strategy.availability() is LoginAvailability.Available)

        val outcome = strategy.login(LoginCredentials.Interactive)

        val session = (outcome as LoginOutcome.Success).session
        assertEquals("token-4", session.token)
        assertEquals("Carol", session.displayName)
    }

    @Test
    fun wechat_cancel_from_channel_is_not_an_error() = runBlocking {
        val fake = FakeAuthApiClient { _, _, _, _ -> error("HTTP should not be called") }
        val strategy = WechatLoginStrategy(
            api = UserAuthHttpApi(fake),
            provider = FakeAuthorizationCodeProvider(AuthorizationCodeResult.Cancelled),
        )

        val outcome = strategy.login(LoginCredentials.Interactive)

        assertEquals(LoginOutcome.Cancelled, outcome)
        assertTrue(fake.calls.isEmpty())
    }

    @Test
    fun wechat_external_code_works_without_kotlin_channel() = runBlocking {
        val fake = FakeAuthApiClient { _, path, _, body ->
            assertEquals(AuthApiPaths.WECHAT_LOGIN, path)
            assertTrue(body.contains("native-wx-code"))
            assertTrue(body.contains("native-device"))
            """{"code":0,"message":"success","data":{"token":"token-5"}}"""
        }

        val outcome = WechatLoginStrategy(UserAuthHttpApi(fake), provider = null)
            .login(LoginCredentials.ExternalCode(code = "native-wx-code", deviceId = "native-device"))

        assertEquals("token-5", (outcome as LoginOutcome.Success).session.token)
    }

    private class FakeAuthorizationCodeProvider(
        private val result: AuthorizationCodeResult,
    ) : AuthorizationCodeProvider {
        override val methodId: String = LoginMethodIds.WECHAT

        override suspend fun requestCode(): AuthorizationCodeResult = result
    }
}
