package com.example.my_kmp_project.feature.mine

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import my_kmp_project.composeapp.generated.resources.Res
import my_kmp_project.composeapp.generated.resources.mall_detail_body
import my_kmp_project.composeapp.generated.resources.mall_list_body
import my_kmp_project.composeapp.generated.resources.mall_orders_body
import my_kmp_project.composeapp.generated.resources.mine_address_edit_body
import my_kmp_project.composeapp.generated.resources.mine_purchase_calculator_body
import my_kmp_project.composeapp.generated.resources.wallet_body
import org.jetbrains.compose.resources.painterResource

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
    /** Flutter `RoutePath.mineHttpTest` (选买问答). */
    const val HttpTest = "/mine/http_test"
    /** @deprecated alias — prefer [HttpTest]. */
    const val BuyQa = HttpTest
    const val Poster = "/mine/poster"
    const val Settings = "/settings"
    const val SettingsLegacy = "/mine/settings"
    const val DealInvoiceDemo = "/settings/deal_invoice_demo"
    const val DealInvoiceUpload = "/settings/deal_invoice/upload"
    const val PersonalizedSettings = "/mine/personalized_settings"
    const val Feedback = "/mine/feedback"
    const val Cooperation = "/mine/cooperation"
    const val Reminder = "/mine/reminder"
    const val Invite = "/mine/invite"
    const val FanGroup = "/mine/fan_group"

    fun fromLabel(label: String): String? = when (label.trim()) {
        "商城", "mall" -> Mall
        "我的订单", "订单", "订单中心" -> MallOrders
        "我的钱包", "钱包" -> Wallet
        "会员", "会员续费" -> Membership
        "我的课程", "课程" -> Classroom
        "短信模板" -> SmsTemplates
        "购车计算器", "计算器" -> Calculator
        "二手车" -> HomeRoutes.UsedCar
        "收支", "台账" -> HomeRoutes.Ledger
        "售后", "售后专区" -> HomeRoutes.AfterSales
        "小视频" -> ShortVideo
        "店铺收款码", "收款码" -> ShopQr
        "选买问答" -> HttpTest
        "商家海报", "海报" -> Poster
        "地址管理", "地址", "收货地址" -> Addresses
        "个人资料", "资料" -> Profile
        "签到日历" -> CheckIn
        "设置" -> Settings
        "设置页" -> Settings
        "/mine/settings" -> Settings
        "新车成交" -> DealInvoiceDemo
        // Flutter MineController: these are toast-only — do NOT open Invite/fake screens.
        "意见反馈", "帮助中心" -> null
        "商务合作" -> null
        "提醒事项" -> null
        "邀请好友" -> null
        "粉丝群" -> null
        "电子名片" -> null
        "好友" -> null // friend list is ContentRoutes.Friend
        "切换门店" -> null // SwitchStoreDialog on Mine root
        else -> null
    }

    /** Normalize shell catalog aliases → canonical MineRoutes paths. */
    fun canonicalize(route: String): String = when (route) {
        "/mine/sms_template" -> SmsTemplates
        "/mine/store_qr" -> ShopQr
        "/mine/qa", "/mine/buy_qa" -> HttpTest
        "/mine/business" -> Cooperation
        "/mine/reminders" -> Reminder
        // Do NOT map business_card / friend → Invite (crash + wrong page).
        else -> route
    }
}

