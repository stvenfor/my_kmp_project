package com.example.my_kmp_project.core.network.realtime

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.NetworkJson
import com.example.my_kmp_project.core.network.remapBackendLocalhost
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/**
 * HTTP 换票：`POST /api/v1/realtime/ws-ticket`（契约表 §2.17 行 #97）。
 *
 * 对齐 Dart `components/realtime/lib/api/ws_ticket_api.dart:14-75`（d7876a1）：
 * - 请求体固定 `{"platform":"mobile"}`（:25-27），`connId` 可选、空则省略键
 * - `accessToken` 为空 **先抛** `IllegalStateException`（Dart `StateError`，:20-22）
 * - 响应是**裸 body、camelCase、无 `{code,data}` 信封**：走 [ApiClient.postRaw] +
 *   [NetworkJson] 直接解码（Dart 的 `converter` 收到的是 `response.data` 整包，
 *   `http.dart:145-146`，同样不剥信封）；把信封体喂进来必然解析失败
 *
 * Authorization / X-Session-ID / X-Device-ID 由 [NetworkFacade] 的统一头注入，
 * 与 Dart「换票走 HTTP 而不是 WS 首帧带 JWT」的理由一致（:10-12）。
 */
public class RealtimeWsTicketApi(
    private val client: ApiClient = NetworkFacade.api(),
) {
    /**
     * 换一张短期票。返回的 [RealtimeWsTicketData.wsUrl] **必须原样拨号**（§5 反直觉语义 4），
     * 只做 loopback 改写（[resolveRealtimeWsUrl]），禁止本地拼地址。
     *
     * [connId] 对应 Go `request.RealtimeTicketRequest.ConnID`（`realtime_request.go:6`），
     * 重连复用连接时传入；为空则省略该键（服务端自行生成）。
     */
    public suspend fun fetchTicket(accessToken: String, connId: String? = null): RealtimeWsTicketData {
        if (accessToken.isEmpty()) {
            // Dart: throw StateError('缺少 access token，请先登录')（ws_ticket_api.dart:21）
            throw IllegalStateException("缺少 access token，请先登录")
        }
        val raw = client.postRaw(
            path = RealtimeConfig.TICKET_PATH,
            body = NetworkJson.encodeToString(RealtimeWsTicketRequest(connId = connId)),
        )
        val data = runCatching {
            NetworkJson.decodeFromString(RealtimeWsTicketData.serializer(), raw)
        }.getOrElse { cause ->
            // 解码失败先判后端 {error,code} 错误体（如 401 未授权），命中则保留码与文案，
            // 否则才回落到「响应体不可解析」，避免把后端 401 吞成解析错误。
            throw (parseBackendRejection(raw) ?: RealtimeWsError.BodyUnparsable(raw, cause))
        }
        if (data.wsUrl.isEmpty()) {
            throw RealtimeWsError.TicketInvalid("WS ticket 缺少 wsUrl（服务端未下发拨号地址）")
        }
        return data
    }
}

/** 换票请求体：`platform` 恒为 `mobile`（ws_ticket_api.dart:26）；`connId` 可选、空则省略键。 */
@Serializable
public class RealtimeWsTicketRequest(
    public val platform: String = RealtimeConfig.PLATFORM_MOBILE,
    public val connId: String? = null,
)

/**
 * 换票响应（Go `RealtimeTicketData`，**camelCase**，ws_ticket_api.dart:48-68）：
 * `{ ticket, wsUrl, expiresInSeconds, connId }`。
 *
 * 四个键都是**必填**（缺任一即 `MissingFieldException`）——
 * 这是「信封体不被接受」的机制：`{code,data}` 包裹时顶层四个键全部缺失 ⇒ 直接失败。
 * 单键值为 `null` / 类型不符时按 Dart 的 `?? ''` / `?? 0` 宽容回退。
 */
@Serializable
public class RealtimeWsTicketData(
    @Serializable(with = RealtimeTextSerializer::class)
    public val ticket: String,
    @Serializable(with = RealtimeTextSerializer::class)
    public val wsUrl: String,
    @Serializable(with = RealtimeLongSerializer::class)
    public val expiresInSeconds: Long,
    @Serializable(with = RealtimeTextSerializer::class)
    public val connId: String,
)

/**
 * `wsUrl` 落地：Dart `BackendWsConfig.resolveWsUrl`（`backend_ws_config.dart:9-12`）
 * = 与 HTTP 同一套 loopback 改写（Android 模拟器 `127.0.0.1` → `10.0.2.2`，真机用注入 host）。
 * 除 host 改写外**不动 scheme / 端口 / 路径**，也不允许由本地配置合成地址。
 */
public fun resolveRealtimeWsUrl(wsUrl: String): String = remapBackendLocalhost(wsUrl)
