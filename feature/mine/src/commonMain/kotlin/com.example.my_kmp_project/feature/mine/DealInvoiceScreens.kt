package com.example.my_kmp_project.feature.mine

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.my_kmp_project.core.design.DemoRadius
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.PlatformNetworkImage
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import com.example.my_kmp_project.feature.mine.dealinvoice.DealInvoiceController
import com.example.my_kmp_project.feature.mine.dealinvoice.DealInvoiceCustomer
import com.example.my_kmp_project.feature.mine.dealinvoice.DealInvoiceItem
import com.example.my_kmp_project.feature.mine.dealinvoice.DealInvoiceStatus
import com.example.my_kmp_project.feature.mine.dealinvoice.DealInvoiceSummary
import com.example.my_kmp_project.feature.mine.dealinvoice.DealInvoiceTab
import kotlinx.coroutines.launch
import my_kmp_project.core.design.generated.resources.Res
import my_kmp_project.core.design.generated.resources.community_avatar

/**
 * 新车成交（成交发票）—— 摘要 / 分类列表 / 详情全部来自
 * `GET /api/v1/deal-invoices`、`GET /api/v1/deal-invoices/summary` 与
 * `GET /api/v1/deal-invoices/{invoiceId}`。
 *
 * 原 PNG 假页（`deal_invoice_demo_body`）已删除：失败只展示错误与重试。
 */
@Composable
internal fun DealInvoiceDemoScreen(
    onBack: () -> Unit,
    onUpload: () -> Unit,
    controller: DealInvoiceController,
) {
    ReportMainTabRoot(isRoot = false)
    val scope = rememberCoroutineScope()
    LaunchedEffect(controller.tab) { controller.load() }

    Column(Modifier.fillMaxSize().background(MineTheme.Background)) {
        MineTopBar(
            title = "新车成交",
            onBack = onBack,
            containerColor = MineTheme.Surface,
            actions = {
                TextButton(onClick = onUpload) {
                    Text("上传发票", color = DemoColors.Accent, fontSize = 14.sp)
                }
            },
        )

        when {
            controller.loading && controller.summary == null && controller.items.isEmpty() ->
                DealInvoiceCenterHint("加载成交发票中…")
            controller.errorMessage != null && controller.items.isEmpty() ->
                DealInvoiceErrorBlock(
                    message = controller.errorMessage ?: "加载成交发票失败",
                    onRetry = { scope.launch { controller.load() } },
                )
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                controller.summary?.let { SummaryCard(it) }
                InvoiceTabRow(
                    selected = controller.tab,
                    onSelected = { controller.selectTab(it) },
                )
                controller.errorMessage?.let { message ->
                    Text(message, color = MineTheme.Danger, fontSize = 13.sp)
                }
                if (controller.items.isEmpty() && !controller.loading) {
                    DealInvoiceCenterHint("暂无成交发票")
                } else {
                    controller.items.forEach { item ->
                        InvoiceRow(
                            item = item,
                            onClick = { scope.launch { controller.openDetail(item.resolvedId) } },
                        )
                    }
                    if (controller.hasMore) {
                        TextButton(
                            onClick = { scope.launch { controller.loadMore() } },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("加载更多", color = DemoColors.Accent) }
                    }
                }
            }
        }
    }

    val detail = controller.detail
    if (detail != null) {
        InvoiceDetailDialog(item = detail, onDismiss = controller::closeDetail)
    }
}

@Composable
private fun SummaryCard(summary: DealInvoiceSummary) {
    val stats = summary.resolvedStats
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MineTheme.RadiusMd))
            .background(MineTheme.Surface)
            .border(MineTheme.Hairline, MineTheme.Separator, RoundedCornerShape(MineTheme.RadiusMd))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlatformNetworkImage(
                url = summary.resolvedAvatarUrl,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)),
                placeholder = Res.drawable.community_avatar,
                contentDescription = summary.resolvedDisplayName,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    summary.resolvedDisplayName.ifBlank { "—" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DemoColors.TextPrimary,
                )
                val subtitle = listOf(summary.resolvedPositionLabel, summary.resolvedStoreName)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                if (subtitle.isNotBlank()) {
                    Text(subtitle, fontSize = 12.sp, color = DemoColors.Muted)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth()) {
            SummaryStat("已上传", stats.resolvedUploaded, Modifier.weight(1f))
            SummaryStat("待审核", stats.resolvedPendingReview, Modifier.weight(1f))
            SummaryStat("已通过", stats.resolvedApproved, Modifier.weight(1f))
            SummaryStat("未通过", stats.resolvedRejected, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = DemoColors.TextPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = DemoColors.Muted)
    }
}

private val InvoiceTabs: List<Pair<DealInvoiceTab, String>> = listOf(
    DealInvoiceTab.All to "全部",
    DealInvoiceTab.PendingReview to "待审核",
    DealInvoiceTab.Approved to "已通过",
    DealInvoiceTab.Rejected to "未通过",
)

