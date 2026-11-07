package com.example.my_kmp_project.feature.ai

import com.example.my_kmp_project.core.network.SseException
import com.example.my_kmp_project.core.sse.AiStreamEvent
import kotlin.test.Test
import kotlin.test.assertEquals

/** 事件→UI 归约的纯逻辑自证；不牵扯 Compose 与真实网络。 */
class AiStreamUiStateTest {

    @Test
    fun deltas_append_in_order_and_done_keeps_answer() {
        var state = AiStreamUiState(phase = AiStreamPhase.Loading)
        state = state.onEvent(AiStreamEvent.Meta(conversationId = "c-1", model = "qwen"))
        assertEquals(AiStreamPhase.Streaming, state.phase)
        assertEquals("c-1", state.conversationId)
        state = state.onEvent(AiStreamEvent.Delta("你"))
        state = state.onEvent(AiStreamEvent.Delta("好"))
        state = state.onEvent(AiStreamEvent.Delta("！"))
        assertEquals("你好！", state.answer)
        state = state.onEvent(AiStreamEvent.Done("stop"))
        assertEquals(AiStreamPhase.Done, state.phase)
        assertEquals("你好！", state.answer)
    }

    @Test
    fun in_stream_error_keeps_answer_and_exposes_message() {
        var state = AiStreamUiState(phase = AiStreamPhase.Streaming, answer = "我")
        state = state.onEvent(
            AiStreamEvent.Error(code = "upstream_timeout", message = "模型上游超时"),
        )
        assertEquals(AiStreamPhase.Error, state.phase)
        assertEquals("模型上游超时", state.errorMessage)
        assertEquals("我", state.answer)
    }

    @Test
    fun blank_meta_does_not_clobber_sticky_conversation_id() {
        var state = AiStreamUiState(conversationId = "c-1")
        state = state.onEvent(AiStreamEvent.Meta(conversationId = ""))
        assertEquals("c-1", state.conversationId)
    }

    @Test
    fun pre_stream_sse_status_maps_to_readable_message() {
        assertEquals(
            "登录已失效，请重新登录",
            mapSseErrorMessage(SseException("raw", statusCode = 401)),
        )
        assertEquals(
            "请求过于频繁，请稍后再试",
            mapSseErrorMessage(SseException("raw", statusCode = 429)),
        )
        assertEquals(
            "服务暂不可用，请稍后再试",
            mapSseErrorMessage(SseException("raw", statusCode = 503)),
        )
        assertEquals(
            "连接失败",
            mapSseErrorMessage(SseException("连接失败")),
        )
    }
}
