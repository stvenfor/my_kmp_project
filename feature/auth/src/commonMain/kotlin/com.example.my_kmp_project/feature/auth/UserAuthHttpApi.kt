package com.example.my_kmp_project.feature.auth

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.postApi
import com.example.my_kmp_project.core.network.postApiOk
import kotlinx.coroutines.CancellationException

/**
 * 传输层归一后的鉴权错误（Flutter `UserAuthApi._mapFailure` 抛出的 `AuthFailure` 家族对等）。
 *
 * 与 [AuthFailure] 的分工：[AuthFailure] 是**语义分类**（互踢 / 密码错 / OTP 错…），
 * 本类型只负责把 BFF 的 `code` 与 HTTP status **原样带出**给会话决策
 * （[classifyAuthHttp] / [AuthBizCode.isSessionGone]），message 已是最终中文文案。
 */
public class AuthHttpFailure(
    override val message: String,
    val code: Int? = null,
    val statusCode: Int? = null,
) : Exception(message)

/**
 * Flutter `UserAuthApi`（`features/auth/lib/api/user_auth_api.dart`，d7876a1）的 KMP 对等：
 * 契约表 **§2.1 #1–#7** 的纯 HTTP 层。
 *
 * 职责边界（与 [AuthRepository] 分工）：本对象**只做请求/应答映射**，返回
 * `Result<DTO>`；不落库、不 commit 会话、不清会话（那是 [AuthRepository] /
 * [AuthTokenRefresh] 的事），因此 UI 之外的调用方也能复用同一份契约。
 *
 * 头注入差异：Dart 的 `logout` 手工带 `Authorization` / `X-Session-ID` / `X-Device-ID`
 * （:118-125）。KMP 只有 `core/network` 的 `DemoRequestHeaders` 注入 `Authorization`，
 * `ApiClient` 无逐请求头出口。
 * `// GAP(R5)`: `X-Session-ID` / `X-Device-ID` 需 `core/network` 增加逐请求头能力后补齐。
 */
public object UserAuthHttpApi {

    /** 契约表 #1 `POST /user/login`（`user_auth_api.dart:15-34`）；`data` = [AuthLoginData]。 */
    suspend fun login(
        username: String,
        password: String,
        deviceId: String,
        platform: String,
    ): Result<AuthLoginData> = postLogin(
        path = AuthApiPaths.LOGIN,
        body = AuthPasswordLoginRequest(
            username = username,
            password = password,
            deviceId = deviceId,
            platform = platform,
        ),
    )

    /**
     * 契约表 #2 `POST /user/register`（`user_auth_api.dart:132-154`）；
     * `data` 双形态由 [AuthRegisterData.resolvedUser] 归一（可带可不带嵌套 `user`）。
     */
    suspend fun register(
        username: String,
        password: String,
        email: String,
        deviceId: String,
        platform: String,
    ): Result<AuthRegisterData> = guard {
        val response = NetworkFacade.api()
            .postApi<AuthRegisterRequest, AuthRegisterData>(
                path = AuthApiPaths.REGISTER,
                body = AuthRegisterRequest(
                    username = username,
                    password = password,
                    email = email,
                    deviceId = deviceId,
                    platform = platform,
                ),
            )
        val data = response.data
        if (isAuthBusinessSuccess(response.code) && data != null) {
            Result.success(data)
        } else {
            Result.failure(authFailure(response))
        }
    }

    /** 契约表 #5 `POST /user/phone/otp/send`（:36-45）；只看 `code`，`data` 恒为 null。 */
    suspend fun sendPhoneOtp(phone: String): Result<Unit> = guard {
        val response = NetworkFacade.api().postApiOk(
            path = AuthApiPaths.SEND_PHONE_OTP,
            body = AuthPhoneOtpSendRequest(phone = phone),
        )
        if (isAuthBusinessSuccess(response.code)) {
            Result.success(Unit)
        } else {
            Result.failure(authFailure(response))
        }
    }

    /** 契约表 #6 `POST /user/phone/otp/verify`（:47-66）；`data` = `LoginResult`。 */
    suspend fun verifyPhoneOtp(
        phone: String,
        otp: String,
        deviceId: String,
        platform: String,
    ): Result<AuthLoginData> = postLogin(
        path = AuthApiPaths.VERIFY_PHONE_OTP,
        body = AuthPhoneOtpVerifyRequest(
            phone = phone,
            otp = otp,
            deviceId = deviceId,
            platform = platform,
        ),
    )

