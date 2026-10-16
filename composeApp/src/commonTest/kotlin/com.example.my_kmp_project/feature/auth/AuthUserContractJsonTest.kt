package com.example.my_kmp_project.feature.auth

import com.example.my_kmp_project.core.network.ApiEnvelope
import com.example.my_kmp_project.core.network.NetworkJson
import com.example.my_kmp_project.core.network.dataOrNull
import com.example.my_kmp_project.core.network.decodeEnvelopeData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 契约表 §2.1 #1–#7（`features/auth/lib/api/user_auth_api.dart` @ d7876a1）的 JSON 夹具测试：
 * 用真实 BFF 信封 `{code,message,data}` 校验 `data` 字段映射（含大小写别名）
 * 与请求体的 **omit-vs-send** 规则。
 */
class AuthUserContractJsonTest {

    private inline fun <reified T> dataOf(raw: String): T? =
        decodeEnvelopeData<T>(
            NetworkJson.decodeFromString(ApiEnvelope.serializer(), raw).dataOrNull(),
        )

    private inline fun <reified T> bodyOf(value: T): JsonObject =
        NetworkJson.parseToJsonElement(NetworkJson.encodeToString(value)) as JsonObject

    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.content

    // ---- #1 POST /user/login ------------------------------------------------

    @Test
    fun login_path_matches_contract() {
        // 契约表 §2.1 七条 path 逐字对齐 Dart `user_auth_api.dart:7-13`。
        assertEquals("/api/v1/user/login", AuthApiPaths.LOGIN)
        assertEquals("/api/v1/user/register", AuthApiPaths.REGISTER)
        assertEquals("/api/v1/user/refresh", AuthApiPaths.REFRESH)
        assertEquals("/api/v1/user/logout", AuthApiPaths.LOGOUT)
        assertEquals("/api/v1/user/phone/otp/send", AuthApiPaths.SEND_PHONE_OTP)
        assertEquals("/api/v1/user/phone/otp/verify", AuthApiPaths.VERIFY_PHONE_OTP)
        assertEquals("/api/v1/user/wechat/login", AuthApiPaths.WECHAT_LOGIN)
    }

    @Test
    fun login_body_sends_all_four_keys() {
        val body = bodyOf(
            AuthPasswordLoginRequest(
                username = "a@b.com",
                password = "secret",
                deviceId = "dev-1",
                platform = "ios",
            ),
        )
        assertEquals(
            setOf("username", "password", "device_id", "platform"),
            body.keys,
        )
        assertEquals("dev-1", body.text("device_id"))
        assertEquals("ios", body.text("platform"))
    }

    @Test
    fun login_result_maps_camel_case_aliases() {
        // Dart BackendUser.fromJson：`user_id|id`、`user_name|username|display_name`、`avatar_url|avatarUrl`。
        val data = dataOf<AuthLoginData>(
            """
            {
              "code": 0,
              "message": "success",
              "data": {
                "token": "AT",
                "refresh_token": "RT",
                "session_id": "SID",
                "user": {
                  "id": "u-1",
                  "user_id": "u-1",
                  "user_name": "张三",
                  "email": "a@b.com",
                  "status": 1,
                  "phone": "13800138000",
                  "avatarUrl": "https://cdn/a.png"
                }
              },
              "timestamp": 1700000000
            }
            """.trimIndent(),
        )
        assertEquals("AT", data?.token)
        assertEquals("RT", data?.refreshToken)
        assertEquals("SID", data?.sessionId)
        val user = data?.resolvedUser()
        assertEquals("u-1", user?.resolvedId)
        assertEquals("u-1", user?.resolvedUserId)
        assertEquals("张三", user?.resolvedUsername)
        assertEquals("张三", user?.resolvedUserName)
        assertEquals(1, user?.resolvedStatus)
        assertEquals("https://cdn/a.png", user?.resolvedAvatarUrl)
        val payload = data?.toPayload()
        assertEquals("u-1", payload?.userId)
        assertEquals("张三", payload?.username)
        assertEquals("https://cdn/a.png", payload?.avatarUrl)
    }

    @Test
    fun login_result_prefers_user_name_over_username_and_display_name() {
        // Dart :450-455 顺序：user_name → username → display_name。
        val onlyDisplay = dataOf<AuthLoginData>(
            """
            {"code":0,"message":"success","data":{"token":"AT",
             "user":{"user_id":"9","username":"legacy","display_name":"昵称"}}}
            """.trimIndent(),
        )!!.resolvedUser()
        assertEquals("legacy", onlyDisplay.resolvedUsername)

        val snakeAvatar = dataOf<AuthLoginData>(
            """
            {"code":0,"message":"success","data":{"token":"AT",
             "user":{"id":"9","display_name":"昵称","avatar_url":"https://cdn/b.png"}}}
            """.trimIndent(),
        )!!.resolvedUser()
        // 无 user_name/username 时 display_name 兜住名字；avatar_url（snake）生效。
        assertEquals("昵称", snakeAvatar.resolvedUsername)
        assertEquals("https://cdn/b.png", snakeAvatar.resolvedAvatarUrl)
        assertEquals("9", snakeAvatar.resolvedId)
    }

