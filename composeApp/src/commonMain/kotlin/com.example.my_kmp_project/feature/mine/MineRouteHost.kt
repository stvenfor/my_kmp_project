package com.example.my_kmp_project.feature.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import com.example.my_kmp_project.feature.classroom.ClassroomRouteHost
import com.example.my_kmp_project.feature.classroom.ClassroomRoutes
import com.example.my_kmp_project.feature.commerce.MembershipScreen
import com.example.my_kmp_project.feature.home.HomeRoutes
import com.example.my_kmp_project.feature.media.VideoRouteHost
import com.example.my_kmp_project.feature.media.VideoRoutes

internal object MineRoutes {
    const val Mall = "/mall"
    const val MallDetail = "/mall/detail"
    const val MallOrders = "/mall/orders"
    const val MallOrderDetail = "/mall/orders/detail"
    const val Wallet = "/wallet"
    const val Pay = "/pay"
    const val Membership = "/pay/membership"
    const val Profile = "/mine/profile"
    const val Addresses = "/mine/addresses"
    const val AddressEdit = "/mine/addresses/edit"
    const val Calculator = "/mine/purchase_calculator"
    const val Classroom = ClassroomRoutes.MyClass
    const val ShortVideo = VideoRoutes.Short
    const val CheckIn = "/home/check_in_mall"

    fun fromLabel(label: String): String? = when (label.trim()) {
        "商城", "mall" -> Mall
        "我的订单", "订单" -> MallOrders
        "我的钱包", "钱包" -> Wallet
        "会员" -> Membership
        "我的课程", "课程" -> Classroom
        "短信模板" -> null // stub toast
        "购车计算器", "计算器" -> Calculator
        "二手车" -> HomeRoutes.UsedCar
        "小视频" -> ShortVideo
        "地址管理", "地址" -> Addresses
        "个人资料", "资料" -> Profile
        "签到日历" -> CheckIn
        else -> null
    }
}

@Composable
internal fun MineRouteHost(
    route: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
) {
    when {
        route.startsWith("/classroom") -> ClassroomRouteHost(route, onBack, onNavigate)
        route.startsWith("/video") -> VideoRouteHost(route, onBack, onNavigate)
        route == MineRoutes.Mall -> MallListScreen(
            onBack = onBack,
            onOpenDetail = { onNavigate(MineRoutes.MallDetail) },
            onOpenOrders = { onNavigate(MineRoutes.MallOrders) },
        )
        route == MineRoutes.MallDetail -> SimpleDetail("商品详情", "SKU · 示例商品 · ¥199", onBack)
        route == MineRoutes.MallOrders -> OrderListScreen(
            onBack = onBack,
            onOpen = { onNavigate(MineRoutes.MallOrderDetail) },
        )
        route == MineRoutes.MallOrderDetail -> SimpleDetail("订单详情", "订单 #MO-1001 · 待发货", onBack)
        route == MineRoutes.Wallet -> WalletScreen(onBack = onBack, onPay = { onNavigate(MineRoutes.Pay) })
        route == MineRoutes.Pay -> SimpleDetail("收银台", "支付网关 stub（见 platform-gap）", onBack)
        route == MineRoutes.Membership -> MembershipScreen(onBack = onBack)
        route == MineRoutes.Profile -> ProfileEditScreen(onBack = onBack)
        route == MineRoutes.Addresses -> AddressListScreen(
            onBack = onBack,
            onEdit = { onNavigate(MineRoutes.AddressEdit) },
        )
        route == MineRoutes.AddressEdit -> AddressEditScreen(onBack = onBack)
        route == MineRoutes.Calculator -> PurchaseCalculatorScreen(onBack = onBack)
        route == MineRoutes.CheckIn -> {
            SimpleDetail("签到", "请从 Home 签到商城进入完整页", onBack)
        }
        else -> SimpleDetail("我的", "route=$route", onBack)
    }
}

@Composable
private fun SimpleDetail(title: String, body: String, onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = title, onBack = onBack)
        Text(body, modifier = Modifier.padding(16.dp), color = DemoColors.TextPrimary)
    }
}

