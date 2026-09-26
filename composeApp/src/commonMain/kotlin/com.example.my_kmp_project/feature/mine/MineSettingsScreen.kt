package com.example.my_kmp_project.feature.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.my_kmp_project.core.platform.showPlatformToast

/**
 * Product settings matching Flutter `module_settings` SettingsPage.
 *
 * Flutter product surface: 通用（环境 / 深色 / 语言）+ 示例（蓝牙）.
 * Debug-only tiles (dialog / linking / realtime / im) stay out of scope — see
 * platform-gap-registry. BLE demo itself is n/a-out-of-scope; row toasts.
 */
@Composable
internal fun MineSettingsScreen(
    onBack: () -> Unit,
) {
    var darkMode by remember { mutableStateOf(false) }
    var localeZh by remember { mutableStateOf(true) }
    var envPickerOpen by remember { mutableStateOf(false) }
    var langPickerOpen by remember { mutableStateOf(false) }
    var envLabel by remember {
        mutableStateOf(DemoApiHosts.labelForBaseUrl(NetworkConfig.effectiveBaseUrl()))
    }
    var hostUrl by remember { mutableStateOf(NetworkConfig.effectiveBaseUrl()) }

    if (langPickerOpen) {
        AlertDialog(
            onDismissRequest = { langPickerOpen = false },
            title = { Text("选择语言") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            localeZh = true
                            langPickerOpen = false
                            showPlatformToast("已切换为简体中文")
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = if (localeZh) "简体中文  ✓" else "简体中文",
                            color = DemoColors.TextPrimary,
                        )
                    }
                    TextButton(
                        onClick = {
                            localeZh = false
                            langPickerOpen = false
                            showPlatformToast("Switched to English")
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = if (!localeZh) "English  ✓" else "English",
                            color = DemoColors.TextPrimary,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { langPickerOpen = false }) {
                    Text("取消")
                }
            },
        )
    }

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
                .padding(top = 12.dp, bottom = 32.dp),
        ) {
            Text(
                text = "通用",
                color = DemoColors.Muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
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
                MineInsetDivider()
                MineSwitchRow(
                    title = "深色模式",
                    subtitle = "切换浅色 / 深色主题",
                    checked = darkMode,
                    onCheckedChange = {
                        darkMode = it
                        showPlatformToast(if (it) "深色模式（本地预览）" else "浅色模式（本地预览）")
                    },
                )
                MineInsetDivider()
                MineNavRow(
                    title = "语言",
                    trailingText = if (localeZh) "简体中文" else "English",
                    onClick = { langPickerOpen = true },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "示例",
                color = DemoColors.Muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
            MineGroupedCard {
                MineNavRow(
                    title = "蓝牙连接示例",
                    subtitle = "BLE 扫描、连接、服务发现",
                    onClick = {
                        // Flutter RoutePath.bluetoothDemo — KMP: platform-gap n/a-out-of-scope
                        showPlatformToast("蓝牙示例暂未接入")
                    },
                )
            }
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