    @Test
    fun login_result_status_accepts_numeric_string_and_missing_defaults_to_zero() {
        val asText = dataOf<AuthLoginData>(
            """{"code":0,"message":"success","data":{"token":"AT","user":{"id":"1","status":"2"}}}""",
        )!!.resolvedUser()
        assertEquals(2, asText.resolvedStatus)

        val missing = dataOf<AuthLoginData>(
            """{"code":0,"message":"success","data":{"token":"AT","user":{"id":"1"}}}""",
        )!!.resolvedUser()
        assertEquals(0, missing.resolvedStatus)
        assertEquals("", missing.resolvedEmail)
        assertEquals("", missing.resolvedAvatarUrl)
    }

    @Test
    fun login_result_without_user_degrades_to_empty_user() {
        // Dart `json['user'] is Map ? … : const {}`（:361-365）。
        val data = dataOf<AuthLoginData>(
            """{"code":0,"message":"success","data":{"token":"AT","session_id":"S"}}""",
        )!!
        assertEquals(null, data.user)
        val user = data.resolvedUser()
        assertEquals("", user.resolvedId)
        assertEquals("", user.resolvedUsername)
        assertEquals("AT", data.toPayload().token)
    }

    @Test
    fun login_failure_envelope_has_no_data() {
        // 10002 账密错：data 为 null，文案走 mapAuthFailure。
        val raw = """{"code":10002,"message":"用户名或密码错误","data":null}"""
        assertEquals(10002, NetworkJson.decodeFromString(ApiEnvelope.serializer(), raw).code)
        assertEquals(null, dataOf<AuthLoginData>(raw))
        assertEquals("密码错误", mapAuthFailure(10002, "用户名或密码错误"))
    }

    // ---- #2 POST /user/register（双形态）------------------------------------

    @Test
    fun register_body_sends_all_five_keys() {
        val body = bodyOf(
            AuthRegisterRequest(
                username = "zhangsan",
                password = "secret",
                email = "a@b.com",
                deviceId = "dev-1",
                platform = "android",
            ),
        )
        assertEquals(
            setOf("username", "password", "email", "device_id", "platform"),
            body.keys,
        )
    }

    @Test
    fun register_result_with_nested_user_shape() {
        val data = dataOf<AuthRegisterData>(
            """
            {"code":0,"message":"success","data":{
              "token":"AT","refresh_token":"RT","session_id":"SID",
              "user":{"user_id":"u-7","user_name":"张三","email":"a@b.com","status":1}}}
            """.trimIndent(),
        )!!
        assertTrue(data.hasSession)
        assertEquals("u-7", data.resolvedUser().resolvedUserId)
        assertEquals("张三", data.resolvedUsername())
        val payload = data.toPayload()
        assertEquals("AT", payload.token)
        assertEquals("RT", payload.refreshToken)
        assertEquals("SID", payload.sessionId)
        assertEquals("a@b.com", payload.email)
    }

    @Test
    fun register_result_flat_shape_reads_root_as_user() {
        // Dart :418-423：没有嵌套 user 时，把整个 `data` 当 BackendUser 解析。
        val data = dataOf<AuthRegisterData>(
            """
            {"code":0,"message":"success","data":{
              "id":"u-8","username":"lisi","email":"l@b.com","status":0,
              "avatar_url":"https://cdn/c.png","phone":"13900000000"}}
            """.trimIndent(),
        )!!
        assertEquals(null, data.user)
        assertEquals("u-8", data.resolvedUser().resolvedId)
        assertEquals("lisi", data.resolvedUsername())
        assertEquals("l@b.com", data.resolvedEmail())
        assertEquals("https://cdn/c.png", data.resolvedAvatarUrl())
        assertEquals("13900000000", data.resolvedPhone())
        // 无 token → hasSession=false，调用方需回落密码登录。
        assertFalse(data.hasSession)
        assertEquals(null, data.toPayload().token)
    }

    @Test
    fun register_result_empty_map_gives_all_blank_user() {
        // 契约表 #2 备注：空 map → 全 `''` user。
        val data = dataOf<AuthRegisterData>(
            """{"code":0,"message":"success","data":{}}""",
        )!!
        assertFalse(data.hasSession)
        assertEquals("", data.resolvedUserId())
        assertEquals("", data.resolvedUsername())
        assertEquals("", data.resolvedEmail())
        assertEquals(0, data.resolvedStatus())
    }

