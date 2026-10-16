package com.example.my_kmp_project.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/* --------------------------------------------------------------------- *
 * 契约表 #96 —— `POST /api/v1/sse/completions` 的**传输层**。
 *
 * Dart SoT: `commons/network/lib/sse/sse_client.dart` + `sse_exception.dart`
 * （my_ai_project d7876a1）。帧协议不在本模块：`SseParser` / `AiSseDecoder`
 * 住在 `composeApp` 的 `core/sse`，本层只负责把响应的**字节流按 UTF-8 增量解码**
 * 成文本分片（Dart `utf8.decoder.bind(byteStream)`，sse_client.dart:93），
 * 好让解析器继续吃 `String`。
 * --------------------------------------------------------------------- */

/** Dart `Headers.acceptHeader: 'text/event-stream'`（sse_client.dart:43）。 */
public const val SSE_ACCEPT_HEADER_VALUE: String = "text/event-stream"

/**
 * Dart `validateStatus: (status) => status != null && status < 600`（sse_client.dart:46）：
 * 状态码本身**从不**构成失败；只有「是不是 event-stream」才决定升级还是报前流错误。
 */
public const val SSE_UPGRADE_STATUS: Int = 200

/** Dart `error: 'connection_error'`（sse_client.dart:59）。 */
public const val SSE_ERROR_CONNECTION: String = "connection_error"

/**
 * KMP 独有：本平台传输根本不能流式（ohosX64 的 `libnet_http` cinterop）——
 * 见 [SseStreamingUnsupportedException] 与 GAP(R5)。
 */
public const val SSE_ERROR_TRANSPORT_UNSUPPORTED: String = "streaming_unsupported"

/**
 * 流式 POST 的 seam（Dart `SseClient.post`，sse_client.dart:25-84）。
 *
 * 返回**冷流**：只有被 collect 才真的发请求；元素是 UTF-8 解码后的文本分片，
 * 分片边界与 TCP/HTTP 块一致，不保证按行 —— 交给 `SseParser.feed(chunk)` 自行切行。
 *
 * 语义（与 Dart 逐条对齐）：
 * - `Accept: text/event-stream` + JSON body，POST；
 * - HTTP status 单独不构成错误（`validateStatus 200–599`）；判据是 content-type：
 *   非 `text/event-stream`（或 status != 200）就是**前流失败**，body 读完按信封解析后
 *   抛 [SseException]（[SseOpenGate]，Dart sse_client.dart:63-81）；
 * - 升级成事件流以后，错误只会是流里的 `event: error` 帧（Dart `sse_client.dart:77-82`
 *   只在升级前抛信封，升级后 `yield* parseSseByteStream` 不再改信封），
 *   传输层不翻译它 —— 那是 `AiSseDecoder` 的事；
 * - 取消 = 取消收集协程（Dart 的 `CancelToken`，sse_client.dart:36,94）。
 */
public interface SseTransport {
    /**
     * 打开一次 SSE 流。[headers] 追加在 `Accept` 之后（同名覆盖），
     * Bearer / `X-Session-ID` 等会话头仍由平台 client 的既有拦截逻辑注入
     * （Dart 由 `HeaderInterceptor` 统一加，本类只覆盖 `Accept` —— sse_client.dart:15）。
     */
    public fun openSseTextStream(
        path: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
    ): Flow<String>
}

/**
 * SSE 失败：前流（尚未升级成 event-stream）或连接层失败。
 *
 * Dart `SseException`（`commons/network/lib/sse/sse_exception.dart:4-71`）逐字段对等：
 * [statusCode] 可为 null（连接根本没建立），[error] 是稳定错误码，[rawBody] 保留原文。
 */