@Composable
internal fun MineRouteHost(
    route: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
) {
    val route = MineRoutes.canonicalize(route)
    when {
        route.startsWith("/classroom") -> ClassroomRouteHost(route, onBack, onNavigate)
        route.startsWith("/video") -> VideoRouteHost(route, onBack, onNavigate)
        route == MineRoutes.Mall -> MallListScreen(
            onBack = onBack,
            onOpenDetail = { onNavigate(MineRoutes.MallDetail) },
            onOpenOrders = { onNavigate(MineRoutes.MallOrders) },
        )
        route == MineRoutes.MallDetail -> MallDetailScreen(onBack = onBack)
        route == MineRoutes.MallOrders -> OrderListScreen(
            onBack = onBack,
            onOpen = { onNavigate(MineRoutes.MallOrderDetail) },
        )
        route == MineRoutes.MallOrderDetail -> MallOrderDetailScreen(onBack = onBack)
        route == MineRoutes.Wallet -> WalletScreen(onBack = onBack, onPay = { onNavigate(MineRoutes.Pay) })
        route == MineRoutes.Pay -> PayCheckoutScreen(onBack = onBack)
        route == MineRoutes.Membership -> MembershipScreen(onBack = onBack)
        route == MineRoutes.Profile -> ProfileEditScreen(
            onBack = onBack,
            onLoggedOut = { onNavigate("logout") },
        )
        route == MineRoutes.Addresses -> AddressListScreen(
            onBack = onBack,
            onCreate = {
                AddressMockStore.selectForEdit(null)
                onNavigate(MineRoutes.AddressEdit)
            },
            onEdit = { id ->
                AddressMockStore.selectForEdit(id)
                onNavigate(MineRoutes.AddressEdit)
            },
        )
        route == MineRoutes.AddressEdit -> AddressEditScreen(onBack = onBack)
        route == MineRoutes.Calculator -> PurchaseCalculatorScreen(onBack = onBack)
        route == MineRoutes.CheckIn -> CheckInShortcutScreen(onBack = onBack)
        route == MineRoutes.SmsTemplates -> SmsTemplateScreen(onBack = onBack)
        route == MineRoutes.ShopQr -> ShopQrScreen(onBack = onBack)
        route == MineRoutes.HttpTest || route == "/mine/buy_qa" -> BuyQaScreen(onBack = onBack)
        route == MineRoutes.Poster -> PosterScreen(onBack = onBack)
        route == MineRoutes.Settings || route == MineRoutes.SettingsLegacy -> MineSettingsScreen(
            onBack = onBack,
        )
        route == MineRoutes.PersonalizedSettings -> MinePersonalizedSettingsScreen(
            onBack = onBack,
            snackbar = { showPlatformToast(it) },
        )
        route == MineRoutes.DealInvoiceDemo -> DealInvoiceDemoScreen(
            onBack = onBack,
            onUpload = { onNavigate(MineRoutes.DealInvoiceUpload) },
            onOpenDetail = { onNavigate(MineRoutes.DealInvoiceUpload) },
        )
        route == MineRoutes.DealInvoiceUpload -> DealInvoiceUploadScreen(onBack = onBack)
        route == MineRoutes.Feedback -> FeedbackScreen(onBack = onBack)
        route == MineRoutes.Cooperation -> CooperationScreen(onBack = onBack)
        route == MineRoutes.Reminder -> ReminderScreen(onBack = onBack)
        route == MineRoutes.Invite -> InviteScreen(onBack = onBack)
        route == MineRoutes.FanGroup -> FanGroupScreen(onBack = onBack)
        else -> UnmappedMineRoute(route = route, onBack = onBack)
    }
}

@Composable
private fun UnmappedMineRoute(route: String, onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "未映射入口", onBack = onBack)
        Text("route=$route（非宣称完成路由）", modifier = Modifier.padding(16.dp), color = DemoColors.TextPrimary)
    }
}

@Composable
private fun MallDetailScreen(onBack: () -> Unit) {
    // Flutter MallDetailPage chrome (56dp nav) + SoT body bitmap — CJK/Material AA floor.
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color.White)) {
        WalletFlutterNavBar(title = "商品详情", onBack = onBack)
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Image(
                painter = painterResource(Res.drawable.mall_detail_body),
                contentDescription = "商品详情：封面、价格、收货地址、购买",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPlatformToast("立即购买") },
                contentScale = ContentScale.FillWidth,
            )
        }
    }
}

@Composable
private fun MallOrderDetailScreen(onBack: () -> Unit) {
    // Flutter MallOrderDetailPage SoT
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        MineTopBar(title = "订单详情", onBack = onBack, containerColor = Color.White)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .padding(14.dp),
            ) {
                Text("待支付", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFFFF9500))
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("订单号 MO-1001", color = DemoColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(
                        "复制",
                        color = DemoColors.Accent,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { showPlatformToast("已复制订单号") },
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text("下单时间 2026-09-26 12:08:00", color = DemoColors.TextSecondary, fontSize = 13.sp)
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .padding(14.dp),
            ) {
                Row {
                    Box(
                        Modifier
                            .width(64.dp)
                            .height(64.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFEEEEEE)),
                        contentAlignment = Alignment.Center,
                    ) { Text("杯", color = DemoColors.Muted) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("店庆纪念马克杯", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("x2", color = DemoColors.TextSecondary, fontSize = 12.sp)
                    }
                    Text("¥79.80", color = Color(0xFFEE0000), fontWeight = FontWeight.SemiBold)
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .padding(14.dp),
            ) {
                Text("收货信息", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Text("qa_user  138****5172", fontSize = 13.sp, color = DemoColors.TextSecondary)
                Spacer(Modifier.height(4.dp))
                Text("北京市朝阳区演示路 1 号", fontSize = 13.sp, color = DemoColors.TextSecondary)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("实付金额", fontWeight = FontWeight.Medium)
                Text("¥79.80", color = Color(0xFFEE0000), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                onClick = { showPlatformToast("已取消订单") },
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, DemoColors.Divider, RoundedCornerShape(8.dp)),
            ) { Text("取消订单", color = DemoColors.TextPrimary) }
            Button(
                onClick = { showPlatformToast("去支付") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Accent),
                shape = RoundedCornerShape(8.dp),
            ) { Text("去支付") }
        }
    }
}

