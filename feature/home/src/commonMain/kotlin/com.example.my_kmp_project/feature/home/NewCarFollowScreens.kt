package com.example.my_kmp_project.feature.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import kotlinx.coroutines.launch

@Composable
internal fun NewCarFollowListScreen(
    onBack: () -> Unit,
    onItem: (NewCarFollowRow) -> Unit,
    onCreate: () -> Unit,
) {
    // Flutter NewCarFollowListPage: profile header + intent tabs + cards + FAB
    val bg = Color(0xFFF5F6F8)
    val accent = Color(0xFF3B8CFF)
    val ink = Color(0xFF1A1A1A)
    val tabs = listOf("全部", "高意向", "中意向", "低意向", "逾期")
    var tab by remember { mutableStateOf("全部") }
    val all = run {
        HomeSecondaryStore.version
        HomeSecondaryStore.newCarFollows()
    }
    val errorMessage = HomeSecondaryStore.errorMessage
    LaunchedEffect(Unit) {
        HomeSecondaryStore.refreshNewCarFollows()
    }
    val filtered = when (tab) {
        "高意向" -> all.filter { it.intentBand == "高" }
        "中意向" -> all.filter { it.intentBand == "中" }
        "低意向" -> all.filter { it.intentBand == "低" }
        "逾期" -> all.filter { it.overdue }
        else -> all
    }
    val active = all.count { it.stage != "成交" && it.stage != "战败" }
    val overdue = all.count { it.overdue }
    val high = all.count { it.intentBand == "高" }
    val lost = all.count { it.stage == "战败" }

    ReportMainTabRoot(isRoot = false)
    Box(Modifier.fillMaxSize().background(bg)) {
        Column(Modifier.fillMaxSize()) {
            MineTopBar(title = "新车跟进", onBack = onBack, containerColor = DemoColors.Background)
            LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                item {
                    Column(
                        Modifier
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 20.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("销售顾问", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ink)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "顾问",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(accent)
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text("演示门店", color = DemoColors.TextSecondary, fontSize = 13.sp)
                            }
                            Box(
                                Modifier
                                    .width(56.dp)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(Color(0xFFE8EEF8)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("销", color = accent, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Row(Modifier.fillMaxWidth()) {
                            listOf(
                                "$active" to "跟进中",
                                "$overdue" to "逾期",
                                "$high" to "高意向",
                                "$lost" to "战败",
                            ).forEach { (v, label) ->
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(v, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ink)
                                    Spacer(Modifier.height(4.dp))
                                    Text(label, fontSize = 12.sp, color = DemoColors.TextSecondary)
                                }
                            }
                        }
                    }
                }
                item {
                    Row(
                        Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        tabs.forEach { t ->
                            val sel = tab == t
                            Column(
                                Modifier
                                    .clickable { tab = t }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    t,
                                    fontSize = 15.sp,
                                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (sel) ink else DemoColors.TextSecondary,
                                )
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    Modifier
                                        .width(28.dp)
                                        .height(3.dp)
                                        .background(if (sel) accent else Color.Transparent),
                                )
                            }
                        }
                    }
                }
                errorMessage?.let { message ->
                    item {
                        Text(
                            message,
                            fontSize = 12.sp,
                            color = DemoColors.Accent,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }
                if (filtered.isEmpty()) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("暂无跟进档案", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Text("点击下方按钮新建客户跟进", color = DemoColors.TextSecondary, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(filtered) { row ->
                        Column(
                            Modifier
                                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .clickable { onItem(row) }
                                .padding(14.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "客户",
                                    fontSize = 11.sp,
                                    color = accent,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x1A3B8CFF))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(row.customerName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = ink)
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "${row.intentBand}意向",
                                    fontSize = 12.sp,
                                    color = when (row.intentBand) {
                                        "高" -> Color(0xFFE53935)
                                        "中" -> Color(0xFFFF9500)
                                        else -> DemoColors.TextSecondary
                                    },
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(row.phone, fontSize = 13.sp, color = DemoColors.TextSecondary)
                            Spacer(Modifier.height(6.dp))
                            Text("意向车型: ${row.vehicle}", fontSize = 13.sp, color = DemoColors.TextSecondary)
                            Spacer(Modifier.height(8.dp))
                            Row {
                                Text("阶段 ${row.stage}", fontSize = 12.sp, color = DemoColors.TextSecondary)
                                Spacer(Modifier.width(12.dp))
                                Text("下次跟进 ${row.nextFollow}", fontSize = 12.sp, color = DemoColors.TextSecondary)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("负责人 ${row.owner}", fontSize = 12.sp, color = DemoColors.TextSecondary)
                        }
                    }
                }
            }
        }
        Button(
            onClick = onCreate,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent),
        ) {
            Text("新建跟进", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun NewCarFollowCreateScreen(onBack: () -> Unit) {
    // Flutter NewCarFollowCreatePage
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var vehicle by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("高") }
    val scope = rememberCoroutineScope()
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F6F8))) {
        MineTopBar(title = "新建跟进", onBack = onBack, containerColor = DemoColors.Background)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            UsedCarField("客户姓名", name) { name = it }
            UsedCarField("手机号", phone) { phone = it }
            UsedCarField("意向车型", vehicle) { vehicle = it }
            Text("意向等级", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("高", "中", "低").forEach { label ->
                    val selected = level == label
                    Text(
                        label,
                        color = if (selected) Color.White else DemoColors.TextPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) DemoColors.Accent else Color.White)
                            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(16.dp))
                            .clickable { level = label }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "提交",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DemoColors.Accent)
                    .clickable {
                        val err = HomeSecondaryStore.validateNewCarFollowCreate(
                            name = name.trim(),
                            phone = phone.trim(),
                        )
                        if (err != null) {
                            showPlatformToast(err)
                        } else {
                            scope.launch {
                                val ok = HomeSecondaryStore.createNewCarFollow(
                                    name = name.trim(),
                                    phone = phone.trim(),
                                    vehicle = vehicle.trim(),
                                    intentBand = level,
                                )
                                if (ok) {
                                    showPlatformToast("已保存跟进")
                                    onBack()
                                } else {
                                    showPlatformToast(HomeSecondaryStore.errorMessage ?: "建档失败")
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
private fun NewCarFollowDetailScreen(row: NewCarFollowRow, onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F6F8))) {
        MineTopBar(title = "跟进详情", onBack = onBack, containerColor = DemoColors.Background)
        Column(Modifier.padding(16.dp)) {
            UsedCarSection(
                "客户",
                listOf(
                    "姓名" to row.customerName,
                    "手机" to row.phone,
                    "意向车型" to row.vehicle,
                    "阶段" to row.stage,
                    "意向" to row.intentBand,
                    "下次跟进" to row.nextFollow,
                    "顾问" to row.owner,
                ),
            )
        }
    }
}
