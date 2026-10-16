package com.example.my_kmp_project.feature.ai

import com.example.my_kmp_project.core.network.SseException
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
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * 契约表 #96 的 feature 侧（Dart `features/ai/lib/ai/api/ai_sse_api.dart`，
 * 消费 `commons/network/lib/sse/sse_client.dart`）：
 * 「组包 → 喂 [com.example.my_kmp_project.core.sse.SseParser] → 出 [AiStreamEvent]」，
 * 外加两条**不能混**的错误通道：流前 = 信封异常抛出，流内 = `event: error` 事件。
 */
class AiSseApiTest {

    // ── fake 传输：与 KtorApiClient 一样经 SseOpenGate 判定（前流不喂解析器） ─────

    private class FakeSseTransport(
        private val statusCode: Int = 200,
        private val contentType: String? = "text/event-stream",
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
                    statusCode = statusCode,
                    contentType = contentType,
                    chunks = flowOf(*bodyChunks.toTypedArray()),
                )
                emitAll(gate.eventStreamText())
            }
        }
    }

    @Test
    fun meta_delta_times_n_done_decode_in_order_across_split_chunks() = runBlocking {
        val transport = FakeSseTransport(
            bodyChunks = listOf(
                // 分片故意切在字段名 / data 中间：帧边界由 SseParser 负责，不由传输层负责。
                "event: meta\nda",
                "ta: {\"requestId\":\"r1\",\"conversationId\":\"c1\",\"model\":\"qwen\",\"clientRequestId\":\"cr1\"}\n",
                "\n", // meta 的空行落在下一个分片
                "event: delta\ndata: {\"text\":\"你\"}\n\nevent: delta\ndata: {\"text\":\"\", \"note\":\"空 delta 丢弃\"}\n\n",
                "event: delta\ndata: {\"text\":\"好\",\"ignored\":1}\n\n",
                "event: delta\ndata: {\"text\":\"！\"}\n\n",
                "event: done\ndata: {\"finishReason\":\"stop\"}\n\n",
            ),
        )

        val events = AiSseApi(transport).streamCompletion("你好").toList()

        assertEquals(
            listOf(
                AiStreamEvent.Meta(
                    requestId = "r1",
                    conversationId = "c1",
                    model = "qwen",
                    clientRequestId = "cr1",
                ),
                AiStreamEvent.Delta("你"),
                AiStreamEvent.Delta("好"),
                AiStreamEvent.Delta("！"),
                AiStreamEvent.Done("stop"),
            ),
            events,
        )
    }

    @Test
    fun in_stream_error_frame_is_an_event_and_does_not_throw() = runBlocking {
        val transport = FakeSseTransport(
            bodyChunks = listOf(
                "event: meta\ndata: {\"requestId\":\"r1\"}\n\n",
                "event: delta\ndata: {\"text\":\"我\"}\n\n",
                // 升级成功后错误只能走帧（Dart sse_client.dart:77-82）。
                "event: error\ndata: {\"error\":{\"code\":\"upstream_timeout\",\"message\":\"模型上游超时\"}}\n\n",
            ),
        )

        val events = AiSseApi(transport).streamCompletion("讲个故事").toList()

        assertEquals(
            listOf(
                AiStreamEvent.Meta(requestId = "r1"),
                AiStreamEvent.Delta("我"),
                AiStreamEvent.Error(code = "upstream_timeout", message = "模型上游超时"),
            ),
            events,
        )
    }

    @Test
    fun pre_stream_json_envelope_is_reported_as_an_error_and_never_parsed() = runBlocking {
        val transport = FakeSseTransport(
            statusCode = 401,
            contentType = "application/json",
            bodyChunks = listOf("""{"code":10023,"message":"登录失效，请重新登录"}"""),
        )
        val events = mutableListOf<AiStreamEvent>()

        val error = assertFailsWith<SseException> {
            AiSseApi(transport).streamCompletion("你好").collect { events += it }
        }

        // Dart SseException.fromPreStream：`error` 缺失 ⇒ 取信封 `message`，错误码按 status 映射。
        assertEquals(401, error.statusCode)
        assertEquals("登录失效，请重新登录", error.message)
        assertEquals("unauthorized", error.error)
        assertTrue(events.isEmpty(), "a pre-stream body must never reach the SSE parser")
    }

    @Test
    fun request_is_camel_case_and_blank_ids_are_omitted_not_sent_empty() = runBlocking {
        val transport = FakeSseTransport()
        val api = AiSseApi(transport)

        // 冷流：没 collect 之前不该碰传输层（Dart async* 同样惰性）。
        val flow = api.streamCompletion("hi", conversationId = "", clientRequestId = null)
        assertEquals(0, transport.openCount)
        flow.toList()

        assertEquals("/api/v1/sse/completions", transport.requestedPath)
        assertEquals("""{"prompt":"hi"}""", transport.requestedBody)

        val withIds = FakeSseTransport()
        AiSseApi(withIds).streamCompletion(
            prompt = "再来一次",
            conversationId = "c-7",
            clientRequestId = "r-7",
        ).toList()
        // 契约表 §2.17 行 #96：**camelCase**，别按其余端点的 snake_case 改。
        assertEquals(
            """{"prompt":"再来一次","conversationId":"c-7","clientRequestId":"r-7"}""",
            withIds.requestedBody,
        )

        val blankSecond = FakeSseTransport()
        AiSseApi(blankSecond).streamCompletion("p", conversationId = "", clientRequestId = "  ").toList()
        assertEquals("""{"prompt":"p","clientRequestId":"  "}""", blankSecond.requestedBody)
    }

    @Test
    fun trailing_frame_without_blank_line_is_dropped_like_dart() = runBlocking {
        // Dart 收尾只 flush 已完成的行；没到空行的半帧丢弃，别把截断 JSON 当事件发出去。
        val transport = FakeSseTransport(
            bodyChunks = listOf("event: delta\ndata: {\"text\":\"完整\"}\n\n", "event: delta\ndata: {\"te"),
        )
        val events = AiSseApi(transport).streamCompletion("hi").toList()
        assertEquals(listOf(AiStreamEvent.Delta("完整")), events)
    }

    @Test
    fun keepalive_comments_and_unknown_events_are_ignored() = runBlocking {
        val transport = FakeSseTransport(
            bodyChunks = listOf(
                ": keep-alive\n\n",
                "event: ping\ndata: {\"type\":\"ping\"}\n\n",
                "event: delta\ndata: {\"text\":\"走\"}\n\n",
                "event: delta\ndata: not-json\n\n",
            ),
        )
        val events = AiSseApi(transport).streamCompletion("hi").toList()
        assertEquals(listOf(AiStreamEvent.Delta("走")), events)
    }
}
