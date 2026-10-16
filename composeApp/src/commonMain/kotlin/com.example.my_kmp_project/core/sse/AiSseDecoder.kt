package com.example.my_kmp_project.core.sse

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Reused across frames; unknown keys never break a payload. */
private val AiSseJson: Json = Json {
    ignoreUnknownKeys = true
}

private const val DEFAULT_ERROR_CODE = "stream_error"
private const val DEFAULT_ERROR_MESSAGE = "生成失败"

/**
 * Typed decode of one `/api/v1/sse/completions` frame into the AI completion model.
 *
 * Ported from Dart `features/ai/lib/ai/api/ai_sse_api.dart` (`_mapFrame`, lines 38-87).
 * The `data:` payload is a flat JSON object (`{"type":"delta","text":"…"}`), NOT the
 * BFF `{code, message, data}` envelope — Dart `sse_client.dart:77-82` only throws the
 * envelope-shaped [com.example.my_kmp_project.core.network.SseException] **before** the
 * upgrade; once the response is `text/event-stream`, errors travel as `event: error` and the
 * server must not switch back to a JSON envelope.
 *
 * Returns `null` for frames the UI has no use for: unparsable or non-object `data`,
 * unknown `type`, empty delta text.
 *
 * Quirk kept from Dart: `event:` wins over the payload's `type`, and the parser always
 * fills in `"message"` when the client omitted `event:` — so a frame with no explicit
 * `event:` line is dropped even if its JSON carries `type`. The BFF always writes
 * `event: <name>`, so this never bites in practice.
 */
public fun SseFrame.toAiStreamEventOrNull(): AiStreamEvent? {
    val payload = dataAsJsonObjectOrNull()

    // Dart prefers a non-empty `event:` over the in-payload `type`; an `event:` line
    // with an empty value stays empty and still falls through to `type`.
    val type = if (event.isNotEmpty()) event else payload?.optString("type") ?: SseFrame.DEFAULT_EVENT

    return when (type) {
        "meta" -> AiStreamEvent.Meta(
            requestId = payload?.optString("requestId"),
            conversationId = payload?.optString("conversationId"),
            model = payload?.optString("model"),
            clientRequestId = payload?.optString("clientRequestId"),
        )

        "delta" -> {
            val text = payload?.optString("text") ?: ""
            if (text.isEmpty()) null else AiStreamEvent.Delta(text)
        }

        "done" -> AiStreamEvent.Done(finishReason = payload?.optString("finishReason"))

        "error" -> decodeError(payload)

        else -> null
    }
}

private fun decodeError(payload: JsonObject?): AiStreamEvent.Error {
    val err = payload?.get("error")
    if (err is JsonObject) {
        return AiStreamEvent.Error(
            code = err.optString("code") ?: DEFAULT_ERROR_CODE,
            message = err.optString("message") ?: DEFAULT_ERROR_MESSAGE,
        )
    }
    if (err != null && err !is JsonNull) {
        return AiStreamEvent.Error(code = DEFAULT_ERROR_CODE, message = err.asStringOrContent())
    }
    return AiStreamEvent.Error(
        code = DEFAULT_ERROR_CODE,
        message = payload?.optString("message") ?: DEFAULT_ERROR_MESSAGE,
    )
}

private fun SseFrame.dataAsJsonObjectOrNull(): JsonObject? {
    if (data.isEmpty()) return null
    val element = runCatching { AiSseJson.parseToJsonElement(data) }.getOrNull()
    return element as? JsonObject
}

private fun JsonObject.optString(key: String): String? = get(key)?.asStringOrNull()

/** Absent key and JSON `null` both read as `null`, matching Dart's `json?['k']?.toString()`. */
private fun JsonElement.asStringOrNull(): String? = if (this is JsonNull) null else asStringOrContent()

private fun JsonElement.asStringOrContent(): String =
    if (this is JsonPrimitive) contentOrNull ?: toString() else toString()
