package com.example.my_kmp_project.core.design

import androidx.compose.ui.graphics.Color

/**
 * Compose adapters over [DesignTokens]. Prefer [DesignSystem] / these aliases in CMP
 * and Mine Compose Island. Native shells should read [DesignTokens] directly (or
 * platform bindings generated from the same values).
 */
internal object DemoColors {
    private fun argb(token: DesignTokens.Argb): Color = Color(token.value)

    val Primary = argb(DesignTokens.Color.Link)
    val OnPrimary = argb(DesignTokens.Color.OnPrimary)
    val Accent = argb(DesignTokens.Color.Link)
    val Background = argb(DesignTokens.Color.Canvas)
    val PageBg = argb(DesignTokens.Color.CanvasSoft2)
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
}
