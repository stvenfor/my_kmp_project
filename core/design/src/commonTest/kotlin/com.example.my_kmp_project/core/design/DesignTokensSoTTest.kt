package com.example.my_kmp_project.core.design

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Locks Shared Design Tokens to Flutter SoT literals (DESIGN.md / VercelTokens.light).
 */
class DesignTokensSoTTest {

    @Test
    fun light_palette_matches_flutter_vercel_tokens() {
        assertEquals(0xFF171717, DesignTokens.Color.Primary.value)
        assertEquals(0xFFFFFFFF, DesignTokens.Color.OnPrimary.value)
        assertEquals(0xFF171717, DesignTokens.Color.Ink.value)
        assertEquals(0xFF4D4D4D, DesignTokens.Color.Body.value)
        assertEquals(0xFF888888, DesignTokens.Color.Mute.value)
        assertEquals(0xFFEBEBEB, DesignTokens.Color.Hairline.value)
        assertEquals(0xFFA1A1A1, DesignTokens.Color.HairlineStrong.value)
        assertEquals(0xFFFFFFFF, DesignTokens.Color.Canvas.value)
        assertEquals(0xFFFAFAFA, DesignTokens.Color.CanvasSoft.value)
        assertEquals(0xFFF5F5F5, DesignTokens.Color.CanvasSoft2.value)
        assertEquals(0xFF0070F3, DesignTokens.Color.Link.value)
        assertEquals(0xFF0761D1, DesignTokens.Color.LinkDeep.value)
        assertEquals(0xFFEE0000, DesignTokens.Color.Error.value)
        assertEquals(0xFFF5A623, DesignTokens.Color.Warning.value)
        assertEquals(0xF2FFFFFF, DesignTokens.Color.TabBarBackground.value)
    }

    @Test
    fun spacing_scale_is_stable() {
        assertEquals(4, DesignTokens.Spacing.Xs)
        assertEquals(8, DesignTokens.Spacing.Sm)
        assertEquals(16, DesignTokens.Spacing.Md)
        assertEquals(24, DesignTokens.Spacing.Lg)
    }
}
