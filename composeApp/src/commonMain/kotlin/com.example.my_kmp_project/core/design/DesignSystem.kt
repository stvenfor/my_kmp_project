package com.example.my_kmp_project.core.design

/**
 * Progressive design-token alias surface for Mine / Membership / shared chrome.
 *
 * Prefer [DesignSystem] (or [DemoColors] directly) in new code.
 * [com.example.my_kmp_project.feature.mine.MineTheme] may bridge selected fields here
 * without a full token migration (task 4.4 — lightweight alias only).
 */
internal object DesignSystem {
    val Accent get() = DemoColors.Accent
    val Primary get() = DemoColors.Primary
    val OnPrimary get() = DemoColors.OnPrimary
    val PageBg get() = DemoColors.PageBg
    val Surface get() = DemoColors.Background
    val TextPrimary get() = DemoColors.TextPrimary
    val TextSecondary get() = DemoColors.TextSecondary
    val Danger get() = DemoColors.Danger
    val Divider get() = DemoColors.Divider
    val Muted get() = DemoColors.Muted
    val Toolbar get() = DemoColors.Toolbar
}
