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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.network.DemoApiHosts
import com.example.my_kmp_project.core.network.NetEnvironment
import com.example.my_kmp_project.core.network.NetworkConfig

/**
 * Product settings matching Flutter `module_settings` SettingsPage.
 *
 * Out of scope (Flutter debug-only; see platform-gap-registry):
 * BLE 连接示例、新车成交/invoice demo、弹框调度、链接与推送调试、
 * Realtime/WebSocket 调试、融云 IM 调试、DoKit / bfui.
 */
@Composable
internal fun MineSettingsScreen(
    onBack: () -> Unit,
    onOpenPersonalized: () -> Unit,
    onOpenMembership: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    var darkMode by remember { mutableStateOf(false) }
    var localeZh by remember { mutableStateOf(true) }
    var envPickerOpen by remember { mutableStateOf(false) }
    var envLabel by remember {
        mutableStateOf(DemoApiHosts.labelForBaseUrl(NetworkConfig.effectiveBaseUrl()))
    }
    var hostUrl by remember { mutableStateOf(NetworkConfig.effectiveBaseUrl()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(title = "设置", onBack = onBack, containerColor = DemoColors.PageBg)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            MineGroupedCard {
                MineNavRow(
                    title = "运行环境",
                    subtitle = "$envLabel · $hostUrl",
                    onClick = { envPickerOpen = !envPickerOpen },
                )
                if (envPickerOpen) {
                    MineInsetDivider()
                    DemoApiHosts.debugSwitchHosts.forEach { (label, url) ->
                        val selected = hostUrl.trimEnd('/') == url.trimEnd('/')
                        MineNavRow(
                            title = label,
                            subtitle = url,
                            trailingText = if (selected) "✓" else null,
                            showChevron = false,
                            onClick = {
                                NetworkConfig.switchDebugHost(label, url)
                                envLabel = label
                                hostUrl = NetworkConfig.effectiveBaseUrl()
                                envPickerOpen = false
                            },
                        )
                        MineInsetDivider()
                    }
                    MineNavRow(
                        title = "恢复默认 (TEST)",
                        showChevron = false,
                        onClick = {
                            NetworkConfig.applyEnvironment(NetEnvironment.Test)
                            envLabel = DemoApiHosts.labelForBaseUrl(NetworkConfig.effectiveBaseUrl())
                            hostUrl = NetworkConfig.effectiveBaseUrl()
                            envPickerOpen = false
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            MineGroupedCard {
                MineSwitchRow(
                    title = "深色模式",
                    checked = darkMode,
                    onCheckedChange = { darkMode = it },
                )
                MineInsetDivider()
                MineNavRow(
                    title = "语言",
                    subtitle = if (localeZh) "简体中文" else "English",
                    onClick = { localeZh = !localeZh },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            MineGroupedCard {
                MineNavRow(title = "会员", onClick = onOpenMembership)
                MineInsetDivider()
                MineNavRow(title = "个性化设置", onClick = onOpenPersonalized)
                MineInsetDivider()
                MineNavRow(title = "关于", onClick = onOpenAbout)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "调试入口（BLE / invoice / DoKit 等）不在本页交付范围内。",
                color = DemoColors.Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
internal fun MineAboutScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(title = "关于", onBack = onBack, containerColor = DemoColors.PageBg)
        Column(modifier = Modifier.padding(16.dp)) {
            MineGroupedCard {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(
                        text = "My AI · KMP 三端壳（Android / iOS / HarmonyOS）。",
                        color = DemoColors.TextPrimary,
                        fontSize = 15.sp,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "默认 host：${DemoApiHosts.TEST}",
                        color = DemoColors.TextSecondary,
                        fontSize = 13.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "当前：${NetworkConfig.effectiveBaseUrl()}",
                        color = DemoColors.Muted,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}
