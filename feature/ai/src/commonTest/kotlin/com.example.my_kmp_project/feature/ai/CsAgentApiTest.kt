package com.example.my_kmp_project.feature.ai

import com.example.my_kmp_project.core.network.SseOpenGate
import com.example.my_kmp_project.core.network.SseTransport
import com.example.my_kmp_project.core.sse.AiStreamEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `POST /api/v1/cs-agent/chat` 契约回归（`cs_agent_routes.go:13`，B 区 #1）。
 * 只断言请求形状（路径 / HTTP verb / 请求体字段名）与 `meta|delta|done` 帧解码，不接 UI。
 */
class CsAgentApiTest {

    private class FakeSseTransport(
        private val bodyChunks: List<String> = listOf("event: done\ndata: {}\n\n"),
    ) : SseTransport {
        var requestedPath: String? = null
        var requestedBody: String? = null
        var openCount: Int = 0

        override fun openSseTextStream(
            path: String,
            body: String,
            headers: Map<String, String>,
        ): Flow<String> {
            openCount++
            requestedPath = path
            requestedBody = body
            return flow {
                val gate = SseOpenGate(
                    statusCode = 200,
                    contentType = "text/event-stream",
                    chunks = flowOf(*bodyChunks.toTypedArray()),
                )
                emitAll(gate.eventStreamText())
            }
        }
    }

    @Test
    fun chatPathAndVerbMatchBackendRoute() {
        assertEquals("/api/v1/cs-agent/chat", CsAgentContract.CHAT_PATH)
        // SSE seam 是流式 POST（core/network/Sse.kt:45）。
        assertEquals("POST", CsAgentContract.METHOD)
    }

    @Test
    fun chatRequestIsCamelCaseAndNullIdOmitted() = runBlocking {
        val transport = FakeSseTransport()
        CsAgentApi(transport).streamChat("你好").toList()
        assertEquals("/api/v1/cs-agent/chat", transport.requestedPath)
        // cs_agent_chat.go:4 CsAgentChatRequest：prompt / clientRequestId（camelCase）。
        assertEquals("""{"prompt":"你好"}""", transport.requestedBody)

        val withId = FakeSseTransport()
        CsAgentApi(withId).streamChat("再问一次", clientRequestId = "r-7").toList()
        assertEquals("""{"prompt":"再问一次","clientRequestId":"r-7"}""", withId.requestedBody)
    }

    @Test
    fun metaDeltaDoneFramesDecodeInOrder() = runBlocking {
        val transport = FakeSseTransport(
            bodyChunks = listOf(
                "event: meta\ndata: {\"requestId\":\"r1\",\"clientRequestId\":\"cr1\"}\n\n",
                "event: delta\ndata: {\"text\":\"您\"}\n\n",
                "event: delta\ndata: {\"text\":\"好\"}\n\n",
                "event: done\ndata: {\"finishReason\":\"stop\"}\n\n",
            ),
        )
        val events = CsAgentApi(transport).streamChat("你好").toList()
        assertEquals(
            listOf(
                AiStreamEvent.Meta(requestId = "r1", clientRequestId = "cr1"),
                AiStreamEvent.Delta("您"),
                AiStreamEvent.Delta("好"),
                AiStreamEvent.Done("stop"),
            ),
            events,
        )
    }
}
