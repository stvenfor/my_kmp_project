package com.example.my_kmp_project.feature.mine

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import com.example.my_kmp_project.core.router.MineRoutes

@Composable
internal fun UnmappedMineRoute(route: String, onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "未映射入口", onBack = onBack)
        Text("route=$route（非宣称完成路由）", modifier = Modifier.padding(16.dp), color = DemoColors.TextPrimary)
    }
}

@Composable
internal fun PayCheckoutScreen(onBack: () -> Unit) {
    // Flutter /pay SoT is a module placeholder ("Pay 模块"), not a full checkout yet.
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(MineTheme.Background)) {
        MineTopBar(title = "支付", onBack = onBack, containerColor = MineTheme.Surface)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Pay 模块", color = DemoColors.TextSecondary, fontSize = 16.sp)
        }
    }
}

@Composable
internal fun CheckInShortcutScreen(onBack: () -> Unit) {
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
                .background(MineTheme.Surface)
                .padding(16.dp),
        ) {
            Text("当前积分", color = DemoColors.TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text("$points", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(
                "今日签到",
                color = DemoColors.OnPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(MineTheme.RadiusMd))
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
                    .clip(RoundedCornerShape(MineTheme.RadiusMd))
                    .background(MineTheme.Surface)
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
