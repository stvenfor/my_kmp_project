package com.example.my_kmp_project.feature.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

private data class AiBubble(val id: String, val role: String, val text: String)

/**
 * AI 小石头 — Flutter-aligned chrome；真 SSE 见 platform-gap。
 */
@Composable
internal fun AiStreamScreen(onBack: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var streaming by remember { mutableStateOf(false) }
    var stopRequested by remember { mutableStateOf(false) }
    var bubbles by remember {
        mutableStateOf(
            listOf(
                AiBubble("0", "assistant", "你好，我是小石头。有什么想问的？"),
            ),
        )
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val chips = listOf("今日学习建议", "语法纠错", "口语话题")

    LaunchedEffect(bubbles.size) {
        if (bubbles.isNotEmpty()) {
            listState.animateScrollToItem(bubbles.lastIndex)
        }
    }

    fun send(prompt: String) {
        val q = prompt.trim()
        if (q.isEmpty() || streaming) return
        bubbles = bubbles + AiBubble("u-${bubbles.size}", "user", q)
        input = ""
        scope.launch {
            streaming = true
            stopRequested = false
            val full = "关于「$q」：建议每天跟读 15 分钟，并记录生词。（mock 流）"
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
                        Text("停止", color = DemoColors.Primary)
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
                val mine = b.role == "user"
                if (mine) {
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
                } else {
                    Column(
                        Modifier
                            .fillMaxWidth(0.92f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                    ) {
                        Text("AI 小石头", fontSize = 12.sp, color = DemoColors.Primary, fontWeight = FontWeight.Medium)
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
            item { Spacer(Modifier.height(8.dp)) }
        }
        Row(
            Modifier.padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            chips.forEach { chip ->
                Text(
                    chip,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(16.dp))
                        .clickable(enabled = !streaming) { send(chip) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    fontSize = 13.sp,
                    color = DemoColors.TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                    if (input.isEmpty()) Text("输入问题…", color = DemoColors.Muted, fontSize = 15.sp)
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
