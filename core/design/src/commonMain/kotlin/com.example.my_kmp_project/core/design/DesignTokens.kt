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
    }

    object Spacing {
        const val Xs = 4
        const val Sm = 8
        const val Md = 16
        const val Lg = 24
        const val Xl = 32
    }

    object Typography {
        const val DisplayMdSp = 24
        const val BodyMdSp = 16
        const val BodySmSp = 14
        const val CaptionSp = 12
        const val TabLabelSp = 10
    }

    object Radius {
        const val Sm = 8
        const val Md = 12
        const val Lg = 16
    }
}
