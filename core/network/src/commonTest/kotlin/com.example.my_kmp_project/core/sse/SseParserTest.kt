package com.example.my_kmp_project.core.sse

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SseParserTest {

    private fun parse(vararg chunks: String): List<SseFrame> {
        val parser = SseParser()
        val out = mutableListOf<SseFrame>()
        chunks.forEach { out += parser.feed(it) }
        out += parser.flush()
        return out
    }

    @Test
    fun parsesMetaDeltaDoneFrames() {
        val frames = parse(
            "event: meta\n" +
                "data: {\"type\":\"meta\",\"conversationId\":\"c1\"}\n" +
                "\n" +
                "event: delta\n" +
                "data: {\"type\":\"delta\",\"text\":\"你好\"}\n" +
                "\n" +
                "event: done\n" +
                "data: {\"type\":\"done\",\"finishReason\":\"stop\"}\n" +
                "\n",
        )
        assertEquals(3, frames.size)
        assertEquals("meta", frames[0].event)
        assertTrue(frames[0].data.contains("conversationId"))
        assertEquals("delta", frames[1].event)
        assertTrue(frames[1].data.contains("你好"))
        assertEquals("done", frames[2].event)
        assertNull(frames[2].id)
    }

    @Test
    fun ignoresCommentLinesIncludingKeepalive() {
        val frames = parse(
            ": keepalive\n" +
                "\n" +
                "event: delta\n" +
                "data: {\"text\":\"a\"}\n" +
                "\n" +
                ": keepalive\n" +
                "\n",
        )
        assertEquals(1, frames.size)
        assertEquals("delta", frames[0].event)
        assertEquals("{\"text\":\"a\"}", frames[0].data)
    }

    @Test
    fun commentInsideFrameDoesNotBreakIt() {
        val frames = parse("event: delta\n: ping\ndata: hi\n\n")
        assertEquals(1, frames.size)
        assertEquals(SseFrame(event = "delta", data = "hi"), frames[0])
    }

    @Test
    fun joinsMultiLineDataWithNewline() {
        val frames = parse("event: message\ndata: line1\ndata: line2\n\n")
        assertEquals(1, frames.size)
        assertEquals("line1\nline2", frames[0].data)
    }

    @Test
    fun completesFrameSplitAcrossChunkBoundaries() {
        val frames = parse("event: del", "ta\ndata: {\"t\":\"x\"}\n\n")
        assertEquals(1, frames.size)
        assertEquals("delta", frames[0].event)
        assertEquals("{\"t\":\"x\"}", frames[0].data)
    }

    @Test
    fun blankLineSplitAcrossChunksCompletesFrame() {
        val frames = parse("data: hello\n", "\n")
        assertEquals(1, frames.size)
        assertEquals("hello", frames[0].data)
    }

    @Test
    fun supportsCrlfLineEndings() {
        val frames = parse("event: delta\r\ndata: {\"text\":\"ok\"}\r\n\r\n")
        assertEquals(1, frames.size)
        assertEquals("delta", frames[0].event)
        assertEquals("{\"text\":\"ok\"}", frames[0].data)
    }

    @Test
    fun crlfTerminatorSplitAcrossChunksIsOneLineEnd() {
        val frames = parse("data: a\r", "\ndata: b\r\n\r\n")
        assertEquals(1, frames.size)
        assertEquals("a\nb", frames[0].data)
    }

    @Test
    fun supportsBareCrLineEndings() {
        val frames = parse("event: delta\rdata: hi\r\r")
        assertEquals(1, frames.size)
        assertEquals("delta", frames[0].event)
        assertEquals("hi", frames[0].data)
    }

    @Test
    fun stripsOnlyOneLeadingSpaceAfterColon() {
        val frames = parse(
            "data:  padded\n\n" +
                "data:nospace\n\n" +
                "data:\n\n",
        )
        assertEquals(3, frames.size)
        assertEquals(" padded", frames[0].data)
        assertEquals("nospace", frames[1].data)
        assertEquals("", frames[2].data)
    }

    @Test
    fun fieldWithoutColonUsesEmptyValue() {
        val frames = parse("data\n\n")
        assertEquals(1, frames.size)
        assertEquals("", frames[0].data)
    }

    @Test
    fun emptyEventValueStaysEmptyInsteadOfDefaulting() {
        // Dart keeps `_event = ''`, so `frame.event` is empty rather than "message".
        val frames = parse("event:\ndata: x\n\n")
        assertEquals(1, frames.size)
        assertEquals("", frames[0].event)
    }

    @Test
    fun defaultsEventToMessageAndCapturesId() {
        val frames = parse("id: 42\ndata: hi\n\n")
        assertEquals(1, frames.size)
        assertEquals("42", frames[0].id)
        assertEquals(SseFrame.DEFAULT_EVENT, frames[0].event)
    }

    @Test
    fun idOnlyBlockStillEmitsFrame() {
        val frames = parse("id: 7\n\n")
        assertEquals(1, frames.size)
        assertEquals(SseFrame(event = SseFrame.DEFAULT_EVENT, data = "", id = "7"), frames[0])
    }

    @Test
    fun retryLineRecordsMillisWithoutEmittingFrame() {
        val parser = SseParser()
        assertTrue(parser.feed("retry: 3000\n").isEmpty())
        assertEquals(3000L, parser.retryMillis)
        assertEquals(1, parser.feed("data: x\n\n").size)
        assertEquals(3000L, parser.retryMillis)
    }

    @Test
    fun retryIgnoresNonDigitValue() {
        val parser = SseParser()
        parser.feed("retry: 500\nretry: abc\nretry: -1\n")
        assertEquals(500L, parser.retryMillis)
    }

    @Test
    fun unknownFieldIsIgnoredAndAloneEmitsNothing() {
        assertTrue(parse("foo: bar\n\n").isEmpty())
    }

    @Test
    fun flushDropsFrameWithoutTerminatingBlankLine() {
        val parser = SseParser()
        assertTrue(parser.feed("event: delta\ndata: oops").isEmpty())
        assertTrue(parser.flush().isEmpty())
    }

    @Test
    fun flushResetsStateSoNextFrameParsesCleanly() {
        val parser = SseParser()
        parser.feed("event: delta\ndata: half")
        parser.flush()
        val frames = parser.feed("data: fresh\n\n")
        assertEquals(1, frames.size)
        assertEquals(SseFrame(event = SseFrame.DEFAULT_EVENT, data = "fresh"), frames[0])
    }

    @Test
    fun emptyChunkFeedsNothing() {
        val parser = SseParser()
        assertTrue(parser.feed("").isEmpty())
        assertTrue(parser.flush().isEmpty())
    }

    @Test
    fun multipleFramesInOneChunkKeepOrder() {
        val frames = parse("data: one\n\ndata: two\n\ndata: three\n\n")
        assertEquals(listOf("one", "two", "three"), frames.map { it.data })
    }
}