public open class SseException(
    message: String,
    public val statusCode: Int? = null,
    public val error: String? = null,
    public val rawBody: String? = null,
) : Exception(message) {

    public companion object {
        /**
         * Dart `SseException.fromPreStream`（sse_exception.dart:12-40）：
         * `{"error":"unauthorized"}` 优先取 `error`，否则取 `message`，再否则整段正文；
         * 空正文回落 `"SSE request failed ($statusCode)"`；错误码取 JSON `error`，
         * 缺失时按 status 映射（[defaultErrorCode]）。
         *
         * 注意与 BFF 信封的关系：流前错误体是 `{"error":"..."}`（Supabase / Gin 裸错）
         * 或 `{"code":…,"message":…}`，**两种都在这一个函数里**读 `error`→`message`，
         * 因此绝不把 `data` 当内容、也绝不把非 2xx 状态当成功。
         */
        public fun fromPreStream(statusCode: Int, body: String): SseException {
            val trimmed = body.trim()
            var errorField: String? = null
            var message = if (trimmed.isEmpty()) "SSE request failed ($statusCode)" else trimmed
            val element = runCatching { NetworkJson.parseToJsonElement(body) }.getOrNull()
            if (element is JsonObject) {
                val err = element["error"]
                if (err != null && err !is JsonNull) {
                    errorField = err.asDisplayText()
                    message = errorField
                } else {
                    element["message"]?.takeIf { it !is JsonNull }?.let { message = it.asDisplayText() }
                }
            }
            return SseException(
                message = message,
                statusCode = statusCode,
                error = errorField ?: defaultErrorCode(statusCode),
                rawBody = body,
            )
        }

        /** Dart `_defaultErrorCode`（sse_exception.dart:49-66）。 */
        public fun defaultErrorCode(statusCode: Int): String = when (statusCode) {
            400 -> "bad_request"
            401 -> "unauthorized"
            403 -> "forbidden"
            404 -> "not_found"
            429 -> "rate_limited"
            503 -> "unavailable"
            else -> "http_$statusCode"
        }

        /** `Map`/`List` 打印成 JSON，字符串取内容 —— Dart `decoded['error'].toString()`。 */
        private fun JsonElement.asDisplayText(): String = when (this) {
            is JsonPrimitive -> contentOrNull ?: toString()
            else -> toString()
        }
    }

    override fun toString(): String =
        "SseException(message: $message, statusCode: $statusCode, error: $error)"
}

/**
 * 本平台传输不具备流式能力（KMP 专有，Dart 无对等物）。
 *
 * `// GAP(R5)`：ohosX64 的 `net_http` cinterop 只在完成回调里给**整包** body，
 * 没有增量分片可读。这里明确抛类型化错误，不伪造「OHOS x64 已就绪」。
 * ohosArm64 有 CPF Ktor + CIO，走 [SseTransport] 的真实现。
 */
public class SseStreamingUnsupportedException(
    transport: String,
    path: String,
) : SseException(
    message = "SSE streaming is not supported by the $transport transport: POST $path",
    error = SSE_ERROR_TRANSPORT_UNSUPPORTED,
)

/**
 * Dart `SseClient.post` 打开响应后的**判定三件套**（sse_client.dart:63-81）。
 *
 * 平台实现拿到 status / content-type / 已解码分片冷流后交给它：
 * 只有 `status == 200 && content-type 含 text/event-stream` 才把分片流放出去，
 * 否则读净 body（Dart `_readResponseBody`，:106-112）并按 [SseException.fromPreStream] 抛。
 *
 * 与 Dart 的一处差异（无害）：Dart 还检查 `response.data == null → 'empty_body'`
 * （:67-73）；KMP 侧 body 恒为一个（可能为空的分片流），空流自然结束成 0 帧，
 * 因此不再有 `empty_body` 分支。
 */
public class SseOpenGate(
    public val statusCode: Int,
    public val contentType: String?,
    public val chunks: Flow<String>,
) {
    /** Dart `contentType.toLowerCase().contains('text/event-stream')`（sse_client.dart:75-77）。 */
    public val isEventStream: Boolean
        get() = contentType?.contains(SSE_ACCEPT_HEADER_VALUE, ignoreCase = true) == true

    /** 升级成功 ⇒ 原样返回分片流；否则读净正文并抛前流错误。 */
    public suspend fun eventStreamText(): Flow<String> {
        if (statusCode == SSE_UPGRADE_STATUS && isEventStream) return chunks
        throw SseException.fromPreStream(statusCode, readAllText())
    }

    /** 把剩余分片拼成一个文本（前流错误体通常是单个 JSON 信封）。 */
    public suspend fun readAllText(): String {
        val builder = StringBuilder()
        chunks.collect { builder.append(it) }
        return builder.toString()
    }
}

