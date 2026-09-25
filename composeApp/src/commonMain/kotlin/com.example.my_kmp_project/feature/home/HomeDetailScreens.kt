package com.example.my_kmp_project.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.ReportMainTabRoot

internal data class AfterSalesDetailRow(
    val title: String,
    val kindLabel: String,
    val customerName: String,
    val customerPhone: String,
    val plateNo: String,
    val mileageKm: Int?,
    val serviceDate: String,
    val content: String,
    val appointmentId: Int?,
)

/** Flutter `AfterSalesDetailPage` — title 服务详情 + hero + 客户车辆 + 服务信息. */
@Composable
internal fun AfterSalesServiceDetailScreen(
    row: AfterSalesDetailRow,
    onBack: () -> Unit,
) {
    ReportMainTabRoot(isRoot = false)
    val kindColor = if (row.kindLabel.contains("修")) Color(0xFFE67E22) else Color(0xFF2E7D32)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
    ) {
        MineTopBar(title = "服务详情", onBack = onBack, containerColor = Color.White)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(0.5.dp, Color(0xFFE8E8E8), RoundedCornerShape(16.dp))
                    .padding(18.dp),
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(kindColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(row.kindLabel, color = kindColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
                Text(row.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
            }
            DetailSection(
                title = "客户车辆",
                rows = buildList {
                    add("客户" to row.customerName)
                    add("手机" to row.customerPhone)
                    if (row.plateNo.isNotBlank()) add("车牌" to row.plateNo)
                    row.mileageKm?.let { add("里程" to "$it km") }
                },
            )
            DetailSection(
                title = "服务信息",
                rows = buildList {
                    add("类型" to row.kindLabel)
                    add("日期" to row.serviceDate)
                    row.appointmentId?.let { add("关联预约" to "#$it") }
                },
            )
            if (row.content.isNotBlank()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(16.dp),
                ) {
                    Text("备注", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(row.content, color = DemoColors.TextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
        }
    }
}

/** Flutter `AnalyticsDetailPage` simplified (hero + metric rows, no chart lib). */
@Composable
internal fun AnalyticsDetailScreen(
    record: AnalyticsRecordRow,
    onBack: () -> Unit,
) {
    ReportMainTabRoot(isRoot = false)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6F8)),
    ) {
        MineTopBar(title = "数据详情", onBack = onBack, containerColor = Color.White)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (record.anomaly) {
                CueBanner(
                    color = Color(0xFFC62828),
                    title = "异常记录",
                    subtitle = "该观测被标记为异常，请优先核对流量与转化。",
                )
            } else if (record.featured) {
                CueBanner(
                    color = Color(0xFF1565C0),
                    title = "精选记录",
                    subtitle = "该观测被标记为精选，适合作为对照样例。",
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .padding(16.dp),
            ) {
                Text(record.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(record.subtitle, color = DemoColors.TextSecondary, fontSize = 13.sp)
            }
            DetailSection(
                title = "流量漏斗",
                rows = listOf(
                    "PV" to "${record.pv}",
                    "点击" to "${record.clicks}",
                    "转化" to "${record.converts}",
                ),
            )
            // Simple bar visualization
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(16.dp),
            ) {
                Text("漏斗占比", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(12.dp))
                val max = record.pv.coerceAtLeast(1)
                MetricBar("PV", record.pv / max.toFloat(), Color(0xFF3B8CFF))
                Spacer(Modifier.height(8.dp))
                MetricBar("点击", record.clicks / max.toFloat(), Color(0xFF5C6BC0))
                Spacer(Modifier.height(8.dp))
                MetricBar("转化", record.converts / max.toFloat(), Color(0xFF26A69A))
            }
        }
    }
}

/** Flutter `NewCarFollowDetailPage` — 跟进档案详情 + 级别 + 写跟进. */
@Composable
internal fun NewCarFollowArchiveDetailScreen(
    row: NewCarFollowRow,
    onBack: () -> Unit,
) {
    var level by remember { mutableStateOf(row.intentBand) }
    var logBody by remember { mutableStateOf("") }
    val logs = remember {
        listOf(
            "首访介绍车型配置，客户关注智驾",
            "报价已发，待配偶到店试驾",
        )
    }
    ReportMainTabRoot(isRoot = false)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6F8)),
    ) {
        MineTopBar(title = "跟进档案详情", onBack = onBack, containerColor = Color.White)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .padding(16.dp),
            ) {
                Text(row.customerName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("${row.vehicle} · ${row.stage}", color = DemoColors.TextSecondary)
            }
            DetailSection(
                title = "档案信息",
                rows = listOf(
                    "手机" to row.phone,
                    "意向车型" to row.vehicle,
                    "阶段" to row.stage,
                    "意向" to row.intentBand,
                    "下次跟进" to row.nextFollow,
                    "顾问" to row.owner,
                ),
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(16.dp),
            ) {
                Text("调整级别", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("高", "中", "低").forEach { label ->
                        val selected = level == label
                        Text(
                            label,
                            color = if (selected) Color(0xFF3B8CFF) else Color(0xFF1A1A1A),
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (selected) Color(0xFF3B8CFF).copy(alpha = 0.15f) else Color.White,
                                )
                                .border(
                                    1.dp,
                                    if (selected) Color(0xFF3B8CFF) else Color(0xFFE0E0E0),
                                    RoundedCornerShape(16.dp),
                                )
                                .clickable { level = label }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(16.dp),
            ) {
                Text("写一条跟进", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = logBody,
                    onValueChange = { logBody = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("跟进内容") },
                    minLines = 3,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "提交跟进",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF3B8CFF))
                        .clickable {
                            if (logBody.isBlank()) {
                                showPlatformToast("请填写跟进内容")
                            } else {
                                showPlatformToast("已记录跟进")
                                logBody = ""
                            }
                        }
                        .padding(vertical = 12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(16.dp),
            ) {
                Text("跟进记录", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(10.dp))
                logs.forEachIndexed { i, line ->
                    Text("${i + 1}. $line", color = DemoColors.TextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
                    if (i != logs.lastIndex) Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, rows: List<Pair<String, String>>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(16.dp),
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1A1A1A))
        Spacer(Modifier.height(10.dp))
        rows.forEach { (k, v) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(k, color = DemoColors.TextSecondary, fontSize = 14.sp)
                Text(v, color = Color(0xFF1A1A1A), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun CueBanner(color: Color, title: String, subtitle: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .border(0.5.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Text(title, color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = DemoColors.TextSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun MetricBar(label: String, fraction: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(40.dp), fontSize = 12.sp, color = DemoColors.TextSecondary)
        Box(
            Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFFEEEEEE)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .height(10.dp)
                    .background(color),
            )
        }
    }
}
