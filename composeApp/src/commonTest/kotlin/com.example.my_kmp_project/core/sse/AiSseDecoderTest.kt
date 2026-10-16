package com.example.my_kmp_project.core.sse

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AiSseDecoderTest {

    private fun decode(event: String = SseFrame.DEFAULT_EVENT, data: String): AiStreamEvent? =
        SseFrame(event = event, data = data).toAiStreamEventOrNull()

    @Test
    fun metaFrameMapsIdentifiers() {
        val event = decode(
            event = "meta",
            data = "{\"requestId\":\"sse_01\",\"seq\":0,\"type\":\"meta\"," +
                "\"conversationId\":\"conv_01\",\"model\":\"mock-v1\",\"clientRequestId\":\"req_1\"}",
        )
        assertEquals(
            AiStreamEvent.Meta(
                requestId = "sse_01",
                conversationId = "conv_01",
                model = "mock-v1",
                clientRequestId = "req_1",
            ),
            event,
        )
    }

    @Test
    fun deltaUsesEventNameWhenPresent() {
        assertEquals(AiStreamEvent.Delta("本项目"), decode("delta", "{\"type\":\"delta\",\"text\":\"本项目\"}"))
    }

    @Test
    fun defaultMessageEventIsIgnoredLikeDart() {
        // Dart prefers a non-empty `event:` over the payload `type`, and the parser always
        // fills in "message", so a frame without an explicit `event:` decodes to null.
        assertNull(decode(data = "{\"type\":\"delta\",\"text\":\"hi\"}"))
    }

    @Test
    fun deltaFallsBackToPayloadTypeWhenEventValueIsEmpty() {
        assertEquals(AiStreamEvent.Delta("hi"), decode(event = "", data = "{\"type\":\"delta\",\"text\":\"hi\"}"))
    }

    @Test
    fun doneMapsFinishReason() {
        assertEquals(
            AiStreamEvent.Done(finishReason = "stop"),
            decode("done", "{\"requestId\":\"r\",\"seq\":42,\"type\":\"done\",\"text\":\"\",\"finishReason\":\"stop\"}"),
        )
    }

    @Test
    fun errorWithObjectPayloadUsesItsCodeAndMessage() {
        assertEquals(
            AiStreamEvent.Error(code = "upstream_timeout", message = "model upstream timeout"),
            decode(
                "error",
                "{\"requestId\":\"r\",\"seq\":10,\"type\":\"error\"," +
                    "\"error\":{\"code\":\"upstream_timeout\",\"message\":\"model upstream timeout\"}}",
            ),
        )
    }

    @Test
    fun errorWithPlainStringPayloadKeepsDefaultCode() {
        assertEquals(AiStreamEvent.Error(code = "stream_error", message = "boom"), decode("error", "{\"error\":\"boom\"}"))
    }

    @Test
    fun errorWithoutErrorFieldUsesTopLevelMessage() {
        assertEquals(AiStreamEvent.Error(code = "stream_error", message = "nope"), decode("error", "{\"message\":\"nope\"}"))
    }

    @Test
    fun errorWithoutAnyDetailUsesDefaults() {
        assertEquals(
            AiStreamEvent.Error(code = "stream_error", message = "生成失败"),
            decode("error", "{\"type\":\"error\"}"),
        )
    }

    @Test
    fun unparsableDataYieldsNull() {
        assertNull(decode("delta", "not json at all"))
    }

    @Test
    fun nonObjectJsonDataYieldsNull() {
        assertNull(decode(SseFrame.DEFAULT_EVENT, "42"))
    }

    @Test
    fun unknownTypeYieldsNull() {
        assertNull(decode("ping", "{\"type\":\"ping\"}"))
    }

    @Test
    fun emptyDeltaTextYieldsNull() {
        assertNull(decode("delta", "{\"text\":\"\"}"))
    }

    @Test
    fun emptyDataWithDefaultEventYieldsNull() {
        assertNull(decode(data = ""))
    }

    @Test
    fun envelopeShapedDataIsNotTheStreamContract() {
        // The BFF `{code, message, data}` envelope never wraps SSE frames; a body that
        // looks like one has no `type`, so it decodes to null rather than to `data`.
        assertNull(decode(data = "{\"code\":0,\"message\":\"ok\",\"data\":{\"text\":\"hi\"}}"))
    }

    @Test
    fun numericFieldsAreStringifiedLikeDart() {
        assertEquals(AiStreamEvent.Meta(requestId = "123"), decode("meta", "{\"requestId\":123}"))
    }

    @Test
    fun jsonNullFieldReadsAsAbsent() {
        assertEquals(AiStreamEvent.Done(finishReason = null), decode("done", "{\"finishReason\":null}"))
    }

    @Test
    fun multiLineDataIsJoinedIntoValidJsonThatStillDecodes() {
        val frame = SseParser().feed("event: delta\ndata: {\"text\":\ndata: \"hi\"}\n\n").single()
        assertEquals("{\"text\":\n\"hi\"}", frame.data)
        assertEquals(AiStreamEvent.Delta("hi"), frame.toAiStreamEventOrNull())
    }

    @Test
    fun fullStreamDecodesInOrder() {
        val parser = SseParser()
        val events = mutableListOf<AiStreamEvent>()
        parser.feed(": keepalive\n\n").forEach { events += listOfNotNull(it.toAiStreamEventOrNull()) }
        parser.feed(
            "event: meta\n" +
                "data: {\"conversationId\":\"c1\"}\n" +
                "\n" +
                "event: delta\n" +
                "data: {\"text\":\"你好\"}\n" +
                "\n",
        ).forEach { events += listOfNotNull(it.toAiStreamEventOrNull()) }
        val last = parser.feed("event: done\r\ndata: {\"finishReason\":\"stop\"}\r\n\r\n").single()
        events += listOfNotNull(last.toAiStreamEventOrNull())

        assertEquals(
            listOf(
                AiStreamEvent.Meta(conversationId = "c1"),
                AiStreamEvent.Delta("你好"),
                AiStreamEvent.Done("stop"),
            ),
            events,
        )
    }
}
