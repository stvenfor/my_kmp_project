package com.example.my_kmp_project.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.ui.ReportMainTabRoot

/**
 * 消息 Tab：IM 依赖融云 SDK，尚未接入。
 * 不保留任何 mock 会话/消息/发送逻辑；接入真实 IM SDK 后再开放。
 *
 * @param onOpenFriends Flutter square_pencil → 通讯录 `/friend`（入口保留）。
 */
@Composable
fun ChatScreen(
    onOpenFriends: () -> Unit = {},
) {
    ReportMainTabRoot(isRoot = true)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "消息",
                color = DemoColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 30.sp,
                letterSpacing = (-1.2).sp,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DemoColors.PrimarySoft)
                    .clickable(onClick = onOpenFriends),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "✎",
                    color = DemoColors.Accent,
                    fontSize = 18.sp,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(DemoColors.PrimarySoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "✎", color = DemoColors.Accent, fontSize = 26.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "IM 消息未接入（需融云 SDK）",
                    color = DemoColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "接入融云 IM SDK 后才能收发会话与消息",
                    color = DemoColors.TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