/**
 * 有状态的 UTF-8 增量解码器 —— Dart `utf8.decoder.bind(byteStream)`
 * （`commons/network/lib/sse/sse_client.dart:93`）的对等物。
 *
 * 分片可能在多字节字符中间切断（中文回复里天天发生），因此把「不完整序列的尾字节」
 * 留到下一片再解，切分处不会吐出乱码。
 *
 * 非法字节用 U+FFFD 替换而不是抛异常：Dart 的 `_readResponseBody` 同样
 * `allowMalformed: true`（sse_client.dart:111）；流式路径抛编码异常会让整段回复报废。
 */
internal class Utf8ChunkDecoder {

    private var pending = ByteArray(0)

    fun decode(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size - offset): String {
        require(offset >= 0 && length >= 0 && offset + length <= bytes.size) {
            "utf8 decode window out of bounds: offset=$offset length=$length size=${bytes.size}"
        }
        if (length == 0) return if (pending.isEmpty()) "" else decode(pending, 0, pending.size)
        val work = if (pending.isEmpty()) {
            bytes.copyOfRange(offset, offset + length)
        } else {
            pending + bytes.copyOfRange(offset, offset + length)
        }
        pending = EMPTY

        val out = StringBuilder(work.size)
        var i = 0
        while (i < work.size) {
            val lead = work[i].toInt() and 0xFF
            val width = sequenceWidth(lead)
            if (width == 0) {
                out.append(REPLACEMENT)
                i++
                continue
            }
            if (i + width > work.size) {
                // 被切断的多字节序列：整段留到下一片。
                pending = work.copyOfRange(i, work.size)
                break
            }
            if (!hasContinuations(work, i, width)) {
                out.append(REPLACEMENT)
                i++
                continue
            }
            val codePoint = when (width) {
                1 -> lead
                2 -> (lead and 0x1F) shl 6 or continuation(work[i + 1])
                3 -> (lead and 0x0F) shl 12 or (continuation(work[i + 1]) shl 6) or continuation(work[i + 2])
                else -> (lead and 0x07) shl 18 or
                    (continuation(work[i + 1]) shl 12) or
                    (continuation(work[i + 2]) shl 6) or
                    continuation(work[i + 3])
            }
            if (codePoint < 0x10000) {
                out.append(codePoint.toChar())
            } else {
                val surrogate = codePoint - 0x10000
                out.append((0xD800 + (surrogate shr 10)).toChar())
                out.append((0xDC00 + (surrogate and 0x3FF)).toChar())
            }
            i += width
        }
        return out.toString()
    }

    /** 流结束时调用：残留的不完整尾巴按乱码处理（Dart 在 EOF 也会 flush）。 */
    fun finish(): String {
        val leftover = pending.size
        pending = EMPTY
        return if (leftover == 0) "" else buildString(leftover) { repeat(leftover) { append(REPLACEMENT) } }
    }

    private fun continuation(byte: Byte): Int = byte.toInt() and 0x3F

    private fun hasContinuations(bytes: ByteArray, at: Int, width: Int): Boolean {
        for (step in 1 until width) {
            val part = bytes[at + step].toInt() and 0xFF
            if (part < 0x80 || part > 0xBF) return false
        }
        return true
    }

    private companion object {
        val EMPTY = ByteArray(0)
        const val REPLACEMENT = '\uFFFD'

        /** 序列宽度；0 = 不是合法前导字节（孤立延续字节 / overlong / 0xF8+）。 */
        fun sequenceWidth(lead: Int): Int = when {
            lead < 0x80 -> 1
            lead in 0xC2..0xDF -> 2
            lead in 0xE0..0xEF -> 3
            lead in 0xF0..0xF4 -> 4
            else -> 0
        }
    }
}
