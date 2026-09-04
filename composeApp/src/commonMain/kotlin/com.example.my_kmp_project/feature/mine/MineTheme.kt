package com.example.my_kmp_project.feature.mine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DesignSystem

/**
 * Mine page visual tokens mirroring Flutter `MineTheme`
 * (`features/settings/lib/mine/theme/mine_theme.dart`).
 *
 * Accent / page background bridge [DesignSystem] so Mine/Membership can migrate
 * toward core design aliases without a wholesale token move (Spike II 4.4).
 */
internal object MineTheme {
    val Accent = DesignSystem.Accent
    val Background = DesignSystem.PageBg
    val Surface = Color(0xFFFFFFFF)
    val FillSecondary = Color(0xFFE9E9EB)
    val LabelPrimary = Color(0xFF000000)
    val LabelSecondary = Color(0x993C3C43)
    val LabelTertiary = Color(0x4D3C3C43)
    val Separator = Color(0xFFC6C6C8)
    val Danger = Color(0xFFFF3B30)
    val Shadow = Color(0x0F8E8E93)

    val RadiusMd = 12.dp
    val RadiusLg = 14.dp

    val LargeTitleSize = 32.sp
    val HeadlineSize = 20.sp
    val StatValueSize = 22.sp
    val SectionTitleSize = 17.sp
    val BodySize = 15.sp
    val CaptionSize = 13.sp
}