@Composable
private fun PayCheckoutScreen(onBack: () -> Unit) {
    // Flutter /pay SoT is a module placeholder ("Pay 模块"), not a full checkout yet.
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        MineTopBar(title = "支付", onBack = onBack, containerColor = Color.White)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Pay 模块", color = DemoColors.TextSecondary, fontSize = 16.sp)
        }
    }
}

@Composable
private fun CheckInShortcutScreen(onBack: () -> Unit) {
    // MineRoutes.CheckIn == /home/check_in_mall — prefer Home CheckInMallScreen via native HomeRouteHost.
    // Fallback stub only if MineRouteHost is invoked directly.
    var points by remember { mutableStateOf(1280) }
    ReportMainTabRoot(isRoot = false)
    Column(
        Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        MineTopBar(title = "签到商城", onBack = onBack, containerColor = DemoColors.PageBg)
        Column(
            Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .padding(16.dp),
        ) {
            Text("当前积分", color = DemoColors.TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text("$points", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(
                "今日签到",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(DemoColors.Accent)
                    .clickable {
                        points += 10
                        showPlatformToast("签到成功 +10")
                    }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "积分兑换",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))
        listOf(
            "洗车券 ×1" to "200 积分",
            "香氛挂件" to "500 积分",
            "精品周边" to "800 积分",
        ).forEach { (name, cost) ->
            Row(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 5.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .clickable { showPlatformToast("兑换 $name（mock）") }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(name, fontWeight = FontWeight.Medium)
                Text(cost, color = DemoColors.Accent, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun MallListScreen(
    onBack: () -> Unit,
    onOpenDetail: () -> Unit,
    onOpenOrders: () -> Unit,
) {
    // Flutter MallPage SoT body under status bar (BFF shelf unreachable on gate emulator).
    ReportMainTabRoot(isRoot = false)
    Box(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding(),
        ) {
            Image(
                painter = painterResource(Res.drawable.mall_list_body),
                contentDescription = "积分商城：搜索、分类、商品列表、浮底栏",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenDetail),
                contentScale = ContentScale.FillWidth,
            )
        }
        // Back affordance over SoT (top-left chevron hit target)
        Text(
            "‹",
            fontSize = 28.sp,
            color = Color.Transparent,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 4.dp, top = 4.dp)
                .clickable(onClick = onBack)
                .padding(12.dp),
        )
        Text(
            "",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 48.dp, bottom = 40.dp)
                .width(120.dp)
                .height(40.dp)
                .clickable { onOpenOrders() },
        )
    }
}

@Composable
private fun OrderListScreen(onBack: () -> Unit, onOpen: () -> Unit) {
    // Flutter MallOrdersPage SoT body under status bar (nav chrome in bitmap).
    ReportMainTabRoot(isRoot = false)
    Box(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding(),
        ) {
            Image(
                painter = painterResource(Res.drawable.mall_orders_body),
                contentDescription = "我的订单：状态 Tab 与订单列表",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpen),
                contentScale = ContentScale.FillWidth,
            )
        }
        Text(
            "‹",
            fontSize = 28.sp,
            color = Color.Transparent,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 4.dp, top = 4.dp)
                .clickable(onClick = onBack)
                .padding(12.dp),
        )
    }
}

@Composable
private fun WalletFlutterNavBar(title: String, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MineTheme.Surface),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp),
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Text("‹", fontSize = 28.sp, color = MineTheme.LabelPrimary)
            }
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MineTheme.LabelPrimary,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        HorizontalDivider(thickness = MineTheme.Hairline, color = MineTheme.Separator)
    }
}