    /**
     * 契约表 #7 `POST /user/wechat/login`（:68-85）；`data` = `LoginResult`。
     *
     * `// GAP(R5)`: 微信授权码要原生 SDK 才有（iOS `WXApi.sendReq` / Android
     * `wechat-sdk-android` / OHOS 无对应 Kit）；本对象只完成 **HTTP 契约**，
     * 取码桥尚未接入，调用方必须自行拿到 `code`（勿据此判定微信登录已可用）。
     */
    suspend fun loginWithWechat(
        code: String,
        deviceId: String,
        platform: String,
    ): Result<AuthLoginData> = postLogin(
        path = AuthApiPaths.WECHAT_LOGIN,
        body = AuthWechatLoginRequest(
            code = code,
            deviceId = deviceId,
            platform = platform,
        ),
    )

    /**
     * 契约表 #3 `POST /user/refresh`（:87-108）——**静默续期**。
     *
     * [raw] **必须**是未装饰 client（[NetworkFacade.rawApi]）：Dart 在此请求上显式
     * `Options(extra: {'skipAuthRefresh': true})`，KMP 由 raw client 等价保证不递归刷新
     * （豁免路径表见 `core/network/TokenRefresh.kt`，勿在 feature 层重复实现）。
     * 可选三键空白省略见 [AuthRefreshRequest.of]。
     */
    suspend fun refresh(
        raw: ApiClient,
        refreshToken: String,
        deviceId: String?,
        sessionId: String?,
        platform: String?,
    ): Result<AuthRefreshData> = guard {
        val response = raw.postApi<AuthRefreshRequest, AuthRefreshData>(
            path = AuthApiPaths.REFRESH,
            body = AuthRefreshRequest.of(
                refreshToken = refreshToken,
                deviceId = deviceId,
                sessionId = sessionId,
                platform = platform,
            ),
        )
        val data = response.data
        if (isAuthBusinessSuccess(response.code) && data != null && data.token.isNotBlank()) {
            Result.success(data)
        } else {
            Result.failure(authFailure(response))
        }
    }

    /**
     * 契约表 #4 `POST /user/logout`（:110-130）：**无 body**、只看 `code == 0`。
     *
     * `skipAuthRefresh` 由 `core/network` 的豁免路径列表（含 `/user/logout`）保证，
     * 本方法不再重复实现；`Authorization` 由 `DemoRequestHeaders` 用当前 access token 注入，
     * 因此调用方**必须**在清本地会话之前发出（否则头就空了）。
     * `X-Session-ID` / `X-Device-ID` 见文件级 `// GAP(R5)`。
     */
    suspend fun logout(): Result<Unit> = guard {
        val response = NetworkFacade.api().postApi<Unit>(
            path = AuthApiPaths.LOGOUT,
            body = "",
            parseData = { },
        )
        if (isAuthBusinessSuccess(response.code)) {
            Result.success(Unit)
        } else {
            Result.failure(authFailure(response))
        }
    }

    /**
     * Dart `_requireData` 对等：非成功码 / `data` 缺失 / `token` 空串一律失败，
     * 文案走 [mapAuthFailure]（保留会话由调用方决定）。
     */
    private suspend inline fun <reified Req : Any> postLogin(
        path: String,
        body: Req,
    ): Result<AuthLoginData> = guard {
        val response = NetworkFacade.api().postApi<Req, AuthLoginData>(path, body)
        val data = response.data
        if (isAuthBusinessSuccess(response.code) && data != null && data.token.isNotBlank()) {
            Result.success(data)
        } else {
            Result.failure(authFailure(response))
        }
    }

    /** 把 `code` / HTTP status 交给 [mapAuthFailure] 出中文文案，同时保留原码供会话决策。 */
    private fun authFailure(response: ApiResponse<*>): AuthHttpFailure =
        AuthHttpFailure(
            message = mapAuthFailure(response.code, response.message, response.httpStatus),
            code = response.code,
            statusCode = response.httpStatus,
        )

    /**
     * Flutter `UserAuthApi._guarded`（:156-170）：`AuthFailure` 原样上抛，
     * transport/其它异常映射成中文文案；业务码缺失（`code = null`）时仍走 [mapAuthFailure]。
     */
    private suspend inline fun <T> guard(
        block: () -> Result<T>,
    ): Result<T> = try {
        block()
    } catch (e: AuthHttpFailure) {
        Result.failure(e)
    } catch (e: CancellationException) {
        // 协程取消不是业务失败，必须原样上抛（Dart 侧无此语义，Kotlin 必需）。
        throw e
    } catch (e: NetworkError.Transport) {
        Result.failure(
            AuthHttpFailure(
                message = mapAuthFailure(null, e.message, e.httpStatus),
                statusCode = e.httpStatus,
            ),
        )
    } catch (e: Throwable) {
        Result.failure(
            AuthHttpFailure(message = mapAuthFailure(null, e.message, null)),
        )
    }
}
