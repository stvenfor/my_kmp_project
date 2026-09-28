package com.example.my_kmp_project.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Harmony stub: CPF-KMP-CMP 三方库清单尚无 Vico / KoalaPlot。
 * Android/iOS 走 chartsVicoMain；此处先留空占位，保持编译通过。
 */
@Composable
internal actual fun AnalyticsConversionRing(
    rate: Float?,
    modifier: Modifier,
    size: Dp,
) {
    Box(modifier = modifier.size(size))
}

@Composable
internal actual fun AnalyticsMiniBars(
    pv: Float,
    uv: Float,
    clicks: Float,
    converts: Float,
    modifier: Modifier,
    height: Dp,
) {
    Box(modifier = modifier.height(height))
}
