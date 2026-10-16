package com.example.my_kmp_project.core.network.realtime

/**
 * Realtime 子系统专用异常（HTTP 换票 / 增量补投 / WS 传输能力）。
 *
 * 为什么不往 [com.example.my_kmp_project.core.network.NetworkError] 里加变体：
 * 该 sealed 由网络 lane 独占（本次改动禁止编辑那些文件），
 * Realtime 专属语义放在自己的 sealed 里，与 [com.example.my_kmp_project.core.network.NetworkError.Transport]
 * 并列抛出，上层按 `Throwable` 分支处理即可。
 */
public sealed class RealtimeWsError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /**
     * 当前构建**没有 WebSocket 能力**（ohosX64：CPF 未发布 Ktor / WebSockets 的 ohosX64 klib）。
     * 语义即「传输层不支持」，上层据此直接判失败、不进入重连死循环。
     */
    public class Unsupported(
        message: String = "当前平台没有可用的 WebSocket 传输（ohosX64：无 CPF Ktor/WebSockets 产物）",
    ) : RealtimeWsError(message)

    /**
     * 裸 body 解析失败。
     * Dart 对应 `Map<String, dynamic>.from(json as Map)` 抛错后被 Dio 错误转换器包成
     * `HttpRequestException`（`commons/network/lib/http/http.dart:473-478`）；
     * 把 `{code,data}` 信封体喂给这两个接口同样走到这里。
     */
    public class BodyUnparsable(
        val raw: String,
        cause: Throwable?,
    ) : RealtimeWsError("Realtime 裸 body 解析失败：${raw.realtimePreview()}", cause)

    /**
     * 换票响应可用但 `wsUrl` 为空。
     * Dart 会在 `WebSocketChannel.connect(Uri.parse(''))` 处抛；KMP 提前失败，
     * 让 telemetry 里看到的是「票无效」而不是「socket 连接异常」。
     */
    public class TicketInvalid(
        message: String,
    ) : RealtimeWsError(message)
}

/** 错误文案里只带响应体前 200 字符，避免整包 JSON 进日志。 */
private fun String.realtimePreview(): String = take(200).replace('\n', ' ')
