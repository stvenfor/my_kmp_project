package com.example.my_kmp_project.feature.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextOverflow
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
    const val SmsTemplates = "/mine/sms_templates"
    const val ShopQr = "/mine/shop_qr"
    const val BuyQa = "/mine/buy_qa"
    const val Poster = "/mine/poster"
    const val Settings = "/mine/settings"
    const val Feedback = "/mine/feedback"
    const val Cooperation = "/mine/cooperation"
    const val Reminder = "/mine/reminder"
    const val Invite = "/mine/invite"
    const val FanGroup = "/mine/fan_group"

    fun fromLabel(label: String): String? = when (label.trim()) {
        "商城", "mall" -> Mall
        "我的订单", "订单" -> MallOrders
        "我的钱包", "钱包" -> Wallet
        "会员" -> Membership
        "我的课程", "课程" -> Classroom
        "短信模板" -> SmsTemplates
        "购车计算器", "计算器" -> Calculator
        "二手车" -> HomeRoutes.UsedCar
        "收支", "台账" -> HomeRoutes.Ledger
        "售后", "售后专区" -> HomeRoutes.AfterSales
        "小视频" -> ShortVideo
        "店铺收款码", "收款码" -> ShopQr
        "选买问答" -> BuyQa
        "商家海报", "海报" -> Poster
        "地址管理", "地址", "收货地址" -> Addresses
        "个人资料", "资料" -> Profile
        "签到日历" -> CheckIn
        "设置" -> Settings
        "意见反馈" -> Feedback
        "商务合作" -> Cooperation
        "提醒事项" -> Reminder
        "邀请好友" -> Invite
        "粉丝群" -> FanGroup
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
        route == MineRoutes.SmsTemplates -> SmsTemplateScreen(onBack = onBack)
        route == MineRoutes.ShopQr -> ShopQrScreen(onBack = onBack)
        route == MineRoutes.BuyQa -> BuyQaScreen(onBack = onBack)
        route == MineRoutes.Poster -> PosterScreen(onBack = onBack)
        route == MineRoutes.Settings -> SettingsScreen(onBack = onBack)
        route == MineRoutes.Feedback -> FeedbackScreen(onBack = onBack)
        route == MineRoutes.Cooperation -> SimpleDetail("商务合作", "商务合作申请入口 · 提交后由运营跟进", onBack)
        route == MineRoutes.Reminder -> SimpleDetail("提醒事项", "今日提醒 3 条 · 明日 1 条", onBack)
        route == MineRoutes.Invite -> SimpleDetail("邀请好友", "邀请码 DEMO2026 · 分享得积分", onBack)
        route == MineRoutes.FanGroup -> FanGroupScreen(onBack = onBack)
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
    // Flutter MallPage: search + categories + filter + 2-col cards + float bar
    val categories = listOf("推荐", "0元起兑", "国庆季", "钻铂专享", "数码家电", "生活好物")
    val filters = listOf("积分", "热兑", "上新", "筛选")
    var category by remember { mutableStateOf(0) }
    var filter by remember { mutableStateOf(1) }
    val products = remember {
        listOf(
            MallProductUi("精品脚垫", "¥99", "500积分", "热兑"),
            MallProductUi("车载香水", "¥59", "300积分", null),
            MallProductUi("保养套餐", "¥299", "1200积分", "上新"),
            MallProductUi("会员礼盒", "¥199", "800积分", "钻铂"),
            MallProductUi("视频会员卡", "¥25", "100积分", "热兑"),
            MallProductUi("行车记录仪", "¥399", "2000积分", null),
        )
    }
    ReportMainTabRoot(isRoot = false)
    Box(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "‹",
                    fontSize = 28.sp,
                    color = DemoColors.TextPrimary,
                    modifier = Modifier
                        .clickable(onClick = onBack)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
                Row(
                    Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF5F5F5))
                        .border(1.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
                        .clickable { showPlatformToast("搜索「视频会员卡」（开发中）") }
                        .padding(start = 10.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("⌕", color = DemoColors.Muted, fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("视频会员卡", color = DemoColors.Muted, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DemoColors.Accent)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text("搜索", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            HorizontalDivider(thickness = 1.dp, color = DemoColors.Divider)
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                categories.forEachIndexed { i, label ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            category = i
                            showPlatformToast("$label（筛选开发中）")
                        },
                    ) {
                        Text(
                            label,
                            fontSize = if (i == category) 15.sp else 14.sp,
                            fontWeight = if (i == category) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (i == category) DemoColors.TextPrimary else DemoColors.Muted,
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier
                                .width(if (i == category) 16.dp else 0.dp)
                                .height(2.dp)
                                .background(if (i == category) DemoColors.Accent else Color.Transparent),
                        )
                    }
                }
            }
            HorizontalDivider(thickness = 1.dp, color = DemoColors.Divider)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                filters.forEachIndexed { i, label ->
                    val active = i == filter
                    val showArrow = label == "积分" || label == "筛选"
                    Row(
                        Modifier
                            .weight(1f)
                            .height(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) DemoColors.Accent.copy(alpha = 0.1f) else Color.White)
                            .border(
                                1.dp,
                                if (active) DemoColors.Accent else DemoColors.Divider,
                                RoundedCornerShape(8.dp),
                            )
                            .clickable {
                                filter = i
                                showPlatformToast("$label（筛选开发中）")
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        if (active) Text("✓", color = DemoColors.Accent, fontSize = 11.sp)
                        Text(
                            label,
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (active) DemoColors.Accent else DemoColors.TextSecondary,
                        )
                        if (showArrow) {
                            Text("▾", color = if (active) DemoColors.Accent else DemoColors.Muted, fontSize = 11.sp)
                        }
                    }
                }
            }
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp, 4.dp, 10.dp, 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(products.chunked(2)) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { p ->
                            Column(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .clickable(onClick = onOpenDetail)
                                    .padding(bottom = 10.dp),
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .background(Color(0xFFE8F1FB)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("商品", color = DemoColors.Accent, fontWeight = FontWeight.SemiBold)
                                    p.tag?.let { tag ->
                                        Text(
                                            tag,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(6.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFEE0000))
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                                Text(
                                    p.name,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    maxLines = 2,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                )
                                Text(
                                    p.price,
                                    color = Color(0xFFEE0000),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                )
                                Text(
                                    p.points,
                                    color = DemoColors.Muted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 48.dp, end = 48.dp, bottom = 24.dp)
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .border(1.dp, DemoColors.Divider, RoundedCornerShape(28.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier.weight(1f).clickable { showPlatformToast("会员权益") },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("◎", color = DemoColors.Accent, fontSize = 16.sp)
                Text("会员权益", fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Box(Modifier.width(1.dp).height(24.dp).background(DemoColors.Divider))
            Column(
                Modifier.weight(1f).clickable { showPlatformToast("颜选好物") },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("店", color = DemoColors.Accent, fontSize = 16.sp)
                Text("颜选好物", fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

private data class MallProductUi(
    val name: String,
    val price: String,
    val points: String,
    val tag: String?,
)

private data class MallOrderUi(
    val no: String,
    val title: String,
    val qty: Int,
    val amount: String,
    val status: String,
    val unpaid: Boolean = false,
)

@Composable
private fun OrderListScreen(onBack: () -> Unit, onOpen: () -> Unit) {
    // Flutter MallOrdersPage: tabs 全部/待支付/已支付/已取消 + order cards
    val tabs = listOf("全部", "待支付", "已支付", "已取消")
    var tab by remember { mutableStateOf("全部") }
    val all = listOf(
        MallOrderUi("MO-1001", "车载香薰套装", 2, "128.00", "待支付", unpaid = true),
        MallOrderUi("MO-0998", "脚垫 · 全包围", 1, "399.00", "已支付"),
        MallOrderUi("MO-0992", "雨刷片一对", 1, "59.00", "已取消"),
    )
    val filtered = when (tab) {
        "待支付" -> all.filter { it.status == "待支付" }
        "已支付" -> all.filter { it.status == "已支付" || it.status == "待发货" }
        "已取消" -> all.filter { it.status == "已取消" }
        else -> all
    }
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        MineTopBar(title = "我的订单", onBack = onBack, containerColor = Color.White)
        Row(Modifier.fillMaxWidth().background(Color.White)) {
            tabs.forEach { t ->
                val sel = tab == t
                Column(
                    Modifier
                        .weight(1f)
                        .clickable { tab = t }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        t,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (sel) Color(0xFFFF9500) else DemoColors.TextSecondary,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .width(28.dp)
                            .height(2.dp)
                            .background(if (sel) Color(0xFFFF9500) else Color.Transparent),
                    )
                }
            }
        }
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无订单", color = DemoColors.TextSecondary)
            }
        } else {
            LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                items(filtered) { o ->
                    Row(
                        Modifier
                            .padding(bottom = 10.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .clickable(onClick = onOpen)
                            .padding(12.dp),
                    ) {
                        Box(
                            Modifier
                                .width(72.dp)
                                .height(72.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFEEEEEE)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("购", color = DemoColors.Muted)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row {
                                Text(
                                    o.title,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                )
                                Text(o.status, color = DemoColors.TextSecondary, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("共${o.qty}件 · ${o.no}", color = DemoColors.TextSecondary, fontSize = 12.sp)
                            if (o.unpaid) {
                                Spacer(Modifier.height(4.dp))
                                Text("待支付 · 请尽快完成", color = Color(0xFFFF9500), fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "¥${o.amount}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.align(Alignment.End),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletScreen(onBack: () -> Unit, onPay: () -> Unit) {
    // Flutter WalletPage: balance card + recharge + bank cards
    var amount by remember { mutableStateOf("") }
    var channel by remember { mutableStateOf(1) } // 1支付宝 2微信 3银行卡
    ReportMainTabRoot(isRoot = false)
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState()),
    ) {
        MineTopBar(title = "我的钱包", onBack = onBack, containerColor = Color.White)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(20.dp),
            ) {
                Text("余额（元）", color = DemoColors.TextSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Text("1,280.00", fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("充值", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            BasicTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                textStyle = TextStyle(fontSize = 15.sp, color = DemoColors.TextPrimary),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                decorationBox = { inner ->
                    if (amount.isEmpty()) Text("金额 0.01–50000", color = DemoColors.Muted, fontSize = 15.sp)
                    inner()
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to "支付宝", 2 to "微信", 3 to "银行卡").forEach { (id, label) ->
                    val sel = channel == id
                    Text(
                        label,
                        color = if (sel) Color.White else DemoColors.TextPrimary,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (sel) DemoColors.Accent else Color.White)
                            .border(1.dp, if (sel) DemoColors.Accent else DemoColors.Divider, RoundedCornerShape(16.dp))
                            .clickable { channel = id }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                listOf("10", "50", "100").forEach { a ->
                    Text(
                        a,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, DemoColors.Divider, RoundedCornerShape(16.dp))
                            .clickable { amount = a }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            Button(
                onClick = {
                    if (amount.isBlank()) showPlatformToast("请输入金额")
                    else {
                        showPlatformToast("充值 ¥$amount（mock）")
                        onPay()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Accent),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("确认充值", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            Text("银行卡", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            listOf("招商银行 ···· 8899", "工商银行 ···· 3321").forEach { card ->
                Text(
                    card,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(14.dp),
                )
            }
            TextButton(onClick = { showPlatformToast("添加银行卡（开发中）") }) {
                Text("+ 添加银行卡", color = DemoColors.Accent)
            }
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

@Composable
private fun SmsTemplateScreen(onBack: () -> Unit) {
    val templates = listOf(
        "到店提醒" to "尊敬的客户，预约保养已排至今日 14:00，请准时到店。",
        "试驾确认" to "您好，试驾预约已确认，顾问将提前电话联系您。",
        "活动邀约" to "本周末门店试驾会，到店即送礼品，欢迎莅临。",
        "回访关怀" to "购车已满一周，如有用车问题请随时联系您的顾问。",
    )
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        MineTopBar(title = "短信模板", onBack = onBack, containerColor = Color.White)
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            items(templates) { (title, body) ->
                Column(
                    Modifier
                        .padding(bottom = 10.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .padding(14.dp),
                ) {
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(body, color = DemoColors.TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "一键发送",
                        color = Color(0xFF0070F3),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable { showPlatformToast("已复制并打开短信（mock）") },
                    )
                }
            }
        }
    }
}

@Composable
private fun ShopQrScreen(onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MineTopBar(title = "店铺收款码", onBack = onBack, containerColor = Color.White)
        Spacer(Modifier.height(24.dp))
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("演示门店", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text("扫码向本店付款", color = DemoColors.TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier
                    .width(200.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF111111)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .width(160.dp)
                        .height(160.dp)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("QR", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color(0xFF111111))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("支持微信 / 支付宝", color = DemoColors.TextSecondary, fontSize = 12.sp)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "保存到相册",
            color = Color(0xFF0070F3),
            modifier = Modifier.clickable { showPlatformToast("已保存（mock）") },
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun FanGroupScreen(onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MineTopBar(title = "粉丝群", onBack = onBack, containerColor = Color.White)
        Spacer(Modifier.height(24.dp))
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("演示门店粉丝群", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text("扫码加入微信粉丝群", color = DemoColors.TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier
                    .width(200.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF07C160).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .width(160.dp)
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("QR", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color(0xFF07C160))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("长按识别二维码 · 演示码", color = DemoColors.TextSecondary, fontSize = 12.sp)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "保存二维码",
            color = Color(0xFF0070F3),
            modifier = Modifier.clickable { showPlatformToast("已保存（mock）") },
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun BuyQaScreen(onBack: () -> Unit) {
    val rows = listOf(
        "全款和贷款怎么选？" to "已解答 · 顾问回复",
        "置换能抵多少？" to "待回复 · 客户追问",
        "保养周期多久一次？" to "已解答 · 知识库",
    )
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        MineTopBar(title = "选买问答", onBack = onBack, containerColor = Color.White)
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            items(rows) { (q, status) ->
                Column(
                    Modifier
                        .padding(bottom = 10.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .padding(14.dp),
                ) {
                    Text(q, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(status, color = DemoColors.TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun PosterScreen(onBack: () -> Unit) {
    val posters = listOf("置换专场", "专卖精选", "估价引流", "到店礼")
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        MineTopBar(title = "商家海报", onBack = onBack, containerColor = Color.White)
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            items(posters.chunked(2)) { row ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { title ->
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFFF3E0)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(title.take(2), fontWeight = FontWeight.Bold, color = Color(0xFFFF9500))
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(onBack: () -> Unit) {
    var notify by remember { mutableStateOf(true) }
    var personalize by remember { mutableStateOf(true) }
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        MineTopBar(title = "设置", onBack = onBack, containerColor = Color.White)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("消息通知", Modifier.weight(1f))
                Switch(checked = notify, onCheckedChange = { notify = it })
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("个性化推荐", Modifier.weight(1f))
                Switch(checked = personalize, onCheckedChange = { personalize = it })
            }
            listOf("账号安全", "隐私协议", "关于我们").forEach { label ->
                Text(
                    label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .clickable { showPlatformToast(label) }
                        .padding(14.dp),
                )
            }
        }
    }
}

@Composable
private fun FeedbackScreen(onBack: () -> Unit) {
    var text by remember { mutableStateOf("") }
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        MineTopBar(title = "意见反馈", onBack = onBack, containerColor = Color.White)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = TextStyle(fontSize = 15.sp, color = DemoColors.TextPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .padding(14.dp),
                decorationBox = { inner ->
                    if (text.isEmpty()) Text("请描述问题或建议…", color = DemoColors.Muted)
                    inner()
                },
            )
            Button(
                onClick = {
                    if (text.isBlank()) showPlatformToast("请填写反馈内容")
                    else {
                        showPlatformToast("已提交")
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0070F3)),
            ) { Text("提交") }
        }
    }
}
