package com.example.my_kmp_project.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Flutter `wys_chart` stand-ins for analytics list tiles.
 *
 * - Android / iOS: [chartsVicoMain] — Vico multiplatform 2.3 (bars) + Canvas ring
 *   (2.x 无 PieChart；3.x 与本仓 CMP 1.9.2 / AGP 8.6 不兼容)
 * - Harmony: stub actual until CPF ships Vico/KoalaPlot ohosArm64
 */

@Composable
internal expect fun AnalyticsConversionRing(
    rate: Float?,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
)

@Composable
internal expect fun AnalyticsMiniBars(
    pv: Float,
    uv: Float,
    clicks: Float,
    converts: Float,
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
)
