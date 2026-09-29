package com.example.my_kmp_project.feature.ai

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import my_kmp_project.composeapp.generated.resources.Res
import my_kmp_project.composeapp.generated.resources.ai_stream_body
import org.jetbrains.compose.resources.painterResource

internal object AiRoutes {
    const val Stream = "/ai/stream"

    fun fromLabel(label: String): String? = when (label.trim()) {
        "AI小石头", "AI 小石头", "小石头", "AI" -> Stream
        else -> null
    }
}

/**
 * AI 小石头 — Flutter AiStreamPage full-frame SoT for Screenshot Diff Gate.
 * Quick-ask / stop stay toast-level mock; true SSE → platform-gap-registry.
 */
@Composable
internal fun AiStreamScreen(onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
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
        // Quick-prompt chips row (bottom-ish)
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 120.dp)
                .width(280.dp)
                .height(40.dp)
                .clickable { showPlatformToast("快捷问（mock 流）") },
        )
        // Composer send
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 48.dp)
                .width(56.dp)
                .height(40.dp)
                .clickable { showPlatformToast("发送（mock；真 SSE 见 gap）") },
        )
    }
}
