package com.example.my_kmp_project.feature.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.example.my_kmp_project.core.platform.loadString
import com.example.my_kmp_project.core.platform.saveString

private val EyeProtectionOptions = listOf("关闭", "开启", "跟随系统")

/** Flutter `PersonalizedSettingsController` SpUtils key prefix. */
private const val PrefsPrefix = "personalized_settings."

private fun loadBool(key: String, default: Boolean): Boolean =
    when (loadString("$PrefsPrefix$key")) {
        "true" -> true
        "false" -> false
        else -> default
    }

private fun persistBool(key: String, value: Boolean) {
    saveString("$PrefsPrefix$key", if (value) "true" else "false")
}

@Composable
internal fun MinePersonalizedSettingsScreen(
    onBack: () -> Unit,
    snackbar: (String) -> Unit,
) {
    var eyeProtection by remember {
        mutableStateOf(loadString("${PrefsPrefix}eye_protection_mode") ?: "关闭")
    }
    var teachingMode by remember { mutableStateOf(loadBool("teaching_mode", false)) }
    var contentRecommendation by remember {
        mutableStateOf(loadBool("content_recommendation", true))
    }
    var adRecommendation by remember { mutableStateOf(loadBool("ad_recommendation", true)) }
    var oralScoring by remember { mutableStateOf(loadBool("oral_scoring", true)) }
    var cellularVideoReminder by remember {
        mutableStateOf(loadBool("cellular_video_reminder", false))
    }
    var uploadStatusMonitor by remember {
        mutableStateOf(loadBool("upload_status_monitor", false))
    }
    var eyePickerOpen by remember { mutableStateOf(false) }

    fun help(title: String) = snackbar("$title：功能说明开发中")

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
                    showHelp = true,
                    onHelp = { help("护眼模式") },
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
                                    saveString("${PrefsPrefix}eye_protection_mode", option)
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
                    onHelp = { help("教学模式") },
                    checked = teachingMode,
                    onCheckedChange = {
                        teachingMode = it
                        persistBool("teaching_mode", it)
                    },
                )
            }

            MineSectionHeader("个性化设置")
            MineGroupedCard {
                MineSwitchRow(
                    title = "个性化内容推荐",
                    showHelp = true,
                    onHelp = { help("个性化内容推荐") },
                    checked = contentRecommendation,
                    onCheckedChange = {
                        contentRecommendation = it
                        persistBool("content_recommendation", it)
                    },
                )
                MineInsetDivider()
                MineSwitchRow(
                    title = "个性化广告推荐",
                    showHelp = true,
                    onHelp = { help("个性化广告推荐") },
                    checked = adRecommendation,
                    onCheckedChange = {
                        adRecommendation = it
                        persistBool("ad_recommendation", it)
                    },
                )
                MineInsetDivider()
                MineSwitchRow(
                    title = "口语评分",
                    checked = oralScoring,
                    onCheckedChange = {
                        oralScoring = it
                        persistBool("oral_scoring", it)
                    },
                )
                MineInsetDivider()
                MineSwitchRow(
                    title = "2/3/4/5G 流量播放视频时提醒我",
                    checked = cellularVideoReminder,
                    onCheckedChange = {
                        cellularVideoReminder = it
                        persistBool("cellular_video_reminder", it)
                    },
                )
                MineInsetDivider()
                MineSwitchRow(
                    title = "作品上传状态监控",
                    checked = uploadStatusMonitor,
                    onCheckedChange = {
                        uploadStatusMonitor = it
                        persistBool("upload_status_monitor", it)
                    },
                )
            }
        }
    }
}
