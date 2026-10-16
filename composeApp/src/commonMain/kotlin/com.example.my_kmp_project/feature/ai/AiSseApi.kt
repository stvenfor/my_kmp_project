package com.example.my_kmp_project.feature.ai

import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.NetworkJson
import com.example.my_kmp_project.core.network.SseTransport
import com.example.my_kmp_project.core.sse.AiStreamEvent
import com.example.my_kmp_project.core.sse.SseParser
import com.example.my_kmp_project.core.sse.toAiStreamEventOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 契约表 #96 —— `POST /api/v1/sse/completions`（AI 小石头）。
 *
 * Dart SoT：`features/ai/lib/ai/api/ai_sse_api.dart`（`streamCompletion` :14-36、
 * `_mapFrame` :38-87）+ `commons/network/lib/sse/sse_client.dart`。
 * 分工：传输（含 UTF-8 增量解码 + 前流判定）在 `:core:network`
 * （[SseTransport] / `SseOpenGate`），帧协议在 `core/sse`（[SseParser] /
 * [toAiStreamEventOrNull]），本对象只做「组包 → 喂解析器 → 出领域事件」。
 *
 * 请求体是 **camelCase**（`conversationId` / `clientRequestId`），与 `/api/v1`
 * 其余端点的 snake_case 不同 —— 契约表 §2.17 行 #96 明确标注，别「顺手统一」。
 */
public class AiSseApi(
    private val transport: SseTransport = NetworkFacade.api(),
) {
    public companion object {
        /** Dart `AiSseApi.completionsPath`（ai_sse_api.dart:10）。 */
        public const val COMPLETIONS_PATH: String = "/api/v1/sse/completions"
    }

    /**
     * 一次提问 → 一段事件流（`meta` → `delta`×N → `done`/`error`）。
     *
     * 冷流：collect 才发请求；协程取消即断流（Dart `CancelToken`，sse_client.dart:36,94）。
     *
     * 失败语义（Dart 逐条对等，勿「顺手改成事件」）：
     * - **前流**（未升级成 `text/event-stream`，或 status != 200）：传输层读净 body 后抛
     *   [com.example.my_kmp_project.core.network.SseException]
     *   （`SseException.fromPreStream` 解析 `{"error":…}` / `{"code":…,"message":…}`
     *   信封，sse_client.dart:78-81 + sse_exception.dart:12-40）⇒ 本流**抛**，
     *   这种 body 绝不喂 [SseParser]；
     * - **流内**失败：只能是 `event: error` 帧 ⇒ 发 [AiStreamEvent.Error]，不抛
     *   （Dart `sse_client.dart:77-82`「一旦升级成 text/event-stream，后续错误必须走
     *   event: error，不得中途改成 JSON 信封」）；
     * - 无法识别 / 空 delta 的帧按 Dart 直接丢弃（ai_sse_api.dart:48-50、:67）。
     */
    public fun streamCompletion(
        prompt: String,
        conversationId: String? = null,
        clientRequestId: String? = null,
    ): Flow<AiStreamEvent> = flow {
        val body = completionsRequestBody(prompt, conversationId, clientRequestId)
        val parser = SseParser()
        transport.openSseTextStream(COMPLETIONS_PATH, body).collect { chunk ->
            parser.feed(chunk).forEach { frame ->
                frame.toAiStreamEventOrNull()?.let { emit(it) }
            }
        }
        // Dart parseSseByteStream 收尾（sse_client.dart:101-103）：EOF 仍要 flush。
        parser.flush().forEach { frame ->
            frame.toAiStreamEventOrNull()?.let { emit(it) }
        }
    }
}

/**
 * Dart `AiSseApi.streamCompletion` 的 body 组装（ai_sse_api.dart:20-26）：
 * `prompt` 必发；两个 id **为 null 或空串都不发**（不是发 `null`、也不是发 `""`）。
 */
internal fun completionsRequestBody(
    prompt: String,
    conversationId: String?,
    clientRequestId: String?,
): String = NetworkJson.encodeToString(
    JsonObject.serializer(),
    buildJsonObject {
        put("prompt", prompt)
        if (!conversationId.isNullOrEmpty()) put("conversationId", conversationId)
        if (!clientRequestId.isNullOrEmpty()) put("clientRequestId", clientRequestId)
    },
)