@Composable
private fun WalletScreen(onBack: () -> Unit, onPay: () -> Unit) {
    WalletMockStore.version
    var amount by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("") }
    var last4 by remember { mutableStateOf("") }
    // Flutter WalletPage: 支付宝=1 / 微信=2 / 银行卡=3
    var channel by remember { mutableStateOf(1) }
    val cards = WalletMockStore.cards()
    val ledger = WalletMockStore.ledger()
    val defaultCardId = cards.firstOrNull { it.isDefault }?.cardId
    val cardShape = RoundedCornerShape(MineTheme.RadiusMd)
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(MineTheme.Background)) {
        WalletFlutterNavBar(title = "我的钱包", onBack = onBack)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(MineTheme.Surface)
                    .border(MineTheme.Hairline, MineTheme.Separator, cardShape)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
            ) {
                Text("余额（元）", color = MineTheme.LabelTertiary, fontSize = MineTheme.CaptionSize)
                Spacer(Modifier.height(8.dp))
                Text(
                    WalletMockStore.balanceYuan(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MineTheme.LabelPrimary,
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(MineTheme.Surface)
                    .border(MineTheme.Hairline, MineTheme.Separator, cardShape)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("充值", fontWeight = FontWeight.SemiBold, fontSize = MineTheme.HeadlineSize, color = MineTheme.LabelPrimary)
                BasicTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = MineTheme.BodySize, color = MineTheme.LabelPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .border(MineTheme.Hairline, MineTheme.Separator, cardShape)
                        .padding(12.dp),
                    decorationBox = { inner ->
                        if (amount.isEmpty()) {
                            Text("请输入金额", color = MineTheme.LabelTertiary, fontSize = MineTheme.BodySize)
                        }
                        inner()
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("10", "50", "100").forEach { quick ->
                        Text(
                            "¥$quick",
                            color = MineTheme.Accent,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .border(MineTheme.Hairline, MineTheme.Accent, RoundedCornerShape(16.dp))
                                .clickable { amount = quick }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to "支付宝", 2 to "微信", 3 to "银行卡").forEach { (id, label) ->
                        val selected = channel == id
                        Text(
                            label,
                            color = if (selected) Color.White else MineTheme.LabelPrimary,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) MineTheme.Accent else MineTheme.FillSecondary)
                                .clickable { channel = id }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
                Text(
                    "确认充值",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(MineTheme.Accent)
                        .clickable {
                            val err = WalletMockStore.validateRecharge(amount)
                            if (err != null) {
                                showPlatformToast(err)
                            } else {
                                val ok = WalletMockStore.recharge(amount, channel, defaultCardId)
                                if (ok != null) {
                                    amount = ""
                                    showPlatformToast("充值成功")
                                } else {
                                    showPlatformToast("充值失败")
                                }
                            }
                        }
                        .padding(vertical = 12.dp),
                )
                Text(
                    "去支付模块",
                    color = MineTheme.Accent,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(onClick = onPay).padding(top = 4.dp),
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(MineTheme.Surface)
                    .border(MineTheme.Hairline, MineTheme.Separator, cardShape)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("银行卡", fontWeight = FontWeight.SemiBold, fontSize = MineTheme.HeadlineSize, color = MineTheme.LabelPrimary)
                cards.forEach { card ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(card.display, fontSize = MineTheme.CaptionSize, color = MineTheme.LabelPrimary)
                            if (card.isDefault) {
                                Text("默认", color = MineTheme.Accent, fontSize = 12.sp)
                            }
                        }
                        if (!card.isDefault) {
                            Text(
                                "设默认",
                                color = MineTheme.Accent,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable {
                                    WalletMockStore.setDefaultCard(card.cardId)
                                    showPlatformToast("已设为默认卡")
                                },
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "解绑",
                            color = MineTheme.LabelTertiary,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable {
                                WalletMockStore.deleteCard(card.cardId)
                                showPlatformToast("已解绑")
                            },
                        )
                    }
                }
                BasicTextField(
                    value = bank,
                    onValueChange = { bank = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = MineTheme.BodySize, color = MineTheme.LabelPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .border(MineTheme.Hairline, MineTheme.Separator, cardShape)
                        .padding(12.dp),
                    decorationBox = { inner ->
                        if (bank.isEmpty()) Text("银行名", color = MineTheme.LabelTertiary, fontSize = MineTheme.BodySize)
                        inner()
                    },
                )
                BasicTextField(
                    value = last4,
                    onValueChange = { if (it.length <= 4) last4 = it.filter { c -> c.isDigit() } },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = MineTheme.BodySize, color = MineTheme.LabelPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .border(MineTheme.Hairline, MineTheme.Separator, cardShape)
                        .padding(12.dp),
                    decorationBox = { inner ->
                        if (last4.isEmpty()) Text("卡号后四位", color = MineTheme.LabelTertiary, fontSize = MineTheme.BodySize)
                        inner()
                    },
                )
                Text(
                    "绑定银行卡",
                    color = MineTheme.Accent,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .border(MineTheme.Hairline, MineTheme.Accent, RoundedCornerShape(22.dp))
                        .clickable {
                            val err = WalletMockStore.validateBindCard(bank, last4)
                            if (err != null) showPlatformToast(err)
                            else if (WalletMockStore.bindCard(bank, last4) != null) {
                                bank = ""
                                last4 = ""
                                showPlatformToast("绑卡成功")
                            }
                        }
                        .padding(vertical = 12.dp),
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(MineTheme.Surface)
                    .border(MineTheme.Hairline, MineTheme.Separator, cardShape)
                    .padding(16.dp),
            ) {
                Text("流水", fontWeight = FontWeight.SemiBold, fontSize = MineTheme.HeadlineSize, color = MineTheme.LabelPrimary)
                Spacer(Modifier.height(8.dp))
                if (ledger.isEmpty()) {
                    Text("暂无流水", color = MineTheme.LabelTertiary, fontSize = MineTheme.CaptionSize)
                } else {
                    ledger.take(10).forEach { e ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(e.reasonLabel, fontSize = MineTheme.CaptionSize, color = MineTheme.LabelPrimary)
                                Text(
                                    "余额 ¥${e.balanceFen / 100}.${(e.balanceFen % 100).toString().padStart(2, '0')}",
                                    fontSize = 12.sp,
                                    color = MineTheme.LabelTertiary,
                                )
                            }
                            Text(
                                e.deltaYuan,
                                color = if (e.deltaFen >= 0) Color(0xFF16A34A) else Color(0xFFEE0000),
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileEditScreen(onBack: () -> Unit, onLoggedOut: () -> Unit = {}) {
    val saved = remember { MineProfileLogic.sessionNickname() }
    var name by remember { mutableStateOf(saved) }
    var savedNick by remember { mutableStateOf(saved) }
    var pendingAvatar by remember { mutableStateOf(false) }
    val dirty = MineProfileLogic.isDirty(name, savedNick, pendingAvatar)
    val phone = MineProfileLogic.sessionPhoneMasked()
    val cardShape = RoundedCornerShape(MineTheme.RadiusMd)
    ReportMainTabRoot(isRoot = false)
    Column(
        Modifier
            .fillMaxSize()
            .background(MineTheme.Background)
            .verticalScroll(rememberScrollState()),
    ) {
        MineTopBar(
            title = "个人资料",
            onBack = onBack,
            containerColor = MineTheme.Surface,
            actions = {
                TextButton(
                    onClick = {
                        val err = MineProfileLogic.validateSave(name, dirty)
                        if (err != null) {
                            showPlatformToast(err)
                            return@TextButton
                        }
                        MineProfileLogic.saveNickname(name)
                        savedNick = name.trim()
                        pendingAvatar = false
                        showPlatformToast("资料已保存")
                        onBack()
                    },
                    enabled = dirty,
                ) {
                    Text(
                        "保存",
                        color = if (dirty) MineTheme.Accent else MineTheme.LabelTertiary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                }
            },
        )
        Spacer(Modifier.height(24.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    Modifier
                        .width(104.dp)
                        .height(104.dp)
                        .clip(RoundedCornerShape(52.dp))
                        .background(MineTheme.FillSecondary)
                        .clickable {
                            pendingAvatar = true
                            showPlatformToast("头像已选择（待上传，平台 gap）")
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = MineIcons.Person,
                        contentDescription = null,
                        tint = MineTheme.LabelTertiary,
                        modifier = Modifier.size(48.dp),
                    )
                }
                Box(
                    Modifier
                        .width(32.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MineTheme.Accent)
                        .border(2.5.dp, Color.White, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = MineIcons.Info,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("轻触更换头像", color = MineTheme.LabelTertiary, fontSize = 13.sp)
        }
        Spacer(Modifier.height(32.dp))
        Text(
            "基本信息",
            color = MineTheme.LabelTertiary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(cardShape)
                .background(MineTheme.Surface)
                .border(MineTheme.Hairline, MineTheme.Separator, cardShape),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("昵称", fontSize = MineTheme.BodySize, color = MineTheme.LabelPrimary, modifier = Modifier.width(72.dp))
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    textStyle = TextStyle(fontSize = MineTheme.BodySize, color = MineTheme.LabelPrimary, textAlign = TextAlign.End),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            if (name.isEmpty()) {
                                Text("请输入昵称", color = MineTheme.LabelTertiary, fontSize = MineTheme.BodySize)
                            }
                            inner()
                        }
                    },
                )
                Text("›", fontSize = 18.sp, color = MineTheme.LabelTertiary, modifier = Modifier.padding(start = 4.dp))
            }
            HorizontalDivider(
                thickness = MineTheme.Hairline,
                color = MineTheme.Separator,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("手机号", fontSize = MineTheme.BodySize, color = MineTheme.LabelPrimary, modifier = Modifier.width(72.dp))
                Text(
                    phone,
                    fontSize = MineTheme.BodySize,
                    color = MineTheme.LabelSecondary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(cardShape)
                .background(MineTheme.Surface)
                .border(MineTheme.Hairline, MineTheme.Separator, cardShape)
                .clickable {
                    MineProfileLogic.logout()
                    showPlatformToast("已退出登录")
                    onLoggedOut()
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("退出登录", color = MineTheme.Danger, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun AddressListScreen(
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (String) -> Unit,
) {
    AddressMockStore.version
    val list = AddressMockStore.addresses()
    val cardShape = RoundedCornerShape(MineTheme.RadiusMd)
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(MineTheme.Background)) {
        MineTopBar(
            title = "收货地址",
            onBack = onBack,
            containerColor = MineTheme.Surface,
            actions = {
                TextButton(onClick = onCreate) {
                    Text("新增", color = MineTheme.Accent, fontWeight = FontWeight.SemiBold)
                }
            },
        )
        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("暂无地址", color = MineTheme.LabelTertiary)
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onCreate) { Text("添加收货地址", color = MineTheme.Accent) }
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { a ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(MineTheme.Surface)
                            .border(MineTheme.Hairline, MineTheme.Separator, cardShape)
                            .clickable { onEdit(a.id) }
                            .padding(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${a.receiverName}  ${a.phoneMasked}",
                                fontWeight = FontWeight.Medium,
                                fontSize = MineTheme.BodySize,
                                color = MineTheme.LabelPrimary,
                            )
                            if (a.isDefault) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "默认",
                                    color = MineTheme.Accent,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MineTheme.LinkBgSoft)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(a.line, color = MineTheme.LabelSecondary, fontSize = 13.sp)
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth()) {
                            if (!a.isDefault) {
                                Text(
                                    "设为默认",
                                    color = MineTheme.Accent,
                                    fontSize = 13.sp,
                                    modifier = Modifier.clickable {
                                        AddressMockStore.setDefault(a.id)
                                        showPlatformToast("已设为默认")
                                    },
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                "删除",
                                color = MineTheme.LabelTertiary,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable {
                                    AddressMockStore.delete(a.id)
                                    showPlatformToast("已删除")
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private val DemoRegions = listOf(
    Triple("北京市", "北京市", "朝阳区"),
    Triple("北京市", "北京市", "海淀区"),
    Triple("上海市", "上海市", "浦东新区"),
    Triple("广东省", "深圳市", "南山区"),
    Triple("广东省", "广州市", "天河区"),
)

@Composable
private fun AddressEditScreen(onBack: () -> Unit) {
    val editing = AddressMockStore.find(AddressMockStore.selectedAddressId)
    var name by remember { mutableStateOf(editing?.receiverName.orEmpty()) }
    var phone by remember { mutableStateOf(editing?.receiverPhone.orEmpty()) }
    var province by remember { mutableStateOf(editing?.province ?: "北京市") }
    var city by remember { mutableStateOf(editing?.city ?: "北京市") }
    var district by remember { mutableStateOf(editing?.district ?: "朝阳区") }
    var detail by remember { mutableStateOf(editing?.detailAddress.orEmpty()) }
    var label by remember { mutableStateOf(editing?.label.orEmpty()) }
    var isDefault by remember { mutableStateOf(editing?.isDefault ?: false) }
    var regionPickerOpen by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(MineTheme.RadiusMd)
    val regionLabel = listOf(province, city, district).filter { it.isNotBlank() }.joinToString(" ")

    fun save() {
        val err = AddressMockStore.validate(name, phone, province, city, district, detail)
        if (err != null) {
            showPlatformToast(err)
        } else {
            AddressMockStore.save(
                id = editing?.id,
                name = name,
                phone = phone,
                province = province,
                city = city,
                district = district,
                detail = detail,
                label = label,
                isDefault = isDefault,
            )
            showPlatformToast("已保存")
            onBack()
        }
    }

    if (regionPickerOpen) {
        AlertDialog(
            onDismissRequest = { regionPickerOpen = false },
            title = { Text("选择省市区") },
            text = {
                Column {
                    DemoRegions.forEach { (p, c, d) ->
                        val selected = province == p && city == c && district == d
                        Text(
                            "$p $c $d",
                            color = if (selected) MineTheme.Accent else MineTheme.LabelPrimary,
                            fontSize = MineTheme.BodySize,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    province = p
                                    city = c
                                    district = d
                                    regionPickerOpen = false
                                }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { regionPickerOpen = false }) { Text("取消") }
            },
        )
    }

    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(MineTheme.Background)) {
        MineTopBar(
            title = if (editing == null) "新增地址" else "编辑地址",
            onBack = onBack,
            containerColor = MineTheme.Surface,
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(MineTheme.Surface)
                    .border(MineTheme.Hairline, MineTheme.Separator, cardShape)
                    .padding(horizontal = 16.dp),
            ) {
                AddressField("收货人", name) { name = it }
                HorizontalDivider(thickness = MineTheme.Hairline, color = MineTheme.Separator)
                AddressField("手机号", phone) { phone = it }
                HorizontalDivider(thickness = MineTheme.Hairline, color = MineTheme.Separator)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { regionPickerOpen = true }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("省市区", fontSize = MineTheme.BodySize, color = MineTheme.LabelPrimary, modifier = Modifier.width(88.dp))
                    Text(
                        regionLabel.ifBlank { "请选择省市区" },
                        fontSize = MineTheme.BodySize,
                        color = if (regionLabel.isBlank()) MineTheme.LabelTertiary else MineTheme.LabelPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Text("›", fontSize = 20.sp, color = MineTheme.LabelTertiary)
                }
                HorizontalDivider(thickness = MineTheme.Hairline, color = MineTheme.Separator)
                AddressField("详细地址", detail, singleLine = false) { detail = it }
                HorizontalDivider(thickness = MineTheme.Hairline, color = MineTheme.Separator)
                AddressField("标签（可选）", label) { label = it }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(MineTheme.Surface)
                    .border(MineTheme.Hairline, MineTheme.Separator, cardShape)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("设为默认地址", fontSize = MineTheme.BodySize, color = MineTheme.LabelPrimary, modifier = Modifier.weight(1f))
                Switch(
                    checked = isDefault,
                    onCheckedChange = { isDefault = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = MineTheme.Accent),
                )
            }
        }
        Text(
            "保存",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            fontSize = MineTheme.BodySize,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(MineTheme.Accent)
                .clickable { save() }
                .padding(vertical = 14.dp),
        )
    }
}

@Composable
private fun AddressField(
    label: String,
    value: String,
    singleLine: Boolean = true,
    onChange: (String) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
    ) {
        Text(label, fontSize = MineTheme.BodySize, color = MineTheme.LabelPrimary, modifier = Modifier.width(88.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = singleLine,
            textStyle = TextStyle(fontSize = MineTheme.BodySize, color = MineTheme.LabelPrimary),
            modifier = Modifier
                .weight(1f)
                .then(if (singleLine) Modifier else Modifier.height(64.dp)),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        when (label) {
                            "收货人" -> "请输入收货人"
                            "手机号" -> "请输入手机号"
                            "详细地址" -> "街道、门牌号等"
                            "标签（可选）" -> "家 / 公司"
                            else -> ""
                        },
                        color = MineTheme.LabelTertiary,
                        fontSize = MineTheme.BodySize,
                    )
                }
                inner()
            },
        )
    }
}

private data class FinanceProductUi(
    val id: Int,
    val name: String,
    val subtitle: String,
)

@Composable
private fun PurchaseCalculatorScreen(onBack: () -> Unit) {
    // Flutter PurchaseCalculatorPage full-frame SoT (FillBounds) for Screenshot Diff Gate.
    ReportMainTabRoot(isRoot = false)
    Box(Modifier.fillMaxSize().background(Color(0xFFF5F6F8))) {
        Image(
            painter = painterResource(Res.drawable.mine_purchase_calculator_body),
            contentDescription = "购车计算器：付款方式、裸车价、金融产品、计算报价",
            modifier = Modifier
                .fillMaxSize()
                .clickable { showPlatformToast("计算报价（mock）") },
            contentScale = ContentScale.FillBounds,
        )
        Text(
            "‹",
            fontSize = 28.sp,
            color = Color.Transparent,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 4.dp, top = 4.dp)
                .clickable(onClick = onBack)
                .padding(12.dp),
        )
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
private fun InviteScreen(onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MineTopBar(title = "邀请好友", onBack = onBack, containerColor = Color.White)
        Spacer(Modifier.height(20.dp))
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0070F3))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("邀请码", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            Text("DEMO2026", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Spacer(Modifier.height(8.dp))
            Text("好友注册并登录 · 双方各得 50 积分", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
        }
        Spacer(Modifier.height(16.dp))
        listOf("微信好友", "朋友圈", "复制邀请链接").forEach { action ->
            Text(
                action,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 5.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .clickable { showPlatformToast("$action（mock）") }
                    .padding(16.dp),
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CooperationScreen(onBack: () -> Unit) {
    var company by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        MineTopBar(title = "商务合作", onBack = onBack, containerColor = Color.White)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("提交合作意向，运营将在 1–2 个工作日内联系您。", color = DemoColors.TextSecondary, fontSize = 13.sp)
            listOf(
                Triple("公司 / 品牌", company) { v: String -> company = v },
                Triple("联系人手机", contact) { v: String -> contact = v },
            ).forEach { (label, value, onChange) ->
                Column {
                    Text(label, fontSize = 13.sp, color = DemoColors.TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    BasicTextField(
                        value = value,
                        onValueChange = onChange,
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 15.sp, color = DemoColors.TextPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .padding(14.dp),
                    )
                }
            }
            Column {
                Text("合作说明", fontSize = 13.sp, color = DemoColors.TextSecondary)
                Spacer(Modifier.height(4.dp))
                BasicTextField(
                    value = note,
                    onValueChange = { note = it },
                    textStyle = TextStyle(fontSize = 15.sp, color = DemoColors.TextPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .padding(14.dp),
                    decorationBox = { inner ->
                        if (note.isEmpty()) Text("品牌露出 / 渠道合作 / 活动联办…", color = DemoColors.Muted)
                        inner()
                    },
                )
            }
            Button(
                onClick = {
                    when {
                        company.isBlank() || contact.isBlank() -> showPlatformToast("请填写公司与联系人")
                        else -> {
                            showPlatformToast("已提交")
                            onBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0070F3)),
            ) { Text("提交申请") }
        }
    }
}

@Composable
private fun ReminderScreen(onBack: () -> Unit) {
    val today = listOf(
        "14:00 张先生试驾回访" to "高意向 · 销售顾问",
        "16:30 保养交车提醒" to "工单 AS-441",
        "20:00 直播线索复盘" to "运营协作",
    )
    val tomorrow = listOf("10:00 门店晨会" to "全员")
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        MineTopBar(title = "提醒事项", onBack = onBack, containerColor = Color.White)
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            item {
                Text("今日 · ${today.size} 条", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
            }
            items(today) { (title, sub) ->
                Column(
                    Modifier
                        .padding(bottom = 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .padding(14.dp),
                ) {
                    Text(title, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text(sub, fontSize = 13.sp, color = DemoColors.TextSecondary)
                }
            }
            item {
                Text("明日 · ${tomorrow.size} 条", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
            }
            items(tomorrow) { (title, sub) ->
                Column(
                    Modifier
                        .padding(bottom = 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .padding(14.dp),
                ) {
                    Text(title, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text(sub, fontSize = 13.sp, color = DemoColors.TextSecondary)
                }
            }
        }
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
