package com.example.my_kmp_project.core.design

/**
 * Shared Design Tokens — single source of truth for all UI stacks
 * (Android Jetpack, SwiftUI, ArkTS, Mine Compose Island).
 *
 * Values match Flutter Visual Source of Truth:
 * `my_ai_project` DESIGN.md / `VercelTokens.light`.
 *
 * Platforms consume [argb] longs (or generated bindings). Do not fork palettes.
 */
object DesignTokens {
    /** ARGB packed color. */
    data class Argb(val value: Long) {
        val alpha: Int get() = ((value shr 24) and 0xFF).toInt()
        val red: Int get() = ((value shr 16) and 0xFF).toInt()
        val green: Int get() = ((value shr 8) and 0xFF).toInt()
        val blue: Int get() = (value and 0xFF).toInt()
    }

    object Color {
        val Primary = Argb(0xFF171717)
        val OnPrimary = Argb(0xFFFFFFFF)
        val Ink = Argb(0xFF171717)
        val Body = Argb(0xFF4D4D4D)
        val Mute = Argb(0xFF888888)
        val Hairline = Argb(0xFFEBEBEB)
        val HairlineStrong = Argb(0xFFA1A1A1)
        val Canvas = Argb(0xFFFFFFFF)
        val CanvasSoft = Argb(0xFFFAFAFA)
        val CanvasSoft2 = Argb(0xFFF5F5F5)
        val Link = Argb(0xFF0070F3)
        val LinkDeep = Argb(0xFF0761D1)
        val LinkBgSoft = Argb(0xFFD3E5FF)
        val Success = Argb(0xFF0070F3)
        val Error = Argb(0xFFEE0000)
        val ErrorSoft = Argb(0xFFF7D4D6)
        val ErrorDeep = Argb(0xFFC50000)
        val Warning = Argb(0xFFF5A623)
        val WarningSoft = Argb(0xFFFFEFCF)
        val WarningDeep = Argb(0xFFAB570A)
        val Violet = Argb(0xFF7928CA)
        val Cyan = Argb(0xFF50E3C2)
        val HighlightPink = Argb(0xFFFF0080)
        val SelectionBg = Argb(0xFF171717)
        val SelectionFg = Argb(0xFFF2F2F2)
        /** Flutter tabBarBackground #F2FFFFFF */
        val TabBarBackground = Argb(0xF2FFFFFF)

        // ── 表面分层（Surface Layering）────────────────────────────────────────
        // 卡片 / 浮层 / 凹陷底三种层级，替代「白卡 + 0.5px 线」的单薄观感。
        /** 抬升表面：卡片、列表组（画布之上第一层）。 */
        val SurfaceRaised = Argb(0xFFFFFFFF)
        /** 浮现表面：弹窗 / 悬浮操作条（画布之上第二层，可叠加阴影）。 */
        val SurfaceFloating = Argb(0xFFFFFFFF)
        /** 凹陷表面：搜索框、分段控件轨道、图片占位。 */
        val SurfaceSunken = Argb(0xFFF1F3F6)

        // ── 品牌浅底（Tint）────────────────────────────────────────────────────
        /** 品牌色 8% 视觉等效的实色浅底，避免半透明叠色在不同背景上漂移。 */
        val PrimarySoft = Argb(0xFFEAF3FF)
        val VioletSoft = Argb(0xFFF4ECFF)
        val CyanSoft = Argb(0xFFE6FAF6)
        val PinkSoft = Argb(0xFFFFEAF4)
        /** 遮罩：底部弹层 / 对话框背后的 scrim。 */
        val Scrim = Argb(0x660A0A0A)

        // ── 品牌渐变（Hero / 主按钮 / 头像环）──────────────────────────────────
        val GradientStart = Argb(0xFF0070F3)
        val GradientEnd = Argb(0xFF7928CA)
        val GradientMid = Argb(0xFF4B5CF5)
    }

    object Spacing {
        const val Xs = 4
        const val Sm = 8
        const val Md = 16
        const val Lg = 24
        const val Xl = 32
        /** 页面左右安全内边距（列表 / 卡片统一贴边距离）。 */
        const val Page = 16
    }

    object Typography {
        const val DisplayLgSp = 30
        const val DisplayMdSp = 24
        const val TitleLgSp = 20
        const val TitleMdSp = 17
        const val BodyMdSp = 16
        const val BodySmSp = 14
        const val LabelMdSp = 13
        const val CaptionSp = 12
        const val MicroSp = 11
        const val TabLabelSp = 10
    }

    object Radius {
        const val Xs = 6
        const val Sm = 8
        const val Md = 12
        const val Lg = 16
        const val Xl = 20
        const val Xxl = 28
        /** 全圆角（药丸 / 头像 / 标签）。 */
        const val Pill = 999
    }

    /** 阴影高度（dp）。卡片默认 [Sm]，浮层用 [Lg]。 */
    object Elevation {
        const val None = 0
        const val Sm = 2
        const val Md = 4
        const val Lg = 8
        const val Xl = 16
    }

    /** 动效时长（ms）。保持「快进慢出」节奏，避免所有过渡一个速度。 */
    object Motion {
        const val Fast = 150
        const val Base = 220
        const val Slow = 320
    }
}