@Composable
private fun MallListScreen(
    onBack: () -> Unit,
    onOpenDetail: () -> Unit,
    onOpenOrders: () -> Unit,
) {
    val items = listOf("精品脚垫", "车载香水", "保养套餐", "会员礼盒")
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(
            title = "商城",
            onBack = onBack,
            actions = {
                TextButton(onClick = onOpenOrders) { Text("订单", color = DemoColors.Accent) }
            },
        )
        LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items) { name ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DemoColors.Background)
                        .clickable(onClick = onOpenDetail)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(name, fontWeight = FontWeight.Medium)
                    Text("¥99 起", color = DemoColors.Accent)
                }
            }
        }
    }
}

@Composable
private fun OrderListScreen(onBack: () -> Unit, onOpen: () -> Unit) {
    val orders = listOf("MO-1001 待发货", "MO-0998 已完成", "MO-0992 已取消")
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "我的订单", onBack = onBack)
        LazyColumn {
            items(orders) { o ->
                Text(
                    o,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpen)
                        .padding(16.dp),
                )
                HorizontalDivider(color = DemoColors.Divider)
            }
        }
    }
}

@Composable
private fun WalletScreen(onBack: () -> Unit, onPay: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "我的钱包", onBack = onBack)
        Column(Modifier.padding(16.dp)) {
            Text("余额（元）", color = DemoColors.TextSecondary)
            Text("1,280.00", fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onPay,
                colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Primary),
            ) { Text("去充值") }
        }
    }
}

@Composable
private fun ProfileEditScreen(onBack: () -> Unit) {
    var name by remember { mutableStateOf("qa_user") }
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "个人资料", onBack = onBack)
        Column(Modifier.padding(16.dp)) {
            Text("昵称")
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                textStyle = TextStyle(fontSize = 16.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DemoColors.Background)
                    .padding(12.dp),
            )
            Button(
                onClick = {
                    showPlatformToast("已保存")
                    onBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Primary),
            ) { Text("保存") }
        }
    }
}

@Composable
private fun AddressListScreen(onBack: () -> Unit, onEdit: () -> Unit) {
    val list = listOf("家 · 北京市朝阳区…", "公司 · 上海市浦东新区…")
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(
            title = "地址管理",
            onBack = onBack,
            actions = {
                TextButton(onClick = onEdit) { Text("新建", color = DemoColors.Accent) }
            },
        )
        LazyColumn {
            items(list) { a ->
                Text(
                    a,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onEdit)
                        .padding(16.dp),
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun AddressEditScreen(onBack: () -> Unit) {
    var line by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(true) }
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "编辑地址", onBack = onBack)
        Column(Modifier.padding(16.dp)) {
            BasicTextField(
                value = line,
                onValueChange = { line = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DemoColors.Background)
                    .padding(12.dp),
                decorationBox = { inner ->
                    if (line.isEmpty()) Text("详细地址", color = DemoColors.Muted)
                    inner()
                },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("设为默认", modifier = Modifier.weight(1f))
                Switch(checked = isDefault, onCheckedChange = { isDefault = it })
            }
            Button(
                onClick = {
                    if (line.isBlank()) showPlatformToast("请填写地址")
                    else {
                        showPlatformToast("已保存")
                        onBack()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Primary),
            ) { Text("保存") }
        }
    }
}

@Composable
private fun PurchaseCalculatorScreen(onBack: () -> Unit) {
    var price by remember { mutableStateOf("150000") }
    var down by remember { mutableStateOf("30") }
    ReportMainTabRoot(isRoot = false)
    Column(
        Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .verticalScroll(rememberScrollState()),
    ) {
        MineTopBar(title = "购车计算器", onBack = onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("车价（元）")
            BasicTextField(
                value = price,
                onValueChange = { price = it.filter { c -> c.isDigit() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DemoColors.Background)
                    .padding(12.dp),
            )
            Text("首付比例（%）")
            BasicTextField(
                value = down,
                onValueChange = { down = it.filter { c -> c.isDigit() }.take(2) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DemoColors.Background)
                    .padding(12.dp),
            )
            val p = price.toIntOrNull() ?: 0
            val d = (down.toIntOrNull() ?: 0).coerceIn(0, 100)
            val downPay = p * d / 100
            val loan = p - downPay
            Text("首付约 ¥$downPay", fontWeight = FontWeight.SemiBold)
            Text("贷款约 ¥$loan", fontWeight = FontWeight.SemiBold)
        }
    }
}
