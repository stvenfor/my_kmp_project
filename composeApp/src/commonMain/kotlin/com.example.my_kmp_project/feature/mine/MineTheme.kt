package com.example.my_kmp_project.feature.mine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DesignSystem

/**
 * Mine page visual tokens mirroring Flutter `MineTheme` /
 * `VercelTokens.light` (`features/settings/lib/mine/theme/mine_theme.dart`).
 *
 * Accent / page background bridge [DesignSystem].
 */
internal object MineTheme {
    val Accent = DesignSystem.Accent
    val Background = DesignSystem.PageBg
    val Surface = Color(0xFFFFFFFF)
    val FillSecondary = Color(0xFFF5F5F5)
    val LabelPrimary = Color(0xFF171717)
    val LabelSecondary = Color(0xFF4D4D4D)
    val LabelTertiary = Color(0xFF888888)
    val Separator = Color(0xFFEBEBEB)
    val Danger = Color(0xFFDC2626)
    val LinkBgSoft = Color(0xFFD3E5FF)
    val Shadow = Color(0x0F8E8E93)

    val RadiusMd = 8.dp
    val RadiusLg = 12.dp
    val Hairline = 0.5.dp

    val LargeTitleSize = 32.sp
    val HeadlineSize = 18.sp
    val StatValueSize = 22.sp
    val SectionTitleSize = 16.sp
    val BodySize = 16.sp
    val CaptionSize = 14.sp
}