@Composable
private fun InvoiceTabRow(selected: DealInvoiceTab, onSelected: (DealInvoiceTab) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        InvoiceTabs.forEach { (tab, label) ->
            val isSelected = tab == selected
            Text(
                label,
                fontSize = 13.sp,
                color = if (isSelected) DemoColors.OnPrimary else DemoColors.TextPrimary,
                modifier = Modifier
                    .clip(DemoRadius.Pill)
                    .background(if (isSelected) DemoColors.Accent else MineTheme.FillSecondary)
                    .clickable { onSelected(tab) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun InvoiceRow(item: DealInvoiceItem, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .mineCard()
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                item.customerDisplay.ifBlank { "发票 ${item.resolvedId}" },
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = DemoColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                item.resolvedStatus.auditLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = invoiceStatusColor(item.resolvedStatus),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text("提交时间 ${item.resolvedSubmittedAt.ifBlank { "—" }}", fontSize = 12.sp, color = DemoColors.Muted)
        if (item.resolvedStatus == DealInvoiceStatus.Rejected) {
            val reason = item.rejectReason?.takeIf { it.isNotBlank() }
            if (reason != null) {
                Spacer(Modifier.height(4.dp))
                Text("驳回原因：$reason", fontSize = 12.sp, color = MineTheme.Danger)
            }
        }
    }
}

@Composable
private fun InvoiceDetailDialog(item: DealInvoiceItem, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发票详情") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("客户：${item.customerDisplay.ifBlank { "—" }}")
                Text("状态：${item.resolvedStatus.auditLabel}")
                Text("提交时间：${item.resolvedSubmittedAt.ifBlank { "—" }}")
                item.rejectReason?.takeIf { it.isNotBlank() }?.let {
                    Text("驳回原因：$it", color = MineTheme.Danger)
                }
                item.ratingStars?.let { Text("评分：$it 星") }
                if (item.imageUrl?.isNotBlank() == true) {
                    Spacer(Modifier.height(6.dp))
                    PlatformNetworkImage(
                        url = item.imageUrl.orEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(MineTheme.RadiusMd)),
                        placeholder = Res.drawable.community_avatar,
                        contentDescription = "发票图片",
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

/**
 * 上传成交发票 —— 客户列表来自 `GET /api/v1/deal-invoices/customers`，
 * 提交走 `POST /api/v1/deal-invoices`。
 *
 * 图片上传依赖对象存储（平台 gap），页面如实说明，不伪造提交成功。
 */
@Composable
internal fun DealInvoiceUploadScreen(onBack: () -> Unit, controller: DealInvoiceController) {
    ReportMainTabRoot(isRoot = false)
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { controller.loadCustomers() }
    var selectedId by remember { mutableStateOf<Int?>(null) }

    Column(Modifier.fillMaxSize().background(MineTheme.Background)) {
        MineTopBar(title = "上传成交发票", onBack = onBack, containerColor = MineTheme.Surface)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("成交客户", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = DemoColors.TextPrimary)
            when {
                controller.customersLoading && controller.customers.isEmpty() ->
                    DealInvoiceCenterHint("加载客户中…")
                controller.customers.isEmpty() ->
                    DealInvoiceCenterHint(controller.errorMessage ?: "暂无可上传的成交客户")
                else -> controller.customers.forEach { customer ->
                    CustomerRow(
                        customer = customer,
                        selected = customer.resolvedId == selectedId,
                        onClick = { selectedId = customer.resolvedId },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "发票图片上传需接入对象存储，当前先按成交客户提交审核。",
                fontSize = 12.sp,
                color = DemoColors.Muted,
            )
        }
        Button(
            onClick = {
                val customerId = selectedId ?: return@Button
                scope.launch {
                    controller.submit(customerId).fold(
                        onSuccess = {
                            showPlatformToast("已提交审核")
                            onBack()
                        },
                        onFailure = { showPlatformToast(it.message ?: "提交失败") },
                    )
                }
            },
            enabled = selectedId != null && !controller.submitting,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MineTheme.Accent),
        ) {
            Text(if (controller.submitting) "提交中…" else "提交审核", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun CustomerRow(customer: DealInvoiceCustomer, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MineTheme.RadiusMd))
            .background(if (selected) MineTheme.Accent.copy(alpha = 0.08f) else MineTheme.Surface)
            .border(
                width = if (selected) 1.dp else MineTheme.Hairline,
                color = if (selected) MineTheme.Accent else MineTheme.Separator,
                shape = RoundedCornerShape(MineTheme.RadiusMd),
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            customer.display.ifBlank { "客户 ${customer.resolvedId}" },
            fontSize = 15.sp,
            color = if (selected) MineTheme.Accent else DemoColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Text("已选择", fontSize = 12.sp, color = MineTheme.Accent)
        }
    }
}

private fun invoiceStatusColor(status: DealInvoiceStatus): Color = when (status) {
    DealInvoiceStatus.PendingReview -> Color(0xFFFF9500)
    DealInvoiceStatus.ApprovedPendingRating, DealInvoiceStatus.Rated -> DemoColors.Accent
    DealInvoiceStatus.Rejected -> MineTheme.Danger
}

@Composable
private fun DealInvoiceCenterHint(text: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = DemoColors.Muted, fontSize = 14.sp)
    }
}

@Composable
private fun DealInvoiceErrorBlock(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, color = DemoColors.TextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onRetry) { Text("重试", color = DemoColors.Accent) }
    }
}
