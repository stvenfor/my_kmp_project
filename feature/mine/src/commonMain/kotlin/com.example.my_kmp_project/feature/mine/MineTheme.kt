package com.example.my_kmp_project.feature.mine

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.DemoElevation
import com.example.my_kmp_project.core.design.DemoRadius
import com.example.my_kmp_project.core.design.DesignSystem

/**
 * Mine page visual tokens mirroring Flutter `MineTheme` /
 * `VercelTokens.light` (`features/settings/lib/mine/theme/mine_theme.dart`).
 *
 * 语义色全部桥接 core/design，避免与全局主题重复定义或漂移；尺寸与阴影跟随
 * [DemoRadius] / [DemoElevation] 设计基线。
 */
object MineTheme {
    val Accent = DesignSystem.Accent
    val Background = DesignSystem.PageBg
    val Surface = DemoColors.SurfaceRaised
    val FillSecondary = DemoColors.SurfaceSunken
    val LabelPrimary = DesignSystem.TextPrimary
    val LabelSecondary = DesignSystem.TextSecondary
    val LabelTertiary = DesignSystem.Muted
    val Separator = DesignSystem.Divider
    val Danger = DesignSystem.Danger
    val LinkBgSoft = DemoColors.PrimarySoft

    val RadiusMd = DemoRadius.Card
    val RadiusLg = DemoRadius.Hero
    val Hairline = 0.5.dp
    val CardElevation = DemoElevation.Card

    val LargeTitleSize = 30.sp
    val HeadlineSize = 18.sp
    val StatValueSize = 22.sp
    val SectionTitleSize = 17.sp
    val BodySize = 16.sp
    val CaptionSize = 14.sp
}
