package com.example.my_kmp_project.feature.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar

private val EyeProtectionOptions = listOf("关闭", "开启", "跟随系统")

@Composable
internal fun MinePersonalizedSettingsScreen(
    onBack: () -> Unit,
    snackbar: (String) -> Unit,
) {
    var eyeProtection by remember { mutableStateOf("关闭") }
    var teachingMode by remember { mutableStateOf(false) }
    var contentRecommendation by remember { mutableStateOf(true) }
    var adRecommendation by remember { mutableStateOf(true) }
    var oralScoring by remember { mutableStateOf(true) }
    var cellularVideoReminder by remember { mutableStateOf(false) }
    var uploadStatusMonitor by remember { mutableStateOf(false) }
    var eyePickerOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(title = "个性化设置", onBack = onBack, containerColor = DemoColors.PageBg)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 24.dp),
        ) {
            MineGroupedCard {
                MineNavRow(
                    title = "装扮中心",
                    onClick = { snackbar("装扮中心（开发中）") },
                )
            }

            MineSectionHeader("模式选择")
            MineGroupedCard {
                MineNavRow(
                    title = "护眼模式",
                    trailingText = eyeProtection,
                    onClick = { eyePickerOpen = !eyePickerOpen },
                )
                if (eyePickerOpen) {
                    MineInsetDivider()
                    EyeProtectionOptions.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    eyeProtection = option
                                    eyePickerOpen = false
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = option,
                                color = DemoColors.TextPrimary,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f),
                            )
                            if (option == eyeProtection) {
                                Text(
                                    text = "✓",
                                    color = DemoColors.Accent,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
                MineInsetDivider()
                MineSwitchRow(
                    title = "教学模式",
                    showHelp = true,
                    onHelp = { snackbar("教学模式：功能说明开发中") },
                    checked = teachingMode,
                    onCheckedChange = { teachingMode = it },
                )
            }

            MineSectionHeader("个性化设置")
            MineGroupedCard {
                MineSwitchRow(
                    title = "个性化内容推荐",
                    showHelp = true,
                    onHelp = { snackbar("个性化内容推荐：功能说明开发中") },
                    checked = contentRecommendation,
                    onCheckedChange = { contentRecommendation = it },
                )
                MineInsetDivider()
                MineSwitchRow(
                    title = "个性化广告推荐",
                    showHelp = true,
                    onHelp = { snackbar("个性化广告推荐：功能说明开发中") },
                    checked = adRecommendation,
                    onCheckedChange = { adRecommendation = it },
                )
                MineInsetDivider()
                MineSwitchRow(
                    title = "口语评分",
                    checked = oralScoring,
                    onCheckedChange = { oralScoring = it },
                )
                MineInsetDivider()
                MineSwitchRow(
                    title = "2/3/4/5G 流量播放视频时提醒我",
                    checked = cellularVideoReminder,
                    onCheckedChange = { cellularVideoReminder = it },
                )
                MineInsetDivider()
                MineSwitchRow(
                    title = "作品上传状态监控",
                    checked = uploadStatusMonitor,
                    onCheckedChange = { uploadStatusMonitor = it },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "偏好为会话内状态；持久化可在后续任务接入平台 KV。",
                color = DemoColors.Muted,
                fontSize = 12.sp,
            )
        }
    }
}
