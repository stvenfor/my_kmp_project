package com.example.my_kmp_project.feature.mine

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.DesignSystem

/**
 * Mine page visual tokens mirroring Flutter `MineTheme` /
 * `VercelTokens.light` (`features/settings/lib/mine/theme/mine_theme.dart`).
 *
 * 语义色全部桥接 core/design，避免与全局主题重复定义或漂移；仅保留 Mine 私有尺寸。
 */
object MineTheme {
    val Accent = DesignSystem.Accent
    val Background = DesignSystem.PageBg
    val Surface = DesignSystem.Surface
    val FillSecondary = DesignSystem.PageBg
    val LabelPrimary = DesignSystem.TextPrimary
    val LabelSecondary = DesignSystem.TextSecondary
    val LabelTertiary = DesignSystem.Muted
    val Separator = DesignSystem.Divider
    val Danger = DesignSystem.Danger
    val LinkBgSoft = DemoColors.LinkBgSoft

    val RadiusMd = 8.dp
    val RadiusLg = 12.dp
    val Hairline = 0.5.dp

    val LargeTitleSize = 32.sp
    val HeadlineSize = 18.sp
    val StatValueSize = 22.sp
    val SectionTitleSize = 17.sp
    val BodySize = 16.sp
    val CaptionSize = 14.sp
}
