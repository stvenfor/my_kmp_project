package com.example.my_kmp_project.feature.home

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
internal fun UsedCarListScreen(
    onBack: () -> Unit,
    onItem: (UsedCarOrderRow) -> Unit,
    onCreate: () -> Unit,
) {
    // Flutter UsedCarListPage: green summary + status/kind chips + order cards
    val accent = Color(0xFF0B6E4F)
    val ink = Color(0xFF1C2430)
    val bg = Color(0xFFF3F5F8)
    val allOrders = run {
        HomeSecondaryStore.version
        HomeSecondaryStore.usedCarOrders()
    }
    val stats = run {
        HomeSecondaryStore.version
        HomeSecondaryStore.usedCarStats
    }
    LaunchedEffect(Unit) {
        HomeSecondaryStore.refreshUsedCars()
    }
    val errorMessage = HomeSecondaryStore.errorMessage
    val statusTabs = listOf("全部", "待审核", "已通过", "未通过")
    val kindTabs = listOf("全部类型", "置换", "专卖", "收车")
    var status by remember { mutableStateOf("全部") }
    var kind by remember { mutableStateOf("全部类型") }
    val filtered = allOrders.filter { row ->
        val statusOk = status == "全部" ||
            row.statusLabel == status ||
            row.statusLabel.startsWith(status)
        val kindOk = kind == "全部类型" || row.kindLabel == kind
        statusOk && kindOk
    }
    val submitted = stats.resolvedSubmitted
    val pending = stats.resolvedPendingReview
    val approved = stats.resolvedApproved
    val rejected = stats.resolvedRejected

    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(bg)) {
        // Flutter UsedCarListPage AppNavBar (56.dp) + add_circle_outline action.
        FlutterFeatureTopBar(
            title = "二手车",
            onBack = onBack,
            containerColor = DemoColors.Background,
            actions = {
                // Flutter Icons.add_circle_outline (ink, not green glyph).
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clickable(onClick = onCreate),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                        val r = size.minDimension / 2f - stroke.width
                        drawCircle(color = ink, radius = r, style = stroke)
                        val arm = r * 0.45f
                        drawLine(ink, Offset(center.x - arm, center.y), Offset(center.x + arm, center.y), stroke.width, StrokeCap.Round)
                        drawLine(ink, Offset(center.x, center.y - arm), Offset(center.x, center.y + arm), stroke.width, StrokeCap.Round)
                    }
                }
            },
        )
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Column(
                    Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF0B6E4F), Color(0xFF149E6F))),
                        )
                        .padding(16.dp),
                ) {
                    Text("销售顾问", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "门店顾问 · 演示门店",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                    )
                    // Visual SoT: Flutter green card h≈414px; Spacer(20) matches (raw dart 14 is shorter on CMP fonts).
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth()) {
                        listOf(
                            "已提交" to submitted,
                            "待审核" to pending,
                            "已通过" to approved,
                            "未通过" to rejected,
                        ).forEach { (label, value) ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$value", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            item {
                // Flutter ChoiceChip/FilterChip = StadiumBorder; ~42.dp tall, row gap ~14.dp.
                // Flutter M3 ChoiceChip ≈ 8.dp corners; SoT blue h≈126px → 42.dp; selected wider via check+pad.
                val chipSelected = DemoColors.Accent
                val chipShape = RoundedCornerShape(8.dp)
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 7.dp, bottom = 12.dp)) {
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        statusTabs.forEach { tab ->
                            val selected = status == tab
                            Row(
                                Modifier
                                    .padding(end = 6.dp)
                                    .height(42.dp)
                                    .clip(chipShape)
                                    .background(if (selected) chipSelected else Color.White)
                                    .border(
                                        width = if (selected) 0.dp else 1.dp,
                                        color = if (selected) Color.Transparent else Color(0xFF1C2430),
                                        shape = chipShape,
                                    )
                                    .clickable { status = tab }
                                    .padding(horizontal = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (selected) {
                                    Canvas(Modifier.size(14.dp)) {
                                        val stroke = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round)
                                        val w = size.width
                                        val h = size.height
                                        drawLine(
                                            Color.White,
                                            Offset(w * 0.12f, h * 0.52f),
                                            Offset(w * 0.38f, h * 0.78f),
                                            stroke.width,
                                            StrokeCap.Round,
                                        )
                                        drawLine(
                                            Color.White,
                                            Offset(w * 0.38f, h * 0.78f),
                                            Offset(w * 0.88f, h * 0.22f),
                                            stroke.width,
                                            StrokeCap.Round,
                                        )
                                    }
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(
                                    tab,
                                    color = if (selected) Color.White else ink,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        kindTabs.forEach { tab ->
                            val selected = kind == tab
                            Row(
                                Modifier
                                    .padding(end = 6.dp)
                                    .height(42.dp)
                                    .clip(chipShape)
                                    .background(if (selected) chipSelected else Color.White)
                                    .border(
                                        width = if (selected) 0.dp else 1.dp,
                                        color = if (selected) Color.Transparent else Color(0xFF1C2430),
                                        shape = chipShape,
                                    )
                                    .clickable { kind = tab }
                                    .padding(horizontal = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (selected) {
                                    Canvas(Modifier.size(14.dp)) {
                                        val stroke = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round)
                                        val w = size.width
                                        val h = size.height
                                        drawLine(
                                            Color.White,
                                            Offset(w * 0.12f, h * 0.52f),
                                            Offset(w * 0.38f, h * 0.78f),
                                            stroke.width,
                                            StrokeCap.Round,
                                        )
                                        drawLine(
                                            Color.White,
                                            Offset(w * 0.38f, h * 0.78f),
                                            Offset(w * 0.88f, h * 0.22f),
                                            stroke.width,
                                            StrokeCap.Round,
                                        )
                                    }
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(
                                    tab,
                                    color = if (selected) Color.White else ink,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                }
            }
            errorMessage?.let { message ->
                item {
                    Text(
                        message,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = DemoColors.Accent,
                        fontSize = 12.sp,
                    )
                }
            }
            if (filtered.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("暂无业务单", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "点击右上角新建置换 / 专卖 / 收车单",
                            color = DemoColors.TextSecondary,
                            fontSize = 13.sp,
                        )
                    }
                }
            } else {
                items(filtered) { row ->
                    Column(
                        Modifier
                            .padding(horizontal = 16.dp, vertical = 5.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .clickable { onItem(row) }
                            .padding(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                row.kindLabel,
                                color = accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x140B6E4F))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                row.statusLabel,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFF0F2F5))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                            Spacer(Modifier.weight(1f))
                            Text(row.submittedDate, fontSize = 12.sp, color = DemoColors.TextSecondary)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            row.vehicleModel,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ink,
                            lineHeight = 22.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${row.plateNo} · ${row.modelYear}款 · ${row.mileageKm}km",
                            fontSize = 13.sp,
                            color = DemoColors.TextSecondary,
                            lineHeight = 18.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(row.amountLabel, fontSize = 12.sp, color = DemoColors.TextSecondary, lineHeight = 16.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "¥${row.amount}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = ink,
                                lineHeight = 24.sp,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(row.customerName, fontSize = 13.sp, color = DemoColors.TextSecondary, lineHeight = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun UsedCarCreateScreen(onBack: () -> Unit) {
    // Flutter UsedCarCreatePage — 新建业务单
    val accent = Color(0xFF0B6E4F)
    val bg = Color(0xFFF3F5F8)
    val kinds = listOf("置换" to "trade_in", "专卖" to "consign", "收车" to "purchase")
    var kind by remember { mutableStateOf("置换") }
    var customerIndex by remember { mutableIntStateOf(0) }
    var model by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }
    var vin by remember { mutableStateOf("") }
    var mileage by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val customers = HomeSecondaryStore.usedCarCustomers
    val selectedCustomer = customers.getOrNull(customerIndex)
    LaunchedEffect(Unit) {
        HomeSecondaryStore.refreshUsedCarCustomers()
    }
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(bg)) {
        MineTopBar(title = "新建业务单", onBack = onBack, containerColor = DemoColors.Background)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("业务类型", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                kinds.forEach { (label, _) ->
                    val selected = kind == label
                    Text(
                        label,
                        color = if (selected) Color.White else Color(0xFF1C2430),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) accent else Color.White)
                            .border(0.5.dp, if (selected) accent else DemoColors.Divider, RoundedCornerShape(16.dp))
                            .clickable { kind = label }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            Text("客户", fontWeight = FontWeight.SemiBold)
            Text(
                selectedCustomer?.let {
                    listOf(it.resolvedDisplayName, it.resolvedPhone).filter { v -> v.isNotBlank() }
                        .joinToString(" ")
                }?.ifBlank { "客户 #${selectedCustomer?.resolvedCustomerId}" } ?: "暂无可选客户",
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
                    .clickable {
                        if (customers.isEmpty()) {
                            showPlatformToast("暂无可选客户")
                        } else {
                            customerIndex = (customerIndex + 1) % customers.size
                        }
                    }
                    .padding(14.dp),
            )
            UsedCarField("车型名", model) { model = it }
            UsedCarField("车牌", plate) { plate = it }
            UsedCarField("VIN", vin) { vin = it }
            UsedCarField("里程(km)", mileage) { mileage = it }
            UsedCarField("年款", year) { year = it }
            UsedCarField("金额(元)", amount) { amount = it }
            Spacer(Modifier.height(8.dp))
            Text(
                "提交",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent)
                    .clickable {
                        val mileageInt = mileage.trim().toIntOrNull()
                        val yearInt = year.trim().toIntOrNull()
                        val amountInt = amount.trim().toIntOrNull()
                            ?: amount.trim().toDoubleOrNull()?.toInt()
                        val customer = selectedCustomer
                        val err = HomeSecondaryStore.validateUsedCarCreate(
                            customerName = customer?.resolvedDisplayName.orEmpty(),
                            vehicleModel = model.trim(),
                            plateNo = plate.trim(),
                            vin = vin.trim(),
                            mileageKm = mileageInt,
                            modelYear = yearInt,
                            amount = amountInt,
                        )
                        if (err != null) {
                            showPlatformToast(err)
                        } else if (customer == null || customer.resolvedCustomerId <= 0) {
                            showPlatformToast("暂无可选客户，无法建单")
                        } else {
                            scope.launch {
                                val ok = HomeSecondaryStore.createUsedCar(
                                    kind = kinds.first { it.first == kind }.second,
                                    customerId = customer.resolvedCustomerId,
                                    vehicleModel = model.trim(),
                                    plateNo = plate.trim(),
                                    vin = vin.trim(),
                                    mileageKm = mileageInt!!,
                                    modelYear = yearInt!!,
                                    amount = amountInt!!,
                                )
                                if (ok) {
                                    showPlatformToast("提交成功")
                                    onBack()
                                } else {
                                    showPlatformToast(HomeSecondaryStore.errorMessage ?: "提交失败")
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
internal fun UsedCarDetailScreen(order: UsedCarOrderRow, onBack: () -> Unit) {
    // Flutter UsedCarDetailPage
    val bg = Color(0xFFF3F5F8)
    val ink = Color(0xFF1C2430)
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(bg)) {
        MineTopBar(title = "业务单详情", onBack = onBack, containerColor = DemoColors.Background)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .padding(18.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        order.kindLabel,
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0B6E4F))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                    Text(
                        order.statusLabel,
                        color = Color(0xFF0B6E4F),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0B6E4F).copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(order.vehicleModel, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ink)
                Spacer(Modifier.height(6.dp))
                Text(
                    "${order.amountLabel} ¥${order.amount}",
                    color = Color(0xFF0B6E4F),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }
            UsedCarSection(
                "客户",
                listOf("姓名" to order.customerName, "手机" to order.phone.ifBlank { "—" }),
            )
            UsedCarSection(
                "车辆",
                listOf(
                    "车型" to order.vehicleModel,
                    "车牌" to order.plateNo,
                    "VIN" to order.vin.ifBlank { "—" },
                    "里程" to "${order.mileageKm} km",
                    "年款" to "${order.modelYear}",
                ),
            )
            UsedCarSection(
                "审核",
                listOf(
                    "状态" to order.statusLabel,
                    "提交时间" to order.submittedDate,
                ),
            )
        }
    }
}