    @Test
    fun register_result_treats_empty_token_string_as_no_session() {
        val data = dataOf<AuthRegisterData>(
            """{"code":0,"message":"success","data":{"token":"","user":{"id":"1"}}}""",
        )!!
        assertFalse(data.hasSession)
        assertEquals(null, data.toPayload().token)
    }

    // ---- #3 POST /user/refresh（空白省略 key）------------------------------

    @Test
    fun refresh_body_omits_blank_and_null_optional_keys() {
        // Dart :97-102：`if (x != null && x.isNotEmpty) 'k': x`。
        val minimal = bodyOf(
            AuthRefreshRequest.of(
                refreshToken = "RT",
                deviceId = null,
                sessionId = "   ",
                platform = "",
            ),
        )
        assertEquals(setOf("refresh_token"), minimal.keys)
        assertEquals("RT", minimal.text("refresh_token"))

        val full = bodyOf(
            AuthRefreshRequest.of(
                refreshToken = "RT",
                deviceId = "dev-1",
                sessionId = "SID",
                platform = "ios",
            ),
        )
        assertEquals(
            setOf("refresh_token", "device_id", "session_id", "platform"),
            full.keys,
        )
        assertEquals("dev-1", full.text("device_id"))
        assertEquals("SID", full.text("session_id"))
    }

    @Test
    fun refresh_result_maps_token_trio() {
        val data = dataOf<AuthRefreshData>(
            """
            {"code":0,"message":"success","data":
              {"token":"AT2","refresh_token":"RT2","session_id":"SID2"}}
            """.trimIndent(),
        )!!
        assertEquals("AT2", data.token)
        assertEquals("RT2", data.refreshToken)
        assertEquals("SID2", data.sessionId)
    }

    @Test
    fun refresh_result_missing_optional_fields_default_to_empty() {
        // 服务端只轮换 access token 时，refresh/session 为空串（调用方沿用旧值）。
        val data = dataOf<AuthRefreshData>(
            """{"code":0,"message":"success","data":{"token":"AT2"}}""",
        )!!
        assertEquals("", data.refreshToken)
        assertEquals("", data.sessionId)
    }

    // ---- #5 / #6 手机验证码 --------------------------------------------------

    @Test
    fun otp_send_body_sends_only_phone() {
        val body = bodyOf(AuthPhoneOtpSendRequest(phone = "+8613800138000"))
        assertEquals(setOf("phone"), body.keys)
        assertEquals("+8613800138000", body.text("phone"))
    }

    @Test
    fun otp_verify_body_and_login_result_shape() {
        val body = bodyOf(
            AuthPhoneOtpVerifyRequest(
                phone = "+8613800138000",
                otp = "123456",
                deviceId = "dev-1",
                platform = "harmony",
            ),
        )
        assertEquals(setOf("phone", "otp", "device_id", "platform"), body.keys)

        // #6 的 data 与 #1 同为 LoginResult。
        val data = dataOf<AuthLoginData>(
            """
            {"code":0,"message":"success","data":{"token":"AT",
              "user":{"user_id":"7","user_name":"张三","phone":"13800138000"}}}
            """.trimIndent(),
        )!!
        assertEquals("7", data.toPayload().userId)
        assertEquals("13800138000", data.toPayload().phone)
    }

    @Test
    fun otp_failure_code_maps_to_invalid_otp_copy() {
        // 契约表 §4：10024 invalidOtp → 「验证码错误或已失效」，原位重试、不登出。
        assertEquals("验证码错误或已失效", mapAuthFailure(10024, "验证码已失效"))
    }

    // ---- #7 POST /user/wechat/login ----------------------------------------

    @Test
    fun wechat_body_sends_code_device_id_platform() {
        val body = bodyOf(
            AuthWechatLoginRequest(code = "wx-code", deviceId = "dev-1", platform = "ios"),
        )
        assertEquals(setOf("code", "device_id", "platform"), body.keys)
        assertEquals("wx-code", body.text("code"))
    }

    @Test
    fun wechat_result_is_login_result() {
        val data = dataOf<AuthLoginData>(
            """
            {"code":0,"message":"success","data":{"token":"AT","session_id":"S",
              "user":{"user_id":"wx-1","display_name":"微信用户","avatarUrl":"https://cdn/wx.png"}}}
            """.trimIndent(),
        )!!
        assertEquals("wx-1", data.toPayload().userId)
        assertEquals("微信用户", data.toPayload().username)
        assertEquals("https://cdn/wx.png", data.toPayload().avatarUrl)
    }
}
