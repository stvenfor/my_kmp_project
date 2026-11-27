package com.example.my_kmp_project.feature.friend

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
 * 通讯录：好友关系与 IM 都依赖融云 SDK / 后端关系链，尚未接入。
 * 不保留任何本地 mock 好友、搜索加好友、建群等假交互；路由与返回保留。
 */
@Composable
fun FriendScreen(onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(
            title = "通讯录",
            onBack = onBack,
            containerColor = DemoColors.PageBg,
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            DemoEmptyState(
                title = "好友 / IM 未接入",
                message = "需要好友关系后端与融云 IM SDK 接入后才能添加好友、建群与聊天",
                icon = MineIcons.Person,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
    }
}
