package com.example.my_kmp_project.feature.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.ui.ReportMainTabRoot

/**
 * 直播页：音视频房间依赖三方 RTC SDK（未接入）。
 * 无 mock 房间数据；真实直播能力接入 SDK 后再开放。
 */
@Composable
public fun LiveScreen(
    onBack: () -> Unit,
    openRoom: Boolean = false,
) {
    ReportMainTabRoot(isRoot = false)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(
            title = if (openRoom) "直播间" else "直播",
            onBack = onBack,
            containerColor = DemoColors.Toolbar,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "直播能力未接入",
                    color = DemoColors.TextPrimary,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "需要接入三方音视频 RTC SDK 后才能开播/观看",
                    color = DemoColors.TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
