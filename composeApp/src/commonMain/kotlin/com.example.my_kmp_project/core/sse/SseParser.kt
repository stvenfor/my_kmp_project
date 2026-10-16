package com.example.my_kmp_project.core.sse

/**
 * Incremental SSE line-protocol parser: feed decoded text chunks, get back the
 * frames completed inside each chunk.
 *
 * Ported from the Dart source of truth `commons/network/lib/sse/sse_parser.dart`
 * (`add` / `flush` / `_drainCompleteLines` / `_handleLine` / `_emitIfReady`).
 *
 * Deviation from Dart, on purpose: Dart only splits on `\n` (and strips one
 * trailing `\r`), so a bare `\r` terminator is not recognised. Here `\n`, `\r\n`
 * and `\r` all terminate a line, per WHATWG; `\r` at the end of a chunk is held
 * one character so a `\r\n` split across TCP chunks is not read as two lines.
 */
public class SseParser {

    private val carry = StringBuilder()
    private var pendingCr = false

    private var event: String? = null
    private val dataLines = mutableListOf<String>()
    private var id: String? = null

    /**
     * Most recent `retry:` value in milliseconds, or `null`.
     *
     * Like the Dart parser this never creates a frame on its own; reconnection
     * timing belongs to the transport layer, not to [SseFrame].
     */
    public var retryMillis: Long? = null
        private set

    /** Append a decoded text segment; returns the frames completed by it. */
    public fun feed(chunk: String): List<SseFrame> {
        if (chunk.isEmpty()) return emptyList()
        val frames = mutableListOf<SseFrame>()
        for (c in chunk) {
            when {
                // Second half of a `\r\n` that was split across chunks, or that
                // directly follows a `\r` terminator: not a line end of its own.
                c == '\n' && pendingCr -> pendingCr = false

                c == '\r' -> {
                    handleLine(takeCarry(), frames)
                    pendingCr = true
                }

                c == '\n' -> handleLine(takeCarry(), frames)

                else -> {
                    carry.append(c)
                    pendingCr = false
                }
            }
        }
        return frames
    }

    /**
     * Call at end of stream. A trailing partial line is decoded, but a frame
     * that never reached its blank line is discarded — matching Dart, which
     * drops half-written payloads rather than emitting truncated JSON as a
     * complete frame.
     */
    public fun flush(): List<SseFrame> {
        val frames = mutableListOf<SseFrame>()
        pendingCr = false
        if (carry.isNotEmpty()) handleLine(takeCarry(), frames)
        resetFieldState()
        return frames
    }

    private fun takeCarry(): String {
        val line = carry.toString()
        carry.setLength(0)
        return line
    }

    private fun handleLine(line: String, frames: MutableList<SseFrame>) {
        if (line.isEmpty()) {
            emitIfReady()?.let { frames.add(it) }
            return
        }
        if (line[0] == ':') return

        val colon = line.indexOf(':')
        val field = if (colon < 0) line else line.substring(0, colon)
        var value = if (colon < 0) "" else line.substring(colon + 1)
        // The spec strips exactly one space after the colon, no more.
        if (value.startsWith(" ")) value = value.substring(1)

        when (field) {
            "event" -> event = value
            "data" -> dataLines.add(value)
            "id" -> id = value
            "retry" -> if (value.isAsciiDigits()) retryMillis = value.toLongOrNull()
            else -> Unit
        }
    }

    private fun emitIfReady(): SseFrame? {
        if (event == null && dataLines.isEmpty() && id == null) return null
        val frame = SseFrame(
            event = event ?: SseFrame.DEFAULT_EVENT,
            data = dataLines.joinToString("\n"),
            id = id,
        )
        resetFieldState()
        return frame
    }

    private fun resetFieldState() {
        event = null
        dataLines.clear()
        id = null
    }

    private fun String.isAsciiDigits(): Boolean =
        isNotEmpty() && all { it in '0'..'9' }
}
