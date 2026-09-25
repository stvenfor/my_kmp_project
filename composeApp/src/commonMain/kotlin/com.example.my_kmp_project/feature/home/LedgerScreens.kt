package com.example.my_kmp_project.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import kotlin.math.abs

/** Flutter `TransactionModel` fields used by ledger list/detail. */
internal data class LedgerTransaction(
    val id: Int,
    val type: String,
    val category: String,
    val amount: Double,
    val date: String,
    val note: String? = null,
)

/**
 * Flutter `LedgerListPage` + `TransactionListItem`.
 * Live Flutter often returns empty API → "暂无收支记录"; mock keeps item chrome for UI parity.
 */
@Composable
internal fun LedgerListScreen(
    items: List<LedgerTransaction>,
    onBack: () -> Unit,
    onItem: (LedgerTransaction) -> Unit,
) {
    ReportMainTabRoot(isRoot = false)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6F8)),
    ) {
        MineTopBar(
            title = "收支",
            onBack = onBack,
            containerColor = Color.White,
        )
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无收支记录", color = DemoColors.TextSecondary, fontSize = 15.sp)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)) {
                items(items, key = { it.id }) { item ->
                    TransactionListItem(
                        item = item,
                        onClick = { onItem(item) },
                    )
                }
            }
        }
    }
}

/** Flutter `LedgerDetailPage`. */
@Composable
internal fun LedgerDetailScreen(
    item: LedgerTransaction?,
    onBack: () -> Unit,
) {
    ReportMainTabRoot(isRoot = false)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6F8)),
    ) {
        MineTopBar(
            title = "收支详情",
            onBack = onBack,
            containerColor = Color.White,
        )
        if (item == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("记录不存在", color = DemoColors.TextSecondary)
            }
        } else {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = item.category,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = formatLedgerAmount(item.amount),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1A1A1A),
                )
                Spacer(Modifier.height(16.dp))
                Text("类型：${item.type}", color = DemoColors.TextPrimary, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                Text("日期：${item.date}", color = DemoColors.TextPrimary, fontSize = 15.sp)
                if (!item.note.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("备注：${item.note}", color = DemoColors.TextPrimary, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun TransactionListItem(
    item: LedgerTransaction,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 0.dp)
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TypeTag(type = item.type)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = item.category,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = formatLedgerAmount(item.amount),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.date, fontSize = 13.sp, color = Color(0xFF757575))
                if (!item.note.isNullOrBlank()) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = item.note,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 13.sp,
                        color = Color(0xFF9E9E9E),
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Text("›", fontSize = 22.sp, color = Color(0xFFBDBDBD))
    }
}

@Composable
private fun TypeTag(type: String) {
    val lower = type.lowercase()
    val (label, fg, bg) = when {
        "income" in lower || "收入" in type ->
            Triple("收入", Color(0xFF2E7D32), Color(0xFFE8F5E9))
        "expense" in lower || "支出" in type ->
            Triple("支出", Color(0xFFC62828), Color(0xFFFFEBEE))
        else -> Triple(type, Color(0xFF1565C0), Color(0xFFE3F2FD))
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

internal fun formatLedgerAmount(amount: Double): String {
    val abs = abs(amount)
    return if (abs >= 10_000.0) {
        "¥ ${"%.2f".format(amount / 10_000.0)} 万"
    } else {
        "¥ ${"%.2f".format(amount)}"
    }
}
