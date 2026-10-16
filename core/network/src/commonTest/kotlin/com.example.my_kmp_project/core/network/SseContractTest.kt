package com.example.my_kmp_project.core.network

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * SSE 传输契约（契约表 #96；Dart `commons/network/lib/sse/sse_client.dart` +
 * `sse_exception.dart`，my_ai_project d7876a1）的**纯逻辑面**：
 * 前流判定（status / content-type）、`SseException.fromPreStream` 的键优先级，
 * 以及 UTF-8 增量解码在多字节字符被切断时的行为。
 *
 * 真发请求的那一段（Ktor `bodyAsChannel`）在 `KtorSseTransportTest`（androidUnitTest）。
 */
class SseContractTest {

    // ── 前流判定：SseOpenGate（Dart sse_client.dart:63-81） ─────────────────────

    @Test
    fun gate_releases_chunks_only_for_200_event_stream(): Unit = runBlocking {
        val gate = SseOpenGate(
            statusCode = 200,
            contentType = "text/event-stream; charset=utf-8",
            chunks = flowOf("event: meta\n\n", "event: delta\n\n"),
        )
        assertTrue(gate.isEventStream)
        assertEquals(
            listOf("event: meta\n\n", "event: delta\n\n"),
            gate.eventStreamText().toList(),
        )
    }

    @Test
    fun gate_treats_http_status_alone_as_a_decision_input_not_an_http_error(): Unit = runBlocking {
        // Dart validateStatus 200..599：状态码不抛 HTTP 异常；但非 200 的 event-stream 仍是前流失败。
        val gate = SseOpenGate(
            statusCode = 500,
            contentType = "text/event-stream",
            chunks = flowOf("""{"error":"upstream_timeout"}"""),
        )
        assertTrue(gate.isEventStream)
        val error = assertFailsWith<SseException> { gate.eventStreamText() }
        assertEquals(500, error.statusCode)
        assertEquals("upstream_timeout", error.message)
        assertEquals("upstream_timeout", error.error)
        assertEquals("""{"error":"upstream_timeout"}""", error.rawBody)
    }

    @Test
    fun gate_reads_json_envelope_when_content_type_is_not_event_stream(): Unit = runBlocking {
        // 200 但 content-type 是 JSON ⇒ 依旧不是流，按信封报错（绝不喂 SseParser）。
        val gate = SseOpenGate(
            statusCode = 200,
            contentType = "application/json",
            chunks = flowOf("""{"code":401,"message":"登录失效"}"""),
        )
        assertFalse(gate.isEventStream)
        val error = assertFailsWith<SseException> { gate.eventStreamText() }
        assertEquals("登录失效", error.message)
        // Dart _defaultErrorCode 无 200 分支 ⇒ http_200（sse_exception.dart:49-66）。
        assertEquals("http_200", error.error)
    }

    @Test
    fun gate_collects_split_chunks_before_decoding_pre_stream_body(): Unit = runBlocking {
        val gate = SseOpenGate(
            statusCode = 401,
            contentType = null,
            chunks = flowOf("""{"error":""", "\"unauthorized\"}"),
        )
        val error = assertFailsWith<SseException> { gate.eventStreamText() }
        assertEquals("unauthorized", error.message)
        assertEquals("unauthorized", error.error)
        assertEquals(401, error.statusCode)
    }

    // ── fromPreStream 键优先级（Dart sse_exception.dart:12-40） ─────────────────

    @Test
    fun from_pre_stream_prefers_error_then_message_then_raw_text() {
        val withError = SseException.fromPreStream(401, """{"error":"unauthorized","message":"ignored"}""")
        assertEquals("unauthorized", withError.message)
        assertEquals("unauthorized", withError.error)

        val withMessage = SseException.fromPreStream(401, """{"code":10023,"message":"token 失效"}""")
        assertEquals("token 失效", withMessage.message)
        assertEquals("unauthorized", withMessage.error)

        val plain = SseException.fromPreStream(500, "gateway blew up")
        assertEquals("gateway blew up", plain.message)
        assertEquals("http_500", plain.error)

        val blank = SseException.fromPreStream(503, "   ")
        assertEquals("SSE request failed (503)", blank.message)
        assertEquals("unavailable", blank.error)
        assertEquals("   ", blank.rawBody)
    }

    @Test
    fun from_pre_stream_maps_dart_default_error_codes() {
        assertEquals("bad_request", SseException.defaultErrorCode(400))
        assertEquals("unauthorized", SseException.defaultErrorCode(401))
        assertEquals("forbidden", SseException.defaultErrorCode(403))
        assertEquals("not_found", SseException.defaultErrorCode(404))
        assertEquals("rate_limited", SseException.defaultErrorCode(429))
        assertEquals("unavailable", SseException.defaultErrorCode(503))
        assertEquals("http_418", SseException.defaultErrorCode(418))
    }

    @Test
    fun unsupported_transport_error_is_typed_and_carries_no_status() {
        val error = SseStreamingUnsupportedException("ohosX64/net_http", COMPLETIONS_PATH)
        assertNull(error.statusCode)
        assertEquals("streaming_unsupported", error.error)
        assertTrue(error.message.orEmpty().contains("ohosX64/net_http"))
        assertTrue(error.message.orEmpty().contains(COMPLETIONS_PATH))
    }

