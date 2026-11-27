package com.example.my_kmp_project.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.DemoRadius
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.design.demoCard
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import com.example.my_kmp_project.core.design.MineIcons
import kotlinx.coroutines.launch

@Composable
internal fun AfterSalesListScreen(
    onBack: () -> Unit,
    onItem: (AfterSalesDetailRow) -> Unit,
    onCreate: () -> Unit,
) {
    // 服务端 `can_create` 决定是否展示「新建」入口。
    val canCreate = HomeSecondaryStore.afterSalesCanCreate
    val records = run {
        HomeSecondaryStore.version
        HomeSecondaryStore.afterSalesDetails()
    }
    val errorMessage = HomeSecondaryStore.errorMessage
    LaunchedEffect(Unit) {
        HomeSecondaryStore.refreshAfterSales()
    }
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color(0xFFF3F5F8))) {
        MineTopBar(
            title = "售后专区",
            onBack = onBack,
            containerColor = DemoColors.Background,
            actions = {
                if (canCreate) {
                    TextButton(onClick = onCreate) {
                        Text("新建", color = Color(0xFFD97706))
                    }
                }
            },
        )
        Column(
            Modifier
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFB45309), Color(0xFFD97706)),
                    ),
                )
                .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = MineIcons.Build,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        if (canCreate) "维修保养档案" else "我的服务记录",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (canCreate) "当前店服务记录与预约跟进" else "查看与您相关的服务记录",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "${records.size}",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text("记录", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            }
        }
        Text(
            if (canCreate) "维修保养记录" else "我的服务记录",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = Color(0xFF1C2430),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
        )
        errorMessage?.let { message ->
            Text(
                message,
                fontSize = 12.sp,
                color = DemoColors.Accent,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
            )
        }
        if (records.isEmpty()) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFECECEC)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = MineIcons.Build,
                            contentDescription = null,
                            tint = Color(0xFFB0B0B0),
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "暂无记录",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color(0xFF6B7280),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "完成售后服务后，在这里沉淀维修保养档案",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                }
            }
        } else {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(records, key = { it.id }) { row ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .demoCard()
                            .clickable { onItem(row) }
                            .padding(14.dp),
                    ) {
                        Text(row.title, fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${row.kindLabel} · ${row.customerName} · ${row.serviceDate}",
                            fontSize = 13.sp,
                            color = DemoColors.TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AfterSalesCreateScreen(onBack: () -> Unit) {
    // Flutter AfterSalesCreatePage
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }
    var mileage by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color(0xFFF3F5F8))) {
        MineTopBar(title = "创建售后", onBack = onBack, containerColor = DemoColors.Background)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            UsedCarField("客户姓名", name) { name = it; if (title.isBlank()) title = "$it 售后服务" }
            UsedCarField("手机号", phone) { phone = it }
            UsedCarField("工单标题", title) { title = it }
            UsedCarField("车牌", plate) { plate = it }
            UsedCarField("里程(km)", mileage) { mileage = it }
            UsedCarField("预约日", date) { date = it }
            UsedCarField("问题描述", content) { content = it }
            Spacer(Modifier.height(8.dp))
            Text(
                "提交",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DemoRadius.Pill)
                    .background(DemoColors.Accent)
                    .clickable {
                        val err = HomeSecondaryStore.validateAfterSalesCreate(
                            name = name.trim(),
                            phone = phone.trim(),
                            title = title.trim(),
                            plate = plate.trim(),
                            content = content.trim(),
                        )
                        if (err != null) {
                            showPlatformToast(err)
                        } else if (date.trim().isBlank()) {
                            showPlatformToast("请填写预约日")
                        } else {
                            scope.launch {
                                val ok = HomeSecondaryStore.createAfterSales(
                                    name = name.trim(),
                                    phone = phone.trim(),
                                    title = title.trim().ifBlank { "${name.trim()} 售后服务" },
                                    plate = plate.trim(),
                                    mileageKm = mileage.trim().toIntOrNull(),
                                    date = date.trim(),
                                    content = content.trim(),
                                )
                                if (ok) {
                                    showPlatformToast("已创建售后工单")
                                    onBack()
                                } else {
                                    showPlatformToast(HomeSecondaryStore.errorMessage ?: "创建售后工单失败")
                                }
                            }
                        }
                    }
                    .padding(vertical = 14.dp),
            )
        }
    }
}

@Composable
private fun AfterSalesDetailScreen(row: HomeListRow, onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color(0xFFF3F5F8))) {
        MineTopBar(title = "售后详情", onBack = onBack, containerColor = DemoColors.Background)
        Column(Modifier.padding(16.dp)) {
            UsedCarSection(
                "工单",
                listOf(
                    "编号" to row.title,
                    "内容" to row.subtitle,
                    "状态" to "进行中",
                    "预约日" to "2026-09-25",
                ),
            )
        }
    }
}
