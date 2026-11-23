package com.example.my_kmp_project.core.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 把 [DesignTokens]（唯一色板真源）映射成 Material3 主题角色，解决 App 入口裸
 * `MaterialTheme {}` 使用默认紫色主题、与 [DemoColors] / 设计令牌脱节的问题。
 *
 * - 只做映射，不新增组件、不改视觉资产、不动业务逻辑。
 * - 不注入任何 insets；沉浸式（edge-to-edge）仍由平台入口与
 *   [ImmersiveInsets] / [ImmersiveCenterTopAppBar] 负责。
 * - 其他槽位直接使用 [DemoTheme] 包裹内容，或按需读取
 *   [DemoColorScheme] / [DemoTypography] / [DemoShapes] / [DemoColors] / [DesignSystem]。
 */
val DemoColorScheme: ColorScheme = lightColorScheme(
    // 蓝色操作强调（Flutter SoT Link #0070F3）。
    primary = DemoColors.Primary,
    onPrimary = DemoColors.OnPrimary,
    primaryContainer = DemoColors.LinkBgSoft,
    onPrimaryContainer = DemoColors.LinkDeep,
    inversePrimary = DemoColors.LinkBgSoft,
    // 中性克制：次级角色取墨色 / 浅灰，避免默认紫色溢出。
    secondary = DemoColors.TextPrimary,
    onSecondary = DemoColors.OnPrimary,
    secondaryContainer = DemoColors.PageBg,
    onSecondaryContainer = DemoColors.TextPrimary,
    tertiary = DemoColors.LinkDeep,
    onTertiary = DemoColors.OnPrimary,
    tertiaryContainer = DemoColors.LinkBgSoft,
    onTertiaryContainer = DemoColors.LinkDeep,
    // 画布 / 表面。onSurface 用主文本墨色，让无样式 Text 回落为高对比正文。
    background = DemoColors.Background,
    onBackground = DemoColors.OnBackground,
    surface = DemoColors.Background,
    onSurface = DemoColors.TextPrimary,
    surfaceVariant = DemoColors.CanvasSoft,
    onSurfaceVariant = DemoColors.TextSecondary,
    surfaceTint = DemoColors.Primary,
    inverseSurface = DemoColors.SelectionBg,
    inverseOnSurface = DemoColors.SelectionFg,
    error = DemoColors.Danger,
    onError = DemoColors.OnPrimary,
    errorContainer = DemoColors.ErrorSoft,
    onErrorContainer = DemoColors.ErrorDeep,
    outline = DemoColors.Outline,
    outlineVariant = DemoColors.Divider,
)

/** 圆角来自 [DesignTokens.Radius]：Sm/Md/Lg = 8/12/16dp。 */
val DemoShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(DesignTokens.Spacing.Xs.dp),
    small = RoundedCornerShape(DesignTokens.Radius.Sm.dp),
    medium = RoundedCornerShape(DesignTokens.Radius.Md.dp),
    large = RoundedCornerShape(DesignTokens.Radius.Lg.dp),
)

/** 字体统一走 Geist（Flutter SoT 同款可变字体），字号取 [DesignTokens.Typography]。 */
val DemoTypography: Typography
    // material3 1.4.0 的公开 Typography(...) 构造器以 internal 的
    // ExperimentalMaterial3ExpressiveApi 标注，外部无法 import，只能按标记名抑制。
    @Suppress("ExperimentalMaterial3ExpressiveApi")
    @Composable
    get() {
        val family = DemoFonts.Geist
        fun textStyle(
            sizeSp: Int,
            lineHeightSp: Int,
            weight: FontWeight = FontWeight.Normal,
            letterSpacingSp: Double = 0.0,
        ) = TextStyle(
            fontFamily = family,
            fontSize = sizeSp.sp,
            lineHeight = lineHeightSp.sp,
            fontWeight = weight,
            letterSpacing = letterSpacingSp.sp,
        )
        return Typography(
            displayLarge = textStyle(28, 32, FontWeight.SemiBold, -1.6),
            displayMedium = textStyle(DesignTokens.Typography.DisplayMdSp, 30, FontWeight.SemiBold, -0.75),
            displaySmall = textStyle(22, 28, FontWeight.SemiBold, -0.5),
            headlineLarge = textStyle(20, 26, FontWeight.SemiBold, -0.5),
            headlineMedium = textStyle(18, 24, FontWeight.SemiBold, -0.54),
            headlineSmall = textStyle(16, 22, FontWeight.SemiBold, -0.3),
            titleLarge = textStyle(18, 24, FontWeight.SemiBold, -0.54),
            titleMedium = textStyle(16, 22, FontWeight.SemiBold, -0.3),
            titleSmall = textStyle(DesignTokens.Typography.BodySmSp, 20, FontWeight.SemiBold, -0.2),
            bodyLarge = textStyle(DesignTokens.Typography.BodyMdSp, 24),
            bodyMedium = textStyle(DesignTokens.Typography.BodySmSp, 20, letterSpacingSp = -0.28),
            bodySmall = textStyle(DesignTokens.Typography.CaptionSp, 16, letterSpacingSp = -0.2),
            labelLarge = textStyle(DesignTokens.Typography.BodySmSp, 20, FontWeight.Medium),
            labelMedium = textStyle(DesignTokens.Typography.CaptionSp, 16, FontWeight.Medium),
            labelSmall = textStyle(DesignTokens.Typography.TabLabelSp, 16, FontWeight.Medium),
        )
    }

/** 应用统一主题入口：在 composition 根部包一层，替换裸 `MaterialTheme {}`。 */
@Composable
fun DemoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DemoColorScheme,
        typography = DemoTypography,
        shapes = DemoShapes,
        content = content,
    )
}
