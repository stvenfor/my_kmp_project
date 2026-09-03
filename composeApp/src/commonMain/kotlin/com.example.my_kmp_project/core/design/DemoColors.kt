package com.example.my_kmp_project.core.design

import androidx.compose.ui.graphics.Color

/**
 * Visual tokens aligned with Flutter `commons/ui` [AppTheme]
 * (`accent` `#007AFF`, `background` `#F2F2F7`, `surface` white, etc.).
 */
internal object DemoColors {
    /** Flutter AppTheme.accent */
    val Primary = Color(0xFF007AFF)
    val OnPrimary = Color.White
    /** Same as primary for selected chrome accents. */
    val Accent = Color(0xFF007AFF)
    /** Flutter AppTheme.surface — cards / tab bar base */
    val Background = Color(0xFFFFFFFF)
    /** Flutter AppTheme.background — page scaffold */
    val PageBg = Color(0xFFF2F2F7)
    val OnBackground = Color(0xFF000000)
    /** Flutter labelSecondary-ish (#3C3C43 @ 60%) approximated for Compose */
    val OnSurface = Color(0x993C3C43)
    /** Flutter separator */
    val Outline = Color(0xFFC6C6C8)
    val Muted = Color(0x993C3C43)
    val Danger = Color(0xFFFF3B30)
    val TextPrimary = Color(0xFF000000)
    val TextSecondary = Color(0x993C3C43)
    val Divider = Color(0xFFC6C6C8)
    /** Flutter app bar uses surface white */
    val Toolbar = Color(0xFFFFFFFF)
    /** Flutter tabBarBackground #F2FFFFFF */
    val TabBarBackground = Color(0xF2FFFFFF)
}
