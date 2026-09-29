package com.example.my_kmp_project.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.round

private val ChartInk = Color(0xFF171717)
private val ChartLink = Color(0xFF0070F3)
private val ChartAccent = Color(0xFFF5A623)
private val ChartGrid = Color(0xFFEBEBEB)
private val ChartMute = Color(0xFF888888)

/**
 * Ring ≈ Flutter WysConversionRing via Canvas.
 * Bars ≈ Flutter WysMiniBarChart (fl_chart width:10, spaceAround) via Canvas —
 * Vico multi-series clusters all rods into one x-group and blows MSE.
 */
@Composable
internal actual fun AnalyticsConversionRing(
    rate: Float?,
    modifier: Modifier,
    size: Dp,
) {
    val valid = rate != null && rate.isFinite() && rate >= 0f
    val clamped = if (valid) rate!!.coerceIn(0f, 1f) else 0f
    val centerText = if (valid) "${round(clamped * 100f).toInt()}%" else "—"
    val strokeWidth = 7.dp

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val valueStroke = Stroke(width = strokePx + 2.dp.toPx(), cap = StrokeCap.Butt)
            val trackStroke = Stroke(width = strokePx, cap = StrokeCap.Butt)
            val diam = (this.size.minDimension - valueStroke.width).coerceAtLeast(1f)
            val topLeft = Offset(
                (this.size.width - diam) / 2f,
                (this.size.height - diam) / 2f,
            )
            val arcSize = Size(diam, diam)
            drawArc(
                color = ChartGrid,
                startAngle = -90f + 360f * clamped,
                sweepAngle = 360f * (1f - clamped).coerceAtLeast(0.0001f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = trackStroke,
            )
            if (valid) {
                drawArc(
                    color = ChartInk,
                    startAngle = -90f,
                    sweepAngle = (360f * clamped).coerceAtLeast(1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = valueStroke,
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerText,
                color = ChartInk,
                fontSize = (size.value * 0.18f).sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = (size.value * 0.18f).sp,
            )
            Spacer(modifier = Modifier.height((size.value * 0.02f).dp))
            Text(
                text = "转化率",
                color = ChartMute,
                fontSize = (size.value * 0.11f).sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = (size.value * 0.11f).sp,
            )
        }
    }
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
    val labels = listOf("PV", "UV", "点", "转")
    val values = listOf(pv, uv, clicks, converts)
    val colors = listOf(ChartInk, ChartLink, ChartAccent, ChartLink)
    val maxY = pv.coerceAtLeast(1f)
    Column(modifier = modifier.height(height)) {
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            val n = values.size
            val barW = 10.dp.toPx()
            val slot = size.width / n
            val chartH = size.height
            val radius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            values.forEachIndexed { i, v ->
                val h = (v.coerceAtLeast(0f) / maxY).coerceIn(0f, 1f) * chartH
                val left = slot * i + (slot - barW) / 2f
                val top = chartH - h
                drawRoundRect(
                    color = colors[i],
                    topLeft = Offset(left, top),
                    size = Size(barW, h.coerceAtLeast(1f)),
                    cornerRadius = radius,
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEach { label ->
                Text(
                    text = label,
                    color = ChartMute,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
