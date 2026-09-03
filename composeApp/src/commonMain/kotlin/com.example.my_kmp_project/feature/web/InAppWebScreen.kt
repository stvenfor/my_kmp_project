package com.example.my_kmp_project.feature.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.platform.DefaultWebBridgeHost
import com.example.my_kmp_project.core.platform.PlatformBackHandler
import com.example.my_kmp_project.core.platform.PlatformWebView
import com.example.my_kmp_project.core.platform.WebBridgeHost
import com.example.my_kmp_project.core.platform.WebViewNavigationHandle
import com.example.my_kmp_project.feature.shell.ReportMainTabRoot

@Composable
internal fun InAppWebScreen(
    url: String,
    onBack: () -> Unit,
    bridge: WebBridgeHost = remember(onBack) {
        DefaultWebBridgeHost(onClose = onBack)
    },
) {
    ReportMainTabRoot(isRoot = false)
    val navigation = remember { WebViewNavigationHandle() }

    PlatformBackHandler {
        if (!navigation.goBack()) {
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(
            title = "网页",
            onBack = {
                if (!navigation.goBack()) {
                    onBack()
                }
            },
            containerColor = DemoColors.PageBg,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            PlatformWebView(
                url = url,
                bridge = bridge,
                modifier = Modifier.fillMaxSize(),
                navigation = navigation,
            )
        }
    }
}
