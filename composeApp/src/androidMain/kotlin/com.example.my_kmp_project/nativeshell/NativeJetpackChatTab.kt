package com.example.my_kmp_project.nativeshell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.component.chat.ChatDetailDeepLinkArgs
import com.example.my_kmp_project.core.design.DemoColors

/**
 * Android 原生 Jetpack Chat Tab：IM 依赖融云 SDK，尚未接入。
 * 不保留任何 mock 会话/消息/发送逻辑；deeplink 参数仅消费掉，避免状态残留。
 */
@Composable
internal fun JetpackChatRoot(
    onOpenContacts: () -> Unit = {},
    pendingDetail: ChatDetailDeepLinkArgs? = null,
    showMissingDetailParams: Boolean = false,
    onPendingDetailConsumed: () -> Unit = {},
    onMissingDetailConsumed: () -> Unit = {},
    onDetailVisibilityChanged: (Boolean) -> Unit = {},
) {
    LaunchedEffect(Unit) {
        onDetailVisibilityChanged(false)
    }
    LaunchedEffect(pendingDetail, showMissingDetailParams) {
        if (pendingDetail != null) onPendingDetailConsumed()
        if (showMissingDetailParams) onMissingDetailConsumed()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "消息",
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                color = DemoColors.TextPrimary,
                letterSpacing = (-1.6).sp,
                lineHeight = 36.sp,
                modifier = Modifier.weight(1f),
            )
            // Flutter square_pencil → 通讯录（入口保留）
            Box(
                Modifier
                    .clickable(onClick = onOpenContacts)
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("✎", color = DemoColors.Accent, fontSize = 20.sp)
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "IM 消息未接入（需融云 SDK）",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DemoColors.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "接入融云 IM SDK 后才能收发会话与消息",
                    fontSize = 15.sp,
                    color = DemoColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
