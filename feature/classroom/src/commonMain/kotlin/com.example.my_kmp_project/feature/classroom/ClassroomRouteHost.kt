package com.example.my_kmp_project.feature.classroom

import androidx.compose.runtime.Composable

/**
 * 课堂路由宿主：所有 `/classroom/...` 路由统一落到「课堂能力未接入」占位。
 * 保留路由分发入口与返回，不保留任何 mock 页面/假作业/假礼品交互。
 */
@Suppress("UNUSED_PARAMETER")
@Composable
public fun ClassroomRouteHost(
    route: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
) {
    ClassroomScreen(onBack = onBack)
}
