package com.example.my_kmp_project.feature.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.ReportMainTabRoot

/** Flutter DealInvoiceTab labels. */
private enum class DealInvoiceTab(val label: String) {
    All("全部发票"),
    Pending("待审核"),
    Approved("已通过"),
    Rejected("未通过"),
}

private enum class DealInvoiceStatus(val label: String, val color: Color) {
    PendingReview("待审核", Color(0xFFFAAD14)),
    Approved("已通过", Color(0xFF52C41A)),
    Rejected("未通过", Color(0xFFE53935)),
}

private data class DealInvoiceItem(
    val id: String,
    val phone: String,
    val status: DealInvoiceStatus,
    val submittedAt: String,
    val rejectReason: String? = null,
)

private data class DealInvoiceSummary(
    val displayName: String,
    val positionLabel: String,
    val storeName: String,
    val uploaded: Int,
    val pendingReview: Int,
    val approved: Int,
    val rejected: Int,
)

private object DealInvoiceMock {
    val summary = DealInvoiceSummary(
        displayName = "qa_user",
        positionLabel = "销售顾问",
        storeName = "朝阳旗舰店",
        uploaded = 6,
        pendingReview = 2,
        approved = 3,
        rejected = 1,
    )
    val items = listOf(
        DealInvoiceItem("1", "138****5172", DealInvoiceStatus.PendingReview, "2026-09-26 14:22"),
        DealInvoiceItem("2", "139****8821", DealInvoiceStatus.Approved, "2026-09-24 09:10"),
        DealInvoiceItem("3", "186****3301", DealInvoiceStatus.Rejected, "2026-09-22 18:40", "发票模糊，请重新上传"),
        DealInvoiceItem("4", "150****6619", DealInvoiceStatus.Approved, "2026-09-20 11:05"),
        DealInvoiceItem("5", "177****9023", DealInvoiceStatus.PendingReview, "2026-09-19 16:33"),
        DealInvoiceItem("6", "135****4410", DealInvoiceStatus.Approved, "2026-09-18 08:55"),
    )
}

/**
 * Flutter [DealInvoiceDemoPage]：顶栏摘要 + Tab 筛选 + 列表 + 上传 FAB。
 */
@Composable
internal fun DealInvoiceDemoScreen(
    onBack: () -> Unit,
    onUpload: () -> Unit,
    onOpenDetail: (String) -> Unit = {},
) {
    ReportMainTabRoot(isRoot = false)
    var tab by remember { mutableStateOf(DealInvoiceTab.All) }
    val summary = DealInvoiceMock.summary
    val filtered = remember(tab) {
        when (tab) {
            DealInvoiceTab.All -> DealInvoiceMock.items
            DealInvoiceTab.Pending -> DealInvoiceMock.items.filter { it.status == DealInvoiceStatus.PendingReview }
            DealInvoiceTab.Approved -> DealInvoiceMock.items.filter { it.status == DealInvoiceStatus.Approved }
            DealInvoiceTab.Rejected -> DealInvoiceMock.items.filter { it.status == DealInvoiceStatus.Rejected }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6F8)),
    ) {
        Column(Modifier.fillMaxSize()) {
            MineTopBar(title = "新车成交", onBack = onBack, containerColor = Color.White)
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 88.dp),
            ) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .width(48.dp)
                                    .height(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF3B8CFF).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    summary.displayName.take(1).uppercase(),
                                    color = Color(0xFF3B8CFF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    summary.displayName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = DemoColors.TextPrimary,
                                )
                                Text(
                                    "${summary.positionLabel} · ${summary.storeName}",
                                    fontSize = 12.sp,
                                    color = DemoColors.TextSecondary,
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth()) {
                            StatCell("已上传", summary.uploaded, Modifier.weight(1f))
                            StatCell("待审核", summary.pendingReview, Modifier.weight(1f))
                            StatCell("已通过", summary.approved, Modifier.weight(1f))
                            StatCell("未通过", summary.rejected, Modifier.weight(1f))
                        }
                    }
                }
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        DealInvoiceTab.entries.forEach { t ->
                            val selected = t == tab
                            Text(
                                t.label,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { tab = t }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                color = if (selected) DemoColors.TextPrimary else DemoColors.TextSecondary,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 15.sp,
                            )
                        }
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color.White),
                    ) {
                        // visual separator under tabs
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .align(Alignment.BottomCenter)
                                .background(DemoColors.Divider),
                        )
                    }
                }
                if (filtered.isEmpty()) {
                    item {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("暂无发票", color = DemoColors.TextSecondary, fontSize = 15.sp)
                            Spacer(Modifier.height(6.dp))
                            Text("上传成交发票后会出现在这里", color = DemoColors.TextSecondary, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(filtered, key = { it.id }) { item ->
                        InvoiceCard(
                            item = item,
                            onTap = { onOpenDetail(item.id) },
                            onReupload = onUpload,
                        )
                    }
                }
            }
        }
        Button(
            onClick = onUpload,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B8CFF)),
        ) {
            Text("上传成交发票", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun StatCell(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DemoColors.TextPrimary)
        Text(label, fontSize = 12.sp, color = DemoColors.TextSecondary)
    }
}

@Composable
private fun InvoiceCard(
    item: DealInvoiceItem,
    onTap: () -> Unit,
    onReupload: () -> Unit,
) {
    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .clickable(onClick = onTap)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFF0F5FF))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text("成交手机", fontSize = 11.sp, color = Color(0xFF3B8CFF))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    item.phone,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = DemoColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(item.status.label, fontSize = 13.sp, color = item.status.color, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(10.dp))
        Text("提交时间: ${item.submittedAt}", fontSize = 12.sp, color = DemoColors.TextSecondary)
        if (item.status == DealInvoiceStatus.Rejected && item.rejectReason != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                "未通过原因: ${item.rejectReason}",
                fontSize = 13.sp,
                color = Color(0xFFE53935),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onReupload,
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("重新上传", color = Color(0xFF3B8CFF), fontSize = 13.sp)
            }
        }
    }
}

/**
 * Flutter [DealInvoiceUploadPage] 精简版：选客户 + 图片区 stub + 提交。
 */
@Composable
internal fun DealInvoiceUploadScreen(onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    var phone by remember { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6F8)),
    ) {
        MineTopBar(title = "上传成交发票", onBack = onBack, containerColor = Color.White)
        Column(Modifier.padding(16.dp)) {
            Text("成交客户手机", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = DemoColors.TextPrimary)
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
                    .clickable {
                        phone = "138****5172"
                        showPlatformToast("已选择示例客户")
                    }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    if (phone.isEmpty()) "点击选择客户" else phone,
                    color = if (phone.isEmpty()) DemoColors.TextSecondary else DemoColors.TextPrimary,
                    fontSize = 15.sp,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("发票照片", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = DemoColors.TextPrimary)
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(10.dp))
                    .clickable { showPlatformToast("选图能力：平台相册/相机接入后启用") },
                contentAlignment = Alignment.Center,
            ) {
                Text("点击添加发票图片", color = DemoColors.TextSecondary, fontSize = 14.sp)
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    showPlatformToast(if (phone.isEmpty()) "请先选择客户" else "已提交审核")
                    if (phone.isNotEmpty()) onBack()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B8CFF)),
            ) {
                Text("提交审核", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
}
