package com.example.my_kmp_project.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.demoCard
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import kotlin.coroutines.cancellation.CancellationException

private fun analyticsUvLabel(uv: Int): String =
    if (uv >= 1000) {
        // Flutter: (n / 1000).toStringAsFixed(1)
        val tenths = kotlin.math.round(uv / 100.0) / 10.0
        val s = if (tenths == tenths.toLong().toDouble()) {
            "${tenths.toLong()}.0"
        } else {
            tenths.toString()
        }
        "UV ${s}k"
    } else {
        "UV $uv"
    }

private fun analyticsRoiLabel(roi: Double): String {
    val cents = kotlin.math.round(roi * 100.0) / 100.0
    val whole = cents.toLong()
    val frac = kotlin.math.round((cents - whole) * 100.0).toInt().coerceIn(0, 99)
    return "ROI $whole.${frac.toString().padStart(2, '0')}"
}

@Composable
internal fun AnalyticsListScreen(
    onBack: () -> Unit,
    onItem: (AnalyticsRecordRow) -> Unit,
) {
    // Flutter AnalyticsListPage: summary + ring/bars tiles (Canvas ≈ wys_chart; CPF 无 Vico/KoalaPlot).
    val primary = DemoColors.Accent
    val ink = Color(0xFF171717)
    val accent = Color(0xFFF5A623)
    val bg = Color(0xFFF5F5F5)
    val border = Color(0xFFEBEBEB)
    val mute = Color(0xFF888888)
    val destructive = Color(0xFFEE0000)

    // 真实数据源：Android 走 gRPC :9090；iOS / OHOS 抛「仅 Android 已接入」。
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var pageData by remember { mutableStateOf<AnalyticsPage?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        error = null
        try {
            pageData = AnalyticsGrpcClient().listRecords(page = 1, pageSize = 20)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            error = t.message ?: "加载数据分析失败"
        } finally {
            loading = false
        }
    }

    val items = pageData?.items.orEmpty()
    val total = pageData?.total ?: 0L

    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(bg)) {
        FlutterFeatureTopBar(title = "数据分析", onBack = onBack)
        Row(
            Modifier
                .padding(start = 12.dp, end = 12.dp, top = 12.dp)
                .fillMaxWidth()
                .demoCard()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("〰", color = primary, fontSize = 14.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                "已加载 ${items.size} / 共 $total · 第 ${pageData?.page ?: 1} 页",
                color = mute,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Text(
                "gRPC",
                color = primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(primary.copy(alpha = 0.08f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = primary)
            }

            error != null -> Box(
                Modifier.fillMaxSize().padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("加载失败", color = ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        error.orEmpty(),
                        color = mute,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无数据", color = mute, fontSize = 14.sp)
            }

            else -> LazyColumn(
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp),
            ) {
                items(items) { row ->
                    val cue = when {
                        row.anomaly -> destructive
                        row.featured -> accent
                        else -> Color.Transparent
                    }
                    val rateFrac = if (row.clicks > 0) {
                        (row.converts.toFloat() / row.clicks.toFloat()).coerceIn(0f, 1f)
                    } else {
                        null
                    }
                    val (statusFg, statusBg) = when (row.status) {
                        "active" -> primary to primary.copy(alpha = 0.10f)
                        "paused" -> accent to accent.copy(alpha = 0.14f)
                        else -> mute to Color(0xFFF0F0F0)
                    }
                    Row(
                        Modifier
                            .padding(bottom = 8.dp)
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .demoCard()
                            .clickable { onItem(row) },
                    ) {
                        Box(Modifier.width(4.dp).fillMaxHeight().background(cue))
                        Column(Modifier.padding(14.dp).weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    row.title,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    row.status,
                                    color = statusFg,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(statusBg)
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(row.subtitle, color = mute, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(12.dp))
                            Row(
                                Modifier.fillMaxWidth().height(72.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AnalyticsConversionRing(rate = rateFrac, size = 64.dp)
                                Spacer(Modifier.width(12.dp))
                                AnalyticsMiniBars(
                                    pv = row.pv.toFloat(),
                                    uv = row.uv.toFloat(),
                                    clicks = row.clicks.toFloat(),
                                    converts = row.converts.toFloat(),
                                    modifier = Modifier.weight(1f).height(64.dp),
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Flutter _MetricPill: radius 8, fontSize 12, alpha 0.1
                                Text(
                                    analyticsUvLabel(row.uv),
                                    color = primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(primary.copy(alpha = 0.1f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    analyticsRoiLabel(row.roi),
                                    color = accent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(accent.copy(alpha = 0.1f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                                Spacer(Modifier.weight(1f))
                                Text(row.code, color = mute, fontSize = 11.sp)
                                Spacer(Modifier.width(4.dp))
                                Text("›", color = mute, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
