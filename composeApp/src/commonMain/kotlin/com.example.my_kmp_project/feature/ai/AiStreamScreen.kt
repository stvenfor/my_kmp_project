package com.example.my_kmp_project.feature.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal object AiRoutes {
    const val Stream = "/ai/stream"

    fun fromLabel(label: String): String? = when (label.trim()) {
        "AI小石头", "AI 小石头", "小石头", "AI" -> Stream
        else -> null
    }
}

/** Flutter AiStreamController.welcomeText / quickPrompts */
private object AiStreamSoT {
    const val WelcomeText =
        "你好，我是 AI 小石头——本 App / 4S 店的业务向导。" +
            "你可以问「二手车入口在哪」「如何登录」或点下方快捷问。"

    val QuickPrompts = listOf(
        "二手车入口在哪里？",
        "怎么登录账号？",
        "数据分析怎么看？",
    )
}

private data class AiBubble(val id: String, val role: String, val text: String)

/**
 * AI 小石头 — Flutter AiStreamPage chrome；真 SSE 见 platform-gap。
 */
@Composable
internal fun AiStreamScreen(onBack: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var streaming by remember { mutableStateOf(false) }
    var stopRequested by remember { mutableStateOf(false) }
    var bubbles by remember {
        mutableStateOf(
            listOf(
                AiBubble("0", "welcome", AiStreamSoT.WelcomeText),
            ),
        )
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showChips = bubbles.size <= 1 && !streaming

    LaunchedEffect(bubbles.size, bubbles.lastOrNull()?.text) {
        if (bubbles.isNotEmpty()) {
            listState.animateScrollToItem(bubbles.lastIndex)
        }
    }

    fun mockReply(q: String): String = when {
        q.contains("二手车") ->
            "二手车入口：首页「二手车」或全部服务 → 二手车（路由 /home/used_car）。需登录后查看车源列表。（mock）"
        q.contains("登录") ->
            "登录：我的 Tab 点头像/登录，或打开 /auth/login；支持密码与验证码（mock，真微信登录见 gap）。"
        q.contains("数据") ->
            "数据分析：首页/全部服务 →「数据分析」（/home/data_analytics），登录后可看门店指标。（mock）"
        else ->
            "关于「$q」：我可以指路到二手车、登录、数据分析等业务入口。更多能力接 SSE 后开放。（mock 流）"
    }

    fun send(prompt: String) {
        val q = prompt.trim()
        if (q.isEmpty() || streaming) return
        bubbles = bubbles + AiBubble("u-${bubbles.size}", "user", q)
        input = ""
        scope.launch {
            streaming = true
            stopRequested = false
            val full = mockReply(q)
            val id = "a-${bubbles.size}"
            bubbles = bubbles + AiBubble(id, "assistant", "")
            val idx = bubbles.lastIndex
            for (i in 1..full.length) {
                if (stopRequested) break
                delay(28)
                bubbles = bubbles.toMutableList().also {
                    it[idx] = AiBubble(id, "assistant", full.take(i))
                }
            }
            streaming = false
        }
    }

    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        MineTopBar(
            title = "AI 小石头",
            onBack = onBack,
            containerColor = Color.White,
            actions = {
                if (streaming) {
                    TextButton(onClick = { stopRequested = true }) {
                        Text("停止", color = Color(0xFFE53935))
                    }
                }
            },
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Spacer(Modifier.height(10.dp)) }
            items(bubbles, key = { it.id }) { b ->
                when (b.role) {
                    "user" -> {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            Text(
                                b.text,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(DemoColors.Primary)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                color = Color.White,
                                fontSize = 15.sp,
                            )
                        }
                    }
                    "welcome" -> {
                        Column(
                            Modifier
                                .fillMaxWidth(0.92f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFE8F0FE))
                                .padding(14.dp),
                        ) {
                            Text(
                                "AI 小石头",
                                fontSize = 12.sp,
                                color = DemoColors.Primary,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                b.text,
                                color = DemoColors.TextPrimary,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                            )
                        }
                    }
                    else -> {
                        Column(
                            Modifier
                                .fillMaxWidth(0.92f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(16.dp))
                                .padding(14.dp),
                        ) {
                            Text(
                                "AI 小石头",
                                fontSize = 12.sp,
                                color = DemoColors.Primary,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                b.text.ifEmpty { "…" },
                                color = DemoColors.TextPrimary,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
        if (showChips) {
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AiStreamSoT.QuickPrompts.forEach { chip ->
                    Text(
                        chip,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(16.dp))
                            .clickable { send(chip) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 13.sp,
                        color = DemoColors.TextSecondary,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val hint = if (streaming) "生成中，请稍候…" else "输入问题…"
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                enabled = !streaming,
                textStyle = TextStyle(fontSize = 15.sp, color = DemoColors.TextPrimary),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFFF5F5F5))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                decorationBox = { inner ->
                    if (input.isEmpty()) Text(hint, color = DemoColors.Muted, fontSize = 15.sp)
                    inner()
                },
            )
            Spacer(Modifier.size(10.dp))
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (streaming || input.isBlank()) DemoColors.Muted else DemoColors.Primary)
                    .clickable(enabled = !streaming) { send(input) },
                contentAlignment = Alignment.Center,
            ) {
                Text("↑", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}
