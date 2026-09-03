package com.example.my_kmp_project.core.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors

/**
 * OHOS: Compose does not host ArkWeb yet. Keep contract; fail acceptance honestly.
 * Shell ArkTS WebPage exists under harmonyApp/ but is not wired into this Compose route.
 */
@Composable
internal actual fun PlatformWebView(
    url: String,
    bridge: WebBridgeHost,
    modifier: Modifier,
    navigation: WebViewNavigationHandle?,
) {
    bridge.openUrl(url)
    navigation?.canGoBack = false
    navigation?.goBackImpl = { false }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "OHOS WebView 未接入 Compose",
                color = DemoColors.Danger,
                fontSize = 16.sp,
            )
            Text(
                text = url,
                color = DemoColors.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "能力状态：missing（registry）。请勿将本页视为验收通过。",
                color = DemoColors.Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
