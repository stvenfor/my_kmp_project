package com.example.my_kmp_project.core.design

import androidx.compose.ui.graphics.Color

/**
 * Compose adapters over [DesignTokens]. Prefer [DesignSystem] / these aliases in CMP
 * and Mine Compose Island. Native shells should read [DesignTokens] directly (or
 * platform bindings generated from the same values).
 */
object DemoColors {
    private fun argb(token: DesignTokens.Argb): Color = Color(token.value)

    val Primary = argb(DesignTokens.Color.Link)
    val OnPrimary = argb(DesignTokens.Color.OnPrimary)
    val Accent = argb(DesignTokens.Color.Link)
    val Background = argb(DesignTokens.Color.Canvas)
    val PageBg = argb(DesignTokens.Color.CanvasSoft2)
    /** Flutter `canvasSoft` #FAFAFA — 指标卡底色，比 [PageBg] 再浅一档。 */
    val CanvasSoft = argb(DesignTokens.Color.CanvasSoft)
    val OnBackground = argb(DesignTokens.Color.Ink)
    val OnSurface = argb(DesignTokens.Color.Body)
    val Outline = argb(DesignTokens.Color.HairlineStrong)
    val Muted = argb(DesignTokens.Color.Mute)
    val Danger = argb(DesignTokens.Color.Error)
    val TextPrimary = argb(DesignTokens.Color.Ink)
    val TextSecondary = argb(DesignTokens.Color.Body)
    val Divider = argb(DesignTokens.Color.Hairline)
    val Toolbar = argb(DesignTokens.Color.Canvas)
    val TabBarBackground = argb(DesignTokens.Color.TabBarBackground)

    // —— 表面分层 / 浅底 / 渐变（新增设计基线，优先在卡片与浮层中使用）——
    val SurfaceRaised = argb(DesignTokens.Color.SurfaceRaised)
    val SurfaceFloating = argb(DesignTokens.Color.SurfaceFloating)
    val SurfaceSunken = argb(DesignTokens.Color.SurfaceSunken)
    val PrimarySoft = argb(DesignTokens.Color.PrimarySoft)
    val VioletSoft = argb(DesignTokens.Color.VioletSoft)
    val CyanSoft = argb(DesignTokens.Color.CyanSoft)
    val PinkSoft = argb(DesignTokens.Color.PinkSoft)
    val Scrim = argb(DesignTokens.Color.Scrim)
    val GradientStart = argb(DesignTokens.Color.GradientStart)
    val GradientMid = argb(DesignTokens.Color.GradientMid)
    val GradientEnd = argb(DesignTokens.Color.GradientEnd)

    // —— Flutter VercelTokens 强调色族（首页服务九宫格轮转用）——
    val LinkDeep = argb(DesignTokens.Color.LinkDeep)
    val LinkBgSoft = argb(DesignTokens.Color.LinkBgSoft)
    val Warning = argb(DesignTokens.Color.Warning)
    val Violet = argb(DesignTokens.Color.Violet)
    val Cyan = argb(DesignTokens.Color.Cyan)
    val HighlightPink = argb(DesignTokens.Color.HighlightPink)

    // —— 语义色：Material3 ColorScheme 的容器 / 反相角色（由 token 派生，勿在别处写死）——
    val ErrorSoft = argb(DesignTokens.Color.ErrorSoft)
    val ErrorDeep = argb(DesignTokens.Color.ErrorDeep)
    val SelectionBg = argb(DesignTokens.Color.SelectionBg)
    val SelectionFg = argb(DesignTokens.Color.SelectionFg)
}
