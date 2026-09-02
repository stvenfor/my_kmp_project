package com.example.my_kmp_project.feature.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.ImmersiveCenterTopAppBar
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.network.DemoApiHosts
import com.example.my_kmp_project.core.network.NetworkConfig
import com.example.my_kmp_project.feature.shell.ReportMainTabRoot
import com.example.my_kmp_project.getPlatform

@Composable
internal fun MineScreen() {
    var showAbout by remember { mutableStateOf(false) }
    if (showAbout) {
        ReportMainTabRoot(isRoot = false)
        AboutScreen(onBack = { showAbout = false })
    } else {
        ReportMainTabRoot(isRoot = true)
        MineHome(onOpenAbout = { showAbout = true })
    }
}

@Composable
private fun MineHome(onOpenAbout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        ImmersiveCenterTopAppBar(
            title = {
                Text(
                    text = "我的 Demo",
                    fontWeight = FontWeight.Bold,
                    color = DemoColors.TextPrimary,
                    fontSize = 18.sp,
                )
            },
            containerColor = DemoColors.Toolbar,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                text = "访客",
                color = DemoColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "平台：${getPlatform().name}",
                color = DemoColors.TextSecondary,
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = DemoColors.Divider)
            MineRow(title = "关于 Demo", onClick = onOpenAbout)
            HorizontalDivider(color = DemoColors.Divider)
            MineRow(
                title = "当前 API Host",
                subtitle = NetworkConfig.effectiveBaseUrl(),
                onClick = null,
            )
            HorizontalDivider(color = DemoColors.Divider)
        }
    }
}

@Composable
private fun AboutScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(title = "关于", onBack = onBack, containerColor = DemoColors.PageBg)
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "本工程已剥离业务域，仅保留 KMP + Compose 双 Tab 壳。",
                color = DemoColors.TextPrimary,
                fontSize = 15.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Demo host：${DemoApiHosts.TEST}",
                color = DemoColors.TextSecondary,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun MineRow(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 14.dp),
    ) {
        Text(text = title, color = DemoColors.TextPrimary, fontSize = 15.sp)
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, color = DemoColors.Muted, fontSize = 12.sp)
        }
    }
}
