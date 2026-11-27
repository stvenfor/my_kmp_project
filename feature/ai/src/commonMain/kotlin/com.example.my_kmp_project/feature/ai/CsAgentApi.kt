package com.example.my_kmp_project.feature.ai

import com.example.my_kmp_project.core.network.NetworkJson
import com.example.my_kmp_project.core.network.SseTransport
import com.example.my_kmp_project.core.sse.AiStreamEvent
import com.example.my_kmp_project.core.sse.SseParser
import com.example.my_kmp_project.core.sse.toAiStreamEventOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/**
 * `POST /api/v1/cs-agent/chat` 契约（后端 `cs_agent_routes.go:13`，契约表 B 区 #1）。
 *
 * 真相源：Go `my_go_study`
 * - 路由：`app/internal/delivery/http/router/cs_agent_routes.go:13` 在 `/cs-agent` 组挂 POST /chat，
 *   走 `sessionAuthChain`；
 * - 请求：`app/internal/delivery/http/dto/request/cs_agent_chat.go:4` `CsAgentChatRequest`
 *   （`prompt` / `clientRequestId`，**camelCase**，与 [AiSseApi] 的 completions 同款）；
 * - 响应：`app/internal/delivery/http/controller/cs_agent_controller.go:59-93` 发
 *   `text/event-stream`，`writeSSEFrame`（`sse_controller.go:111-118`）写
 *   `event: meta|delta|done|error` + `data: <json>` 帧 —— 与 `/sse/completions` 同一帧协议，
 *   因此复用 [SseParser] / [toAiStreamEventOrNull]，不另造解析器。
 *
 * 仿 [PaymentPrepayApi] 样板：**只登记后端真实路由契约**（路径常量 + 请求模型 +
 * 可调用方法 + 契约测试），不自造 UI 调用点，也不改后端仓库。
 * 与 [AiSseApi] 分开：两者是不同路由（`/cs-agent/chat` vs `/sse/completions`），
 * 但帧协议一致，故共用 `core/sse` 解码。
 */
public class CsAgentApi(
    private val transport: SseTransport,
) {
    /**
     * 一次提问 → 一段 `meta`/`delta`/`done`/`error` 事件流（冷流，collect 才发请求）。
     *
     * 请求体是 camelCase（`prompt` / `clientRequestId`），后端 `CsAgentChatRequest` 同形；
     * 两个 id 为 null 时不发该键（[NetworkJson] 关掉 `explicitNulls`）。
     */
    public fun streamChat(prompt: String, clientRequestId: String? = null): Flow<AiStreamEvent> = flow {
        val body = NetworkJson.encodeToString(CsAgentChatRequest(prompt, clientRequestId))
        val parser = SseParser()
        transport.openSseTextStream(CsAgentContract.CHAT_PATH, body).collect { chunk ->
            parser.feed(chunk).forEach { frame ->
                frame.toAiStreamEventOrNull()?.let { emit(it) }
            }
        }
        parser.flush().forEach { frame ->
            frame.toAiStreamEventOrNull()?.let { emit(it) }
        }
    }
}

/** 客服 Agent chat 契约常量（对齐 `cs_agent_routes.go:13`）。 */
public object CsAgentContract {
    /** `POST /api/v1/cs-agent/chat`。 */
    public const val CHAT_PATH: String = "/api/v1/cs-agent/chat"

    /**
     * HTTP verb。SSE 流由 `SseTransport.openSseTextStream` 承载，该 seam 恒为**流式 POST**
     * （`core/network/Sse.kt:45`）；单列常量以便契约测试显式断言 verb。
     */
    public const val METHOD: String = "POST"
}

/** `POST /cs-agent/chat` body（Go `cs_agent_chat.go:4` `CsAgentChatRequest`，camelCase）。 */
@Serializable
public data class CsAgentChatRequest(
    val prompt: String,
    @SerialName("clientRequestId") val clientRequestId: String? = null,
)