    @Test
    fun api_client_default_streaming_is_typed_unsupported_not_silent(): Unit = runBlocking {
        // 没 override 流式的 client（测试替身 / 老实现）必须显式失败，不能返回空流装作成功。
        val api: ApiClient = EnvelopeOnlyApiClient()
        val error = assertFailsWith<SseStreamingUnsupportedException> {
            api.openSseTextStream(COMPLETIONS_PATH, """{"prompt":"hi"}""").toList()
        }
        assertEquals("streaming_unsupported", error.error)
    }

    @Test
    fun refreshing_decorator_forwards_streaming_without_replay(): Unit = runBlocking {
        // AuthRefreshingApiClient 对流式只转发（不刷新、不重放）——见 TokenRefresh.kt 的 KDoc。
        val transport = RecordingTransport()
        val decorated = AuthRefreshingApiClient(
            delegate = transport,
            policy = { _, _, _ -> AuthRefreshDecision.Refresh },
            refresher = { error("流式路径不该触发刷新") },
        )
        val chunks = decorated.openSseTextStream(COMPLETIONS_PATH, """{"prompt":"hi"}""").toList()
        assertEquals(listOf("event: done\ndata: {}\n\n"), chunks)
        assertEquals(1, transport.openCount)
    }

    // ── UTF-8 增量解码（Dart utf8.decoder.bind，sse_client.dart:93） ────────────

    @Test
    fun utf8_decoder_reassembles_sequences_split_across_chunks() {
        // 1 字节 'a' + 3 字节中文 + 4 字节 emoji：逐字节喂，任何切分点都不该产出乱码。
        val source = "中a文😀"
        val bytes = source.encodeToByteArray()
        val decoder = Utf8ChunkDecoder()
        val rebuilt = StringBuilder()
        for (byte in bytes) {
            rebuilt.append(decoder.decode(byteArrayOf(byte), 0, 1))
        }
        rebuilt.append(decoder.finish())
        assertEquals(source, rebuilt.toString())
    }

    @Test
    fun utf8_decoder_holds_back_only_the_partial_tail() {
        val chunk = "你好".encodeToByteArray() // 6 字节 = 2 x 3 字节序列
        val first = chunk.copyOfRange(0, 4) // 完整「你」+「好」的第 1 个字节
        val second = chunk.copyOfRange(4, chunk.size)
        val decoder = Utf8ChunkDecoder()
        assertEquals("你", decoder.decode(first, 0, first.size))
        assertEquals("好", decoder.decode(second, 0, second.size))
        assertEquals("", decoder.finish())
    }

    @Test
    fun utf8_decoder_is_lenient_like_dart_allow_malformed() {
        val decoder = Utf8ChunkDecoder()
        val orphan = byteArrayOf(0x80.toByte(), 'a'.code.toByte())
        assertEquals("\uFFFDa", decoder.decode(orphan, 0, orphan.size))
        // 流结束时残留半个序列 ⇒ 按乱码收尾，不抛（Dart _readResponseBody allowMalformed :111）。
        val truncated = byteArrayOf(0xE4.toByte(), 0xB8.toByte())
        assertEquals("", decoder.decode(truncated, 0, truncated.size))
        assertEquals("\uFFFD\uFFFD", decoder.finish())
    }

    // ── fixtures ────────────────────────────────────────────────────────────────

    private open class EnvelopeOnlyApiClient : ApiClient {
        override suspend fun getRaw(path: String, query: Map<String, String>): String = error("unused")
        override suspend fun getAbsoluteRaw(url: String): String = error("unused")
        override suspend fun postRaw(path: String, body: String): String = error("unused")
        override suspend fun postForm(path: String, fields: Map<String, String>): HttpTextResponse = error("unused")
        override suspend fun putRaw(path: String, body: String): String = error("unused")
        override suspend fun patchRaw(path: String, body: String): String = error("unused")
        override suspend fun deleteRaw(path: String): String = error("unused")

        override suspend fun <T> getApi(
            path: String,
            query: Map<String, String>,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> = error("unused")

        override suspend fun <T> postApi(
            path: String,
            body: String,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> = error("unused")

        override suspend fun <T> putApi(
            path: String,
            body: String,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> = error("unused")

        override suspend fun <T> patchApi(
            path: String,
            body: String,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> = error("unused")

        override suspend fun <T> deleteApi(
            path: String,
            query: Map<String, String>,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> = error("unused")
    }

    /** 只转发流式的装饰器替身：证明 [AuthRefreshingApiClient] 那条路不重放、不刷新。 */
    private class RecordingTransport : EnvelopeOnlyApiClient() {
        var openCount: Int = 0

        override fun openSseTextStream(
            path: String,
            body: String,
            headers: Map<String, String>,
        ) = flowOf("event: done\ndata: {}\n\n").also { openCount++ }
    }

    private companion object {
        /** 契约表 §2.17 行 #96（Dart `ai_sse_api.dart:10`）。 */
        const val COMPLETIONS_PATH: String = "/api/v1/sse/completions"
    }
}
