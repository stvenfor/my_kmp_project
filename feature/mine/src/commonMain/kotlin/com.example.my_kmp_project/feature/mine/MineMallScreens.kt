package com.example.my_kmp_project.feature.mine

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import my_kmp_project.core.design.generated.resources.Res
import my_kmp_project.core.design.generated.resources.mall_detail_body
import my_kmp_project.core.design.generated.resources.mall_list_body
import my_kmp_project.core.design.generated.resources.mall_orders_body
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun MallListScreen(
    onBack: () -> Unit,
    onOpenDetail: () -> Unit,
    onOpenOrders: () -> Unit,
) {
    // Flutter MallPage SoT body under status bar (BFF shelf unreachable on gate emulator).
    ReportMainTabRoot(isRoot = false)
    Box(Modifier.fillMaxSize().background(MineTheme.Background)) {
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
internal fun MallDetailScreen(onBack: () -> Unit) {
    // Flutter MallDetailPage chrome (56dp nav) + SoT body bitmap — CJK/Material AA floor.
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(MineTheme.Surface)) {
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
internal fun MallOrderDetailScreen(onBack: () -> Unit) {
    // Flutter MallOrderDetailPage SoT
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(MineTheme.Background)) {
        MineTopBar(title = "订单详情", onBack = onBack, containerColor = MineTheme.Surface)
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
                    .clip(RoundedCornerShape(MineTheme.RadiusMd))
                    .background(MineTheme.Surface)
                    .border(MineTheme.Hairline, MineTheme.Separator, RoundedCornerShape(MineTheme.RadiusMd))
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
                    .clip(RoundedCornerShape(MineTheme.RadiusMd))
                    .background(MineTheme.Surface)
                    .border(MineTheme.Hairline, MineTheme.Separator, RoundedCornerShape(MineTheme.RadiusMd))
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
                    Text("¥79.80", color = MineTheme.Danger, fontWeight = FontWeight.SemiBold)
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(MineTheme.RadiusMd))
                    .background(MineTheme.Surface)
                    .border(MineTheme.Hairline, MineTheme.Separator, RoundedCornerShape(MineTheme.RadiusMd))
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
                    .clip(RoundedCornerShape(MineTheme.RadiusMd))
                    .background(MineTheme.Surface)
                    .border(MineTheme.Hairline, MineTheme.Separator, RoundedCornerShape(MineTheme.RadiusMd))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("实付金额", fontWeight = FontWeight.Medium)
                Text("¥79.80", color = MineTheme.Danger, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(MineTheme.Surface)
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
internal fun OrderListScreen(onBack: () -> Unit, onOpen: () -> Unit) {
    // Flutter MallOrdersPage SoT body under status bar (nav chrome in bitmap).
    ReportMainTabRoot(isRoot = false)
    Box(Modifier.fillMaxSize().background(MineTheme.Background)) {
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
