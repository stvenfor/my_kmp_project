package com.example.my_kmp_project.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.TextContent
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * `KtorApiClient.openSseTextStream` 真落到 HTTP 层的证据（契约表 #96，
 * Dart `commons/network/lib/sse/sse_client.dart:34-48`）：
 * `Accept: text/event-stream` + JSON body 上线、响应体**增量**吐分片、
 * 非 event-stream 的应答走 `SseException` 而不是 Ktor 的 `ResponseException`。
 *
 * `HttpStatement.execute()`（不带 block）会把整包读进内存，所以 mock engine 用
 * `respond(ByteReadChannel)` 才能验证「没有 bodyAsText()」这条路。
 */
class KtorSseTransportTest {

    private val sseHeaders = headersOf(HttpHeaders.ContentType, "text/event-stream; charset=utf-8")
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun streamingClient(channel: ByteChannel, status: HttpStatusCode = HttpStatusCode.OK) =
        HttpClient(MockEngine) {
            engine {
                addHandler { respond(channel, status, sseHeaders) }
            }
        }

    @Test
    fun open_sse_stream_posts_json_body_with_event_stream_accept() = runBlocking {
        val captured = mutableListOf<HttpRequestData>()
        val channel = ByteChannel(autoFlush = true)
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    captured += request
                    respond(channel, HttpStatusCode.OK, sseHeaders)
                }
            }
        }
        channel.close() // 空流：仍然要走到「升级成功 ⇒ 0 分片」这条出口

        val api = KtorApiClient(client)
        val body = """{"prompt":"你好，小石头","conversationId":"c-1","clientRequestId":"r-9"}"""
        assertEquals(emptyList(), api.openSseTextStream(AI_PATH, body).toList())

        assertEquals(1, captured.size)
        val request = captured.single()
        assertEquals("POST", request.method.value)
        assertEquals(AI_PATH, request.url.encodedPath)
        // Dart sse_client.dart:43 —— Accept 覆盖成 event-stream。
        assertEquals("text/event-stream", request.headers[HttpHeaders.Accept])
        // body 仍是 JSON（Dart 由 dio 的 json encoder 发出）。Ktor 的 DefaultTransform 会把
        // Content-Type 从头上挪到 body 上，所以这里断言 TextContent.contentType。
        val content = request.body as? TextContent
        assertTrue(content?.contentType?.match(ContentType.Application.Json) == true, "${content?.contentType}")
        assertEquals(body, String(request.body.toByteArray(), Charsets.UTF_8))
    }

    @Test
    fun chunks_are_emitted_incrementally_while_the_server_is_still_writing() = runBlocking {
        val body = ByteChannel()
        val api = KtorApiClient(streamingClient(body))

        val frameOne = "event: meta\ndata: {\"requestId\":\"r1\"}\n\n"
        val frameTwo = "event: delta\ndata: {\"text\":\"你好\"}\n\n"
        val sawFirst = CompletableDeferred<Unit>()
        val allowClose = CompletableDeferred<Unit>()
        var secondFrameWritten = false

        val feeder = launch {
            body.writeFully(frameOne.encodeToByteArray())
            body.flush()
            // 收集端先拿到第一片，第二片才上线 —— 这才叫增量，而不是整包读完再切。
            sawFirst.await()
            secondFrameWritten = true
            body.writeFully(frameTwo.encodeToByteArray())
            body.flush()
            allowClose.await()
            body.close()
        }

        val received = mutableListOf<String>()
        api.openSseTextStream(AI_PATH, """{"prompt":"hi"}""").collect { chunk ->
            received += chunk
            if (received.size == 1) {
                assertTrue(!secondFrameWritten, "chunk 2 must not be on the wire before chunk 1 is emitted")
                sawFirst.complete(Unit)
            } else {
                allowClose.complete(Unit)
            }
        }
        feeder.join()

        assertEquals(listOf(frameOne, frameTwo), received)
    }

    @Test
    fun multibyte_characters_split_across_chunks_are_not_mangled() = runBlocking {
        // Dart utf8.decoder 是**有状态**的：分片切在「你」的第 1 个字节上也不该出乱码。
        val bytes = "你好".encodeToByteArray()
        val channel = ByteChannel(autoFlush = true)
        val api = KtorApiClient(streamingClient(channel))

        val received = mutableListOf<String>()
        val collector = launch {
            api.openSseTextStream(AI_PATH, """{"prompt":"hi"}""").collect { received += it }
        }
        channel.writeFully(bytes.copyOfRange(0, 1))
        channel.flush()
        channel.writeFully(bytes.copyOfRange(1, bytes.size))
        channel.flush()
        channel.close()
        collector.join()

        assertEquals("你好", received.joinToString(""))
        assertTrue(received.none { it.contains('\uFFFD') }, "no replacement chars: $received")
    }

    @Test
    fun pre_stream_json_answer_is_a_typed_sse_error_not_a_ktor_response_exception() = runBlocking {
        // Dart validateStatus < 600：401 不抛 HTTP 异常；判据是 content-type ⇒ 前流信封错误。
        val client = HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond("""{"error":"unauthorized"}""", HttpStatusCode.Unauthorized, jsonHeaders)
                }
            }
        }
        val error = assertFailsWith<SseException> {
            KtorApiClient(client).openSseTextStream(AI_PATH, """{"prompt":"hi"}""").toList()
        }
        assertEquals(401, error.statusCode)
        assertEquals("unauthorized", error.message)
        assertEquals("unauthorized", error.error)
        assertEquals("""{"error":"unauthorized"}""", error.rawBody)
    }

    @Test
    fun envelope_answer_on_200_is_not_fed_to_the_parser() = runBlocking {
        // 服务端忘了改 Content-Type：返回 `{code,message}` 信封而不是流 ⇒ 报错，不解析帧。
        val client = HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond("""{"code":10023,"message":"token 失效"}""", HttpStatusCode.OK, jsonHeaders)
                }
            }
        }
        val error = assertFailsWith<SseException> {
            KtorApiClient(client).openSseTextStream(AI_PATH, """{"prompt":"hi"}""").toList()
        }
        assertEquals("token 失效", error.message)
        // Dart _defaultErrorCode 没有 200 分支 ⇒ http_200（sse_exception.dart:49-66）。
        assertEquals("http_200", error.error)
    }

    private companion object {
        const val AI_PATH: String = "/api/v1/sse/completions"
    }
}
