package com.example.my_kmp_project.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonElement

/**
 * 401 出口决策（core 层枚举；具体策略由 feature 层注入，core 不依赖 feature）。
 *
 * 后端 `sessionAuthChain` 是**两层**：Go `shared.SessionAuthChain`（`app/internal/shared/http.go:10`）
 * 拼 `[SessionAuth（必选）, 可选 AccountGate]`。
 * - **SessionAuth** 校验 access token + 单设备 session，失败给 **401**（业务码 10023）；
 * - **AccountGate**（账号状态门 / 权限）失败给 **403**。
 *
 * 两层的处置必须分开：401 走 [Refresh] / [ForceLogout]；**403 来自 AccountGate / 权限，
 * 不刷新、不登出** → 一律 [PassThrough]，业务自理。
 *
 * Flutter `AuthTokenRefreshInterceptor` + `SessionGuardHook.classify`（d7876a1）：
 * - [Refresh]：10023 / HTTP 401（非互踢）→ 静默 refresh 成功后代发原请求一次
 * - [ForceLogout]：10021 / 10022 → 清会话回登录，不重试
 * - [PassThrough]：其它（含 403 Forbidden）→ 原样透传，业务自理
 */
public enum class AuthRefreshDecision { Refresh, ForceLogout, PassThrough }

/**
 * 策略注入点：feature/auth 用 `classifyAuthHttp` 实现（Flutter `SessionGuardHook.classify`）。
 * [statusCode] 为 HTTP status（服务器应答错误时非空），[code] 为业务码（可能与 status 重合）。
 *
 * 后端两层鉴权链 `SessionAuth + 可选 AccountGate`：401 出自 SessionAuth（token / session），
 * 403 出自 AccountGate（账号状态 / 权限）。因此实现方对 403 必须返回 [AuthRefreshDecision.PassThrough]，
 * 不刷新、不登出；详见 [AuthRefreshDecision] 的 KDoc。
 */
public fun interface AuthRefreshPolicy {
    fun decide(statusCode: Int?, code: Int?, message: String?): AuthRefreshDecision
}

/**
 * 刷新执行体：feature/auth 实现；[raw] 是**未装饰**的 client，
 * refresh 请求走它发出，避免递归触发刷新（Flutter `skipAuthRefresh` 对等）。
 * 刷新失败 / 无 refreshToken 时实现方自行走「刷新耗尽」清会话，然后抛出。
 */
public fun interface TokenRefreshHandler {
    suspend fun refresh(raw: ApiClient): String
}

/**
 * Flutter `AuthTokenRefreshInterceptor`（QueuedInterceptor，单飞）的 KMP 对等实现：
 * 装饰任意 [ApiClient]，在 getApi/postApi/putApi/patchApi/deleteApi 出口按 401 语义刷新并重试一次。
 *
 * 与 Flutter 的差异（刻意为之）：KMP 侧 401 流的最终 owner 是本装饰器——
 * 启用后平台 client 不再挂 onTokenExpired，由这里统一「刷新→重试 / 清会话」。
 */
public class AuthRefreshingApiClient(
    private val delegate: ApiClient,
    private val policy: AuthRefreshPolicy? = null,
    private val refresher: TokenRefreshHandler? = null,
    private val onSessionCleared: (() -> Unit)? = null,
) : ApiClient {

    /** 登录/注册/刷新/登出路径不参与刷新（Flutter 同款豁免，防递归与死循环）。 */
    private fun isAuthExemptPath(path: String): Boolean {
        val p = path.trim().trimStart('/')
        return p.contains("/user/login") ||
            p.contains("/user/register") ||
            p.contains("/user/refresh") ||
            p.contains("/user/logout")
    }

    override suspend fun getRaw(path: String, query: Map<String, String>): String =
        delegate.getRaw(path, query)

    override suspend fun getAbsoluteRaw(url: String): String = delegate.getAbsoluteRaw(url)

    override suspend fun postRaw(path: String, body: String): String =
        delegate.postRaw(path, body)

    override suspend fun postForm(path: String, fields: Map<String, String>): HttpTextResponse =
        delegate.postForm(path, fields)

    override suspend fun putRaw(path: String, body: String): String =
        delegate.putRaw(path, body)

    override suspend fun patchRaw(path: String, body: String): String =
        delegate.patchRaw(path, body)

    override suspend fun deleteRaw(path: String): String = delegate.deleteRaw(path)

    override suspend fun <T> getApi(
        path: String,
        query: Map<String, String>,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = withRefresh(path) { delegate.getApi(path, query, parseData) }

    override suspend fun <T> postApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = withRefresh(path) { delegate.postApi(path, body, parseData) }

    override suspend fun <T> putApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = withRefresh(path) { delegate.putApi(path, body, parseData) }

    override suspend fun <T> patchApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = withRefresh(path) { delegate.patchApi(path, body, parseData) }

    override suspend fun <T> deleteApi(
        path: String,
        query: Map<String, String>,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = withRefresh(path) { delegate.deleteApi(path, query, parseData) }

    /**
     * 流式 POST：**只转发，不进刷新流程**（刻意与五个 `*Api` 出口不同）。
     *
     * 1. 会话头由被装饰的平台 client 在 open 时注入（`DemoRequestHeaders` /
     *    Ktor 的 `DemoDynamicHeaders`），与 Dart 的 `HeaderInterceptor` 一致；
     *    流一旦建立，中途刷新出来的新 token 也救不了这条连接（Dart 同理）。
     * 2. 流无法「重放」：withRefresh 的重试前提是响应体可重复解析，而 SSE 的 body 是
     *    一次性消费的分片序列；重发一次 POST = 服务端再生成一遍（契约表 §5.3 的
     *    at-least-once 雷区），所以 401 决策只能交给上层「重新提问」。
     * 3. Dart 行为等价：`SseClient.post` 走 `dio.post` 且 `validateStatus: status < 600`
     *    （sse_client.dart:46），401 不会变成 `DioException`，`AuthTokenRefreshInterceptor`
     *    因此**根本没机会**刷新；最终以 `SseException(error = "unauthorized")`
     *    抛给调用方（sse_client.dart:78-81 → sse_exception.dart:49-66）。
     *
     * 结论：这里不需要 [isAuthExemptPath] 那样的豁免表——SSE 路径压根不参与刷新。
     */
    override fun openSseTextStream(
        path: String,
        body: String,
        headers: Map<String, String>,
    ): Flow<String> = delegate.openSseTextStream(path, body, headers)

    /**
     * 首次应答 → 决策 → Refresh 代发一次 / ForceLogout 清会话 / PassThrough 透传。
     * 刷新失败时实现方已清会话，这里原样返回首次应答（业务层按 401/10021 提示）。
     */
    private suspend fun <T> withRefresh(
        path: String,
        call: suspend () -> ApiResponse<T>,
    ): ApiResponse<T> {
        val p = policy ?: return call()
        val r = refresher ?: return call()
        val first = call()
        if (isAuthExemptPath(path)) return first
        return when (p.decide(first.httpStatus, first.code, first.message)) {
            AuthRefreshDecision.PassThrough -> first
            AuthRefreshDecision.ForceLogout -> {
                // 互踢/会话无效：会话已废，清会话回登录（与业务层 clearing 幂等）。
                onSessionCleared?.invoke()
                first
            }
            AuthRefreshDecision.Refresh -> {
                val refreshed = try {
                    r.refresh(delegate)
                    true
                } catch (_: Throwable) {
                    // 刷新耗尽：实现方已走 handleRefreshExhausted 清会话。
                    false
                }
                if (refreshed) call() else first
            }
        }
    }
}
