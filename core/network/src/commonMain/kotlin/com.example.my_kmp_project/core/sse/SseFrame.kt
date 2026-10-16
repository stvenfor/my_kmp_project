package com.example.my_kmp_project.core.sse

/**
 * One completed SSE logical frame (the block between blank lines).
 *
 * Field set and defaults mirror the Dart source of truth
 * (`commons/network/lib/sse/sse_frame.dart`): `event` falls back to the WHATWG
 * default [SseFrame.DEFAULT_EVENT], `data` is the `\n`-joined payload, `id` is
 * optional. A `retry:` line is reconnect metadata, not frame content, so it is
 * exposed by [SseParser.retryMillis] instead of a field here.
 */
public data class SseFrame(
    val event: String = DEFAULT_EVENT,
    val data: String = "",
    val id: String? = null,
) {
    public companion object {
        public const val DEFAULT_EVENT: String = "message"
    }
}
