@file:Suppress("DEPRECATION")

package com.example.my_kmp_project.core.network

/**
 * Common API / business result codes for the demo network layer.
 *
 * 三套码表并存（勿合并）：
 * - [NetworkCodes.BFF_OK] = 0：Go BFF 统一信封的成功码
 *   （`app/internal/delivery/http/dto/response/common.go:12` 的 `CodeSuccess`，
 *   `response.Success` 写入 `Response.Code`）。
 * - [BackendCodes]：Go 后端五位数错误码（`common.go:12-29` 的 `Code*` 常量），
 *   由 [ApiEnvelopeParser] 的 `when(code)` 显式分类。
 * - 遗留 Flutter 对齐表 `commons/wys_network/lib/src/network_codes.dart`：
 *   `ok = 200`、`0 = 无网络` …；下方旧常量一律标注 `@Deprecated`，仅为兼容旧调用点保留，
 *   Go 后端从不返回这些业务码。
 *
 * 因此 `0` 同值不同义：解析器在**缺 `code`** 时回退 [NetworkCodes.PASSTHROUGH]，
 * 绝不回退到 0，否则畸形响应会被 [ApiResponse.isSuccess] 误判为成功。
 */
public object NetworkCodes {
    /** Go BFF 统一信封业务成功码（`code == 0`，等价于 [BackendCodes.SUCCESS]）。 */
    const val BFF_OK = 0

    @Deprecated("Flutter 遗留码表 wys_network：0 表示无网络；Go 后端不用，见 BackendCodes.SUCCESS")
    const val NOT_NETWORK = 0

    @Deprecated("Flutter 遗留码表 wys_network 的 ok=200；Go BFF 成功码是 0，后端从不返回业务码 200。保留兼容，isSuccess 已不再采用")
    const val OK = 200

    @Deprecated("Flutter 遗留码表 wys_network；Go 后端对应 BackendCodes.INVALID_PARAMS 等五位数码")
    const val INVALID_AUTH = 100

    @Deprecated("Flutter 遗留码表 wys_network；Go 后端不用此业务码")
    const val INVALID_MEMBER = 301

    @Deprecated("Flutter 遗留码表 wys_network；401 在 Go 侧是 HTTP status，token 失效对应 BackendCodes.TOKEN_INVALID(10023)")
    const val EXPIRE_TOKEN = 401

    /**
     * 无权限（Flutter `NetworkCodes.forbidden`，d7876a1）。
     * HTTP 403 与业务 code 403 均映射至此：**不**触发登出 / token 刷新，
     * 由业务自行提示（镜像 `forbidden_status_test.dart`）。
     *
     * 注意：Go `BackendCodes.FORBIDDEN = 10003` 有歧义，**只认 HTTP 403** 判无权限，
     * 不要按 `code == 10003` 触发无权限。
     */
    @Deprecated("Flutter 遗留码表 wys_network；HTTP 403 仍沿用，后端 code=10003 另见 BackendCodes.FORBIDDEN")
    const val FORBIDDEN = 403

    @Deprecated("Flutter 遗留码表 wys_network；Go 后端不用此业务码")
    const val BUSY = 429

    @Deprecated("Flutter 遗留码表 wys_network；Go 后端不用此业务码")
    const val INVALID_ACCOUNT = 500

    const val PASSTHROUGH = -1
}

/**
 * Go 后端统一响应五位数码表，逐一对应
 * `app/internal/delivery/http/dto/response/common.go:12-29` 的 `Code*` 常量。
 *
 * 这是 KMP 客户端与 Go 后端之间「纯双方」的码表真相源；[ApiEnvelopeParser] 按此分类，
 * 10021 / 10022 / 10023 的最终刷新 / 登出决策由 feature/auth 的装饰器处理。
 */
public object BackendCodes {
    /** CodeSuccess = 0，成功（`response.Success`）。 */
    const val SUCCESS = 0
    /** CodeInvalidParams = 10001，参数错误。 */
    const val INVALID_PARAMS = 10001
    /** CodeUnauthorized = 10002，登录凭证错误（如密码错）；勿与 token 失效混用。 */
    const val UNAUTHORIZED = 10002
    /** CodeForbidden = 10003；歧义码，客户端只认 HTTP 403 判无权限，不按此码判 Forbidden。 */
    const val FORBIDDEN = 10003
    /** CodeNotFound = 10004，资源不存在。 */
    const val NOT_FOUND = 10004
    /** CodeNotImplemented = 10005，能力未接入（HTTP 501）；客户端不得按成功或重试处理。 */
    const val NOT_IMPLEMENTED = 10005
    /** CodeSessionReplaced = 10021，单设备互踢。 */
    const val SESSION_REPLACED = 10021
    /** CodeSessionInvalid = 10022，会话无效。 */
    const val SESSION_INVALID = 10022
    /** CodeTokenInvalid = 10023，access JWT 无效；客户端 tryRefresh，失败再清会话。 */
    const val TOKEN_INVALID = 10023
    /** CodeInvalidOTP = 10024，短信 / 邮箱验证码错误或过期。 */
    const val INVALID_OTP = 10024
    /** CodeInternalError = 50000，内部错误。 */
    const val INTERNAL_ERROR = 50000
}
