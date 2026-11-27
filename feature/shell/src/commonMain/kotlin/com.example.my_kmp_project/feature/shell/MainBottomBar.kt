package com.example.my_kmp_project.feature.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.DemoFonts
import com.example.my_kmp_project.core.design.ImmersiveInsets.MainBottomBarHeight
import com.example.my_kmp_project.core.router.MainTab
import my_kmp_project.core.design.generated.resources.Res
import my_kmp_project.core.design.generated.resources.main_tab_chat_selected
import my_kmp_project.core.design.generated.resources.main_tab_chat_unselected
import my_kmp_project.core.design.generated.resources.main_tab_community_selected
import my_kmp_project.core.design.generated.resources.main_tab_community_unselected
import my_kmp_project.core.design.generated.resources.main_tab_home_selected
import my_kmp_project.core.design.generated.resources.main_tab_home_unselected
import my_kmp_project.core.design.generated.resources.main_tab_me_selected
import my_kmp_project.core.design.generated.resources.main_tab_me_unselected
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * 底部 TabBar —— Flutter `commons/ui/lib/widgets/ios_tab_bar.dart` 的逐项对齐实现。
 *
 * Flutter 规格（`IosTabBar` + `_TabItem`）：
 * - 容器：`tabBarBackground`（#F2FFFFFF）+ 顶部 0.5px `hairline@0.6` 描边，
 *   内容区高 49，**底部安全区包在背景里**（`SafeArea(top: false)`）；
 * - 每项：`Expanded` 等分，内容垂直居中，图标外包 44×28 圆角 14 药丸
 *   （选中 `link@12%`，AnimatedContainer 200ms easeOut），图标 22，
 *   药丸与文字间隙 2，文字 10sp（选中 w600 / 未选 w400，height 1.1），
 *   颜色选中 `link` / 未选 `mute`。
 *
 * 与旧实现的差异（本次重构修掉）：
 * - 旧版用 M3 [androidx.compose.material3.NavigationBar]，indicator 为 64×32、
 *   内部 padding 与 49dp 固定高冲突、无顶部分隔线、未选色误用 `body`(#4D4D4D)
 *   而非 `mute`(#888888)；
 * - 现改为纯 [Row] 布局，尺寸/间隙/字重与 Flutter 一一对应。
 *
 * 关于毛玻璃：Flutter 侧 `BackdropFilter(blur 20)` 叠在 95% 不透明的
 * `#F2FFFFFF` 上，实际可见的模糊几乎为零。Compose 的 `Modifier.blur` 只能模糊
 * 自身而非「背后的内容」，三端（Android/iOS/OHOS）也没有统一的 backdrop filter
 * 原语，故此处只保留同等不透明度的纯色，视觉结果与 SoT 一致。
 */
@Composable
fun MainBottomBar(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 顶部 0.5px hairline 在 Flutter 里是 BoxDecoration 的 border，画在 49 高的
    // 盒子**内部**、不额外增加高度；这里用 Box 叠加而不是 Column 堆叠，避免栏体
    // 从 49 变成 49.5。
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    ambientColor = Color(0x14171717),
                    spotColor = Color(0x14171717),
                )
                .background(DemoColors.TabBarBackground),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MainBottomBarHeight),
            ) {
                MainTab.entries.forEach { item ->
                    MainTabItem(
                        item = item,
                        selected = selected == item,
                        onClick = { onSelect(item) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            // 底部安全区仍属于栏体背景（Flutter `SafeArea(top: false)`）。
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = DemoColors.Divider.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.TopStart),
        )
    }
}

/** Side navigation for expanded windows; keeps the same destinations and icons as the bottom bar. */
@Composable
fun MainNavigationRail(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(
        modifier = modifier,
        containerColor = DemoColors.TabBarBackground,
    ) {
        MainTab.entries.forEach { item ->
            NavigationRailItem(
                selected = selected == item,
                onClick = { onSelect(item) },
                icon = {
                    Image(
                        painter = painterResource(
                            if (selected == item) item.selectedIcon() else item.unselectedIcon(),
                        ),
                        contentDescription = item.label,
                        modifier = Modifier.size(22.dp),
                        colorFilter = ColorFilter.tint(
                            if (selected == item) DemoColors.Accent else DemoColors.Muted,
                        ),
                    )
                },
                label = { Text(item.label) },
                alwaysShowLabel = true,
            )
        }
    }
}

@Composable
private fun MainTabItem(
    item: MainTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint by animateColorAsState(
        targetValue = if (selected) DemoColors.Accent else DemoColors.Muted,
        animationSpec = tween(durationMillis = 200),
        label = "tabTint",
    )
    val pillColor by animateColorAsState(
        targetValue = if (selected) DemoColors.PrimarySoft else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "tabPill",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "tabIconScale",
    )
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(pillColor),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(
                    if (selected) item.selectedIcon() else item.unselectedIcon(),
                ),
                contentDescription = item.label,
                modifier = Modifier
                    .size(22.dp)
                    .scale(iconScale),
                colorFilter = ColorFilter.tint(tint),
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = item.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                fontFamily = DemoFonts.Geist,
                fontSize = 10.sp,
                lineHeight = 11.sp, // Flutter height: 1.1
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = tint,
            ),
        )
    }
}

private fun MainTab.selectedIcon(): DrawableResource = when (this) {
    MainTab.Home -> Res.drawable.main_tab_home_selected
    MainTab.Chat -> Res.drawable.main_tab_chat_selected
    MainTab.Community -> Res.drawable.main_tab_community_selected
    MainTab.Mine -> Res.drawable.main_tab_me_selected
}

private fun MainTab.unselectedIcon(): DrawableResource = when (this) {
    MainTab.Home -> Res.drawable.main_tab_home_unselected
    MainTab.Chat -> Res.drawable.main_tab_chat_unselected
    MainTab.Community -> Res.drawable.main_tab_community_unselected
    MainTab.Mine -> Res.drawable.main_tab_me_unselected
}
