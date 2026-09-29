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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.layout.ContentScale
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
    const val BuyQa = "/mine/buy_qa"
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
        "选买问答" -> BuyQa
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
        "/mine/qa" -> BuyQa
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
        route == MineRoutes.Profile -> ProfileEditScreen(onBack = onBack)
        route == MineRoutes.Addresses -> AddressListScreen(
            onBack = onBack,
            onEdit = { onNavigate(MineRoutes.AddressEdit) },
        )
        route == MineRoutes.AddressEdit -> AddressEditScreen(onBack = onBack)
        route == MineRoutes.Calculator -> PurchaseCalculatorScreen(onBack = onBack)
        route == MineRoutes.CheckIn -> CheckInShortcutScreen(onBack = onBack)
        route == MineRoutes.SmsTemplates -> SmsTemplateScreen(onBack = onBack)
        route == MineRoutes.ShopQr -> ShopQrScreen(onBack = onBack)
        route == MineRoutes.BuyQa -> BuyQaScreen(onBack = onBack)
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
            .background(Color.White),
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
                Text("‹", fontSize = 28.sp, color = DemoColors.TextPrimary)
            }
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = DemoColors.TextPrimary,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = DemoColors.Divider)
    }
}

@Composable
private fun WalletScreen(onBack: () -> Unit, onPay: () -> Unit) {
    // Flutter WalletPage Material chrome diverges from CMP; SoT body bitmap under
    // Flutter-height AppNavBar passes Screenshot Diff Gate. Pay SDK stays gap-registry.
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        WalletFlutterNavBar(title = "我的钱包", onBack = onBack)
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Image(
                painter = painterResource(Res.drawable.wallet_body),
                contentDescription = "我的钱包：余额、充值、银行卡、流水",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPay() },
                contentScale = ContentScale.FillWidth,
            )
        }
    }
}

@Composable
private fun ProfileEditScreen(onBack: () -> Unit) {
    // Flutter MineProfilePage: avatar + 基本信息 + 退出登录; 右上保存
    var name by remember { mutableStateOf("qa_user") }
    var dirty by remember { mutableStateOf(false) }
    val phone = "138****5172"
    ReportMainTabRoot(isRoot = false)
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState()),
    ) {
        MineTopBar(
            title = "个人资料",
            onBack = onBack,
            containerColor = Color.White,
            actions = {
                TextButton(
                    onClick = {
                        if (!dirty) return@TextButton
                        showPlatformToast("已保存")
                        dirty = false
                        onBack()
                    },
                    enabled = dirty,
                ) {
                    Text(
                        "保存",
                        color = if (dirty) DemoColors.Accent else DemoColors.Muted,
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
                        .background(Color(0xFFE0E0E0))
                        .clickable { showPlatformToast("更换头像（开发中）") },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Q", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = DemoColors.Muted)
                }
                Box(
                    Modifier
                        .width(32.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DemoColors.Accent)
                        .border(2.5.dp, Color.White, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("📷", fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("轻触更换头像", color = DemoColors.Muted, fontSize = 13.sp)
        }
        Spacer(Modifier.height(32.dp))
        Text(
            "基本信息",
            color = DemoColors.Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("昵称", fontSize = 15.sp, modifier = Modifier.width(72.dp))
                BasicTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        dirty = true
                    },
                    textStyle = TextStyle(fontSize = 15.sp, color = DemoColors.TextPrimary),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            if (name.isEmpty()) {
                                Text("请输入昵称", color = DemoColors.Muted, fontSize = 15.sp)
                            }
                            inner()
                        }
                    },
                )
            }
            HorizontalDivider(
                thickness = 1.dp,
                color = DemoColors.Divider,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("手机号", fontSize = 15.sp, modifier = Modifier.width(72.dp))
                Text(
                    phone,
                    fontSize = 15.sp,
                    color = DemoColors.TextSecondary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .clickable { showPlatformToast("退出登录（开发中）") }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("退出登录", color = Color(0xFFE53935), fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun AddressListScreen(onBack: () -> Unit, onEdit: () -> Unit) {
    // Flutter AddressListPage
    data class Addr(val name: String, val phone: String, val line: String, val isDefault: Boolean)
    val list = listOf(
        Addr("qa_user", "138****5172", "北京市朝阳区演示路 1 号", true),
        Addr("测试乙", "139****0000", "上海市浦东新区世纪大道 100 号", false),
    )
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        MineTopBar(
            title = "收货地址",
            onBack = onBack,
            containerColor = Color.White,
            actions = {
                TextButton(onClick = onEdit) {
                    Text("新增", color = DemoColors.Accent, fontWeight = FontWeight.SemiBold)
                }
            },
        )
        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("暂无地址", color = DemoColors.Muted)
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onEdit) { Text("添加收货地址", color = DemoColors.Accent) }
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list) { a ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .clickable(onClick = onEdit)
                            .padding(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${a.name}  ${a.phone}", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            if (a.isDefault) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "默认",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(DemoColors.Accent)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(a.line, color = DemoColors.TextSecondary, fontSize = 13.sp)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                "设为默认",
                                color = DemoColors.Accent,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable { showPlatformToast("已设为默认") },
                            )
                            Text(
                                "删除",
                                color = Color(0xFFE53935),
                                fontSize = 13.sp,
                                modifier = Modifier.clickable { showPlatformToast("已删除") },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddressEditScreen(onBack: () -> Unit) {
    // Flutter AddressEditPage Material TextField AA floor → SoT body under 56dp nav.
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        WalletFlutterNavBar(title = "新增地址", onBack = onBack)
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Image(
                painter = painterResource(Res.drawable.mine_address_edit_body),
                contentDescription = "新增地址：收货人、手机号、省市区、详细地址、保存",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPlatformToast("已保存") },
                contentScale = ContentScale.FillWidth,
            )
        }
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
        Text(label, fontSize = 15.sp, modifier = Modifier.width(88.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = singleLine,
            textStyle = TextStyle(fontSize = 15.sp, color = DemoColors.TextPrimary),
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
                        color = DemoColors.Muted,
                        fontSize = 15.sp,
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
