package com.example.my_kmp_project.core.sse

/**
 * Domain events for `POST /api/v1/sse/completions`.
 *
 * Mirrors the Dart sealed hierarchy `features/ai/lib/ai/model/ai_stream_event.dart`.
 */
public sealed interface AiStreamEvent {

    /** First frame: request/conversation identifiers issued by the BFF. */
    public data class Meta(
        val requestId: String? = null,
        val conversationId: String? = null,
        val model: String? = null,
        val clientRequestId: String? = null,
    ) : AiStreamEvent

    /** Incremental assistant text to append. */
    public data class Delta(
        val text: String,
    ) : AiStreamEvent

    /** Stream finished normally. */
    public data class Done(
        val finishReason: String? = null,
    ) : AiStreamEvent

    /**
     * In-stream failure (after the response was upgraded to `text/event-stream`).
     * Dart calls this `AiStreamError`; nested here so it never shadows `kotlin.Error`.
     */
    public data class Error(
        val code: String,
        val message: String,
    ) : AiStreamEvent
}
