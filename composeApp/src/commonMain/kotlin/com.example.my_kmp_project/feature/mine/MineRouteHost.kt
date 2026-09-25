package com.example.my_kmp_project.feature.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
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
        "收支", "台账" -> HomeRoutes.Ledger
        "售后", "售后专区" -> HomeRoutes.AfterSales
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

private data class FinanceProductUi(
    val id: Int,
    val name: String,
    val subtitle: String,
)

@Composable
private fun PurchaseCalculatorScreen(onBack: () -> Unit) {
    // Flutter PurchaseCalculatorPage layout (cash/loan + products + 计算报价).
    var mode by remember { mutableStateOf("cash") } // cash | loan
    var barePrice by remember { mutableStateOf("100000") }
    var taxable by remember { mutableStateOf("") }
    var includeCommercial by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf(1) }
    var quoteLines by remember { mutableStateOf<List<Pair<String, String>>?>(null) }
    val products = remember {
        listOf(
            FinanceProductUi(1, "示例银行车贷", "年利率 4.5% · 最低首付 20.0%"),
            FinanceProductUi(2, "厂商金融贴息", "年利率 4.5% · 最低首付 20.0% · 贴息减 0.5%"),
            FinanceProductUi(3, "低息精品贷", "年利率 3.98% · 最低首付 15.0% · 减本金 ¥2000"),
        )
    }
    ReportMainTabRoot(isRoot = false)
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6F8))
            .verticalScroll(rememberScrollState()),
    ) {
        MineTopBar(title = "购车计算器", onBack = onBack, containerColor = Color.White)
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CalculatorSection {
                Text("付款方式", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeChip("全款", selected = mode == "cash") { mode = "cash" }
                    ModeChip("贷款", selected = mode == "loan") { mode = "loan" }
                }
                Spacer(Modifier.height(12.dp))
                CalculatorField("裸车价（元）", barePrice) { barePrice = it.filter { c -> c.isDigit() || c == '.' } }
                Spacer(Modifier.height(8.dp))
                CalculatorField("计税价格（可选，默认裸车价/1.13）", taxable) {
                    taxable = it.filter { c -> c.isDigit() || c == '.' }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("计入商业险粗算", modifier = Modifier.weight(1f), fontSize = 15.sp)
                    Switch(
                        checked = includeCommercial,
                        onCheckedChange = { includeCommercial = it },
                    )
                }
            }
            CalculatorSection {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "金融产品",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { showPlatformToast("已刷新金融产品（mock）") }) {
                        Text("刷新", color = DemoColors.Accent)
                    }
                }
                products.forEach { p ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { selectedProduct = p.id }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            if (selectedProduct == p.id) "◉" else "○",
                            color = if (selectedProduct == p.id) DemoColors.Accent else DemoColors.Muted,
                            fontSize = 18.sp,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                p.name,
                                color = if (selectedProduct == p.id) DemoColors.Accent else DemoColors.TextPrimary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                            )
                            Text(p.subtitle, color = DemoColors.TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
            Button(
                onClick = {
                    val bare = barePrice.toDoubleOrNull() ?: 0.0
                    val tax = taxable.toDoubleOrNull() ?: (bare / 1.13)
                    val product = products.first { it.id == selectedProduct }
                    quoteLines = listOf(
                        "付款方式" to if (mode == "cash") "全款" else "贷款",
                        "金融产品" to product.name,
                        "裸车价" to "¥${bare.toInt()}",
                        "计税价格" to "¥${tax.toInt()}",
                        "商业险" to if (includeCommercial) "已计入粗算" else "未计入",
                        "合计参考" to "¥${(bare * 1.08).toInt()}",
                    )
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF171717),
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("计算报价", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            quoteLines?.let { lines ->
                CalculatorSection {
                    Text("报价结果", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    lines.forEach { (k, v) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(k, color = DemoColors.TextSecondary, modifier = Modifier.weight(1f))
                            Text(v, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalculatorSection(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(14.dp),
    ) {
        content()
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) DemoColors.Accent else Color.White)
            .border(
                1.dp,
                if (selected) DemoColors.Accent else DemoColors.Divider,
                RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Text("✓ ", color = Color.White, fontSize = 12.sp)
        }
        Text(
            label,
            color = if (selected) Color.White else DemoColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun CalculatorField(label: String, value: String, onChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp, color = DemoColors.TextSecondary)
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(fontSize = 15.sp, color = DemoColors.TextPrimary),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF5F6F8))
                .padding(horizontal = 12.dp, vertical = 12.dp),
        )
    }
}
