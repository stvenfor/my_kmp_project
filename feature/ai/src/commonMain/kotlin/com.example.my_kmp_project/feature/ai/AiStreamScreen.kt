package com.example.my_kmp_project.feature.ai

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.network.SseException
import com.example.my_kmp_project.core.network.SseTransport
import com.example.my_kmp_project.core.sse.AiStreamEvent
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import my_kmp_project.core.design.generated.resources.Res
import my_kmp_project.core.design.generated.resources.ai_stream_body
import org.jetbrains.compose.resources.painterResource

/** 一次提问在屏幕上的可见状态（对齐 Flutter `AiStreamController` 的 isStreaming 三态）。 */
internal enum class AiStreamPhase {
    Idle,
    Loading,
    Streaming,
    Done,
    Stopped,
    Error,
}

/**
 * 纯 UI 状态：屏幕把 [AiSseApi.streamCompletion] 的 [AiStreamEvent] 用 [onEvent]
 * 折叠进来；stop 与前流错误由屏幕直接写 [phase]。不依赖 Compose/网络，便于单测。
 */
internal data class AiStreamUiState(
    val phase: AiStreamPhase = AiStreamPhase.Idle,
    val answer: String = "",
    val errorMessage: String = "",
    val conversationId: String? = null,
)

/** 把一条 SSE 领域事件折叠进 UI 状态（meta 的 conversationId 非空才覆盖，保持会话粘性）。 */
internal fun AiStreamUiState.onEvent(event: AiStreamEvent): AiStreamUiState = when (event) {
    is AiStreamEvent.Meta -> copy(
        conversationId = event.conversationId?.takeIf { it.isNotEmpty() } ?: conversationId,
        phase = AiStreamPhase.Streaming,
    )
    is AiStreamEvent.Delta -> copy(
        answer = answer + event.text,
        phase = AiStreamPhase.Streaming,
    )
    is AiStreamEvent.Done -> copy(phase = AiStreamPhase.Done)
    is AiStreamEvent.Error -> copy(
        phase = AiStreamPhase.Error,
        errorMessage = event.message,
    )
}

/** 前流/连接层 [SseException] 的用户可读文案（对齐 Dart `_mapSseError`）。 */
internal fun mapSseErrorMessage(error: SseException): String = when (error.statusCode) {
    401, 403 -> "登录已失效，请重新登录"
    429 -> "请求过于频繁，请稍后再试"
    503 -> "服务暂不可用，请稍后再试"
    else -> error.message ?: "网络异常"
}

/**
 * AI 小石头 —— Flutter AiStreamPage full-frame SoT（截图 diff 基线）。
 * 背景图 + 返回 / 快捷问 / 发送三个命中区保持不变；回复区叠加在中部空白处，
 * 由真实 [AiSseApi] 的 `POST /api/v1/sse/completions` 事件流驱动：
 * 增量 token 追加、加载中、错误、停止(stop) 四态都来自流本身，不再弹 toast。
 */
@Composable
public fun AiStreamScreen(
    onBack: () -> Unit,
    transport: SseTransport,
) {
    ReportMainTabRoot(isRoot = false)
    val scope = rememberCoroutineScope()
    val api = remember(transport) { AiSseApi(transport) }
    var state by remember { mutableStateOf(AiStreamUiState()) }
    var streamJob by remember { mutableStateOf<Job?>(null) }
    var requestSeq by remember { mutableStateOf(0) }

    // 客户端来自 AppGraph 注入；token 由网络层按请求动态绑定，无需每次重建。
    fun startStream(prompt: String) {
        if (streamJob?.isActive == true) return
        val stickyConversationId = state.conversationId
        val clientRequestId = "req_${requestSeq++}"
        state = state.copy(phase = AiStreamPhase.Loading, answer = "", errorMessage = "")
        streamJob = scope.launch {
            try {
                api.streamCompletion(
                    prompt = prompt,
                    conversationId = stickyConversationId,
                    clientRequestId = clientRequestId,
                ).collect { event ->
                    state = state.onEvent(event)
                }
            } catch (e: CancellationException) {
                // 用户 stop：phase 已由 stop() 置为 Stopped，协程取消属正常收尾。
                throw e
            } catch (e: SseException) {
                state = state.copy(phase = AiStreamPhase.Error, errorMessage = mapSseErrorMessage(e))
            } catch (e: Exception) {
                state = state.copy(phase = AiStreamPhase.Error, errorMessage = e.message ?: "网络异常")
            }
        }
    }

    fun stop() {
        if (state.phase != AiStreamPhase.Loading && state.phase != AiStreamPhase.Streaming) return
        streamJob?.cancel()
        streamJob = null
        state = state.copy(phase = AiStreamPhase.Stopped)
    }

    val generating = state.phase == AiStreamPhase.Loading || state.phase == AiStreamPhase.Streaming

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Image(
            painter = painterResource(Res.drawable.ai_stream_body),
            contentDescription = "AI 小石头：欢迎语、快捷问、输入框",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        Text(
            "‹",
            fontSize = 28.sp,
            color = Color.Transparent,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 4.dp, top = 4.dp)
                .clickable(onClick = onBack)
                .padding(12.dp),
        )

        // 中部空白区叠加真实 SSE 回复：增量 token + 加载 / 错误 / 停止状态（Idle 时保持基线画面）。
        if (state.phase != AiStreamPhase.Idle) {
            val (label, labelColor) = when (state.phase) {
                AiStreamPhase.Loading, AiStreamPhase.Streaming -> "生成中…" to DemoColors.Muted
                AiStreamPhase.Done -> "已完成" to DemoColors.Muted
                AiStreamPhase.Stopped -> "已停止生成" to DemoColors.Muted
                AiStreamPhase.Error -> state.errorMessage to DemoColors.Danger
                AiStreamPhase.Idle -> "" to DemoColors.Muted
            }
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .fillMaxHeight(0.62f)
                    .padding(horizontal = 20.dp)
                    .padding(top = 160.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (state.answer.isNotEmpty()) {
                    Text(
                        text = state.answer,
                        color = DemoColors.TextPrimary,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
                if (label.isNotEmpty()) {
                    Text(text = label, color = labelColor, fontSize = 13.sp)
                }
            }
        }

        // 快捷问命中区 → 真实 SSE（Flutter quickPrompts 第一条；生成中由 startStream 守卫忽略）。
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 120.dp)
                .width(280.dp)
                .height(40.dp)
                .clickable { startStream(QUICK_PROMPT) },
        )
        // 发送命中区：空闲时发起真实 SSE，生成中变为停止(stop)。
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 48.dp)
                .width(56.dp)
                .height(40.dp)
                .clickable { if (generating) stop() else startStream(COMPOSER_PROMPT) },
        )
    }
}

/** 图片 SoT 没有真实输入框，发送/快捷问用固定 prompt 打真实 SSE；文本输入能力登记为 gap。 */
private const val QUICK_PROMPT: String = "二手车入口在哪里？"
private const val COMPOSER_PROMPT: String = "请介绍一下这个 App 的主要功能"
