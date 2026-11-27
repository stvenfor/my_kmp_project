package com.example.my_kmp_project.feature.classroom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.DemoEmptyState
import com.example.my_kmp_project.core.design.MineIcons
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.ui.ReportMainTabRoot

/**
 * 课堂：班级 / 作业 / 礼品卡等能力依赖后端与三方课堂 SDK，尚未接入。
 * 无 mock 班级、作业、礼品数据；路由与返回保留。
 */
@Composable
public fun ClassroomScreen(onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(
            title = "我的班级",
            onBack = onBack,
            containerColor = DemoColors.PageBg,
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            DemoEmptyState(
                title = "课堂能力未接入",
                message = "班级、作业统计、礼品卡等能力需后端与课堂 SDK 接入后开放",
                icon = MineIcons.DocText,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
    }
}
