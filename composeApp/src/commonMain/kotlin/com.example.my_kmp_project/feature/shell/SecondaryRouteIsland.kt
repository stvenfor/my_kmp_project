package com.example.my_kmp_project.feature.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.router.AppRoutePath
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import com.example.my_kmp_project.feature.community.CommunityRoutes
import com.example.my_kmp_project.feature.content.ContentRoutes
import com.example.my_kmp_project.feature.home.HomeRoutes
import com.example.my_kmp_project.feature.mine.MineRouteHost
import com.example.my_kmp_project.feature.mine.MineRoutes

/**
 * Mine-only Compose secondary host (ADR 0002 / #23).
 *
 * In-scope **non-Mine** routes must open via Native Shell UI
 * (`NativeAndroidMain` / `NativeFeatureHost` / ArkTS `nativePane`) — never here.
 * Android main path does not use this host at all.
 */
object SecondaryRouteResolver {
    fun resolve(titleOrPath: String): String? {
        val raw = titleOrPath.trim()
        if (raw.isEmpty()) return null
        val toastOnlyPaths = setOf(
            "/mine/business_card",
            "/mine/invite",
            "/mine/business",
            "/mine/reminders",
            "/mine/reminder",
            "/mine/fan_group",
            "/mine/feedback",
        )
        if (raw.startsWith("/")) {
            val canon = MineRoutes.canonicalize(raw)
            if (canon in toastOnlyPaths) return null
            return canon
        }
        when (raw) {
            "电子名片", "商务合作", "好友", "粉丝群", "帮助中心",
            "意见反馈", "提醒事项", "邀请好友", "头像", "请先登录",
            "切换门店", "切换店铺", "个人资料",
            "全部服务", "更多", "扫一扫", "H5 调试", "内嵌网页", "消息",
            -> return null
            "订单中心" -> return MineRoutes.MallOrders
            "会员续费" -> return MineRoutes.Membership
        }
        MineRoutes.fromLabel(raw)?.let { return it }
        // Non-Mine labels resolve for shell routers, but must not open this island.
        HomeRoutes.fromLabel(raw)?.let { return it }
        CommunityRoutes.fromLabel(raw)?.let { return it }
        ContentRoutes.fromLabel(raw)?.let { return it }
        return null
    }

    /** Mine Compose Island ownership (settings / profile / addresses / membership / mall children). */
    fun isMineIslandOwned(route: String): Boolean {
        val r = MineRoutes.canonicalize(route.trim())
        if (r.startsWith("/mine") || r.startsWith("/settings") || r == AppRoutePath.settings) return true
        if (r == "/pay/membership" || r.startsWith("/pay/membership")) return true
        // Commerce children reachable from MineRouteHost stack (not primary native hosts).
        if (r.startsWith("/mall") || r.startsWith("/wallet") || r.startsWith("/pay")) return true
        return false
    }
}

@Composable
fun SecondaryRouteIsland(
    initialRoute: String,
    onRequestClose: () -> Unit,
) {
    var stack by remember(initialRoute) {
        mutableStateOf(listOf(SecondaryRouteResolver.resolve(initialRoute) ?: initialRoute))
    }
    val route = stack.last()

    fun navigate(next: String) {
        val resolved = SecondaryRouteResolver.resolve(next) ?: next
        if (!SecondaryRouteResolver.isMineIslandOwned(resolved)) {
            // Refuse non-Mine push — native shell owns those paths (#23).
            return
        }
        stack = stack + resolved
    }

    fun pop() {
        if (stack.size <= 1) {
            onRequestClose()
        } else {
            stack = stack.dropLast(1)
        }
    }

    if (!SecondaryRouteResolver.isMineIslandOwned(route)) {
        LegacyNonMineBlocked(route = route, onBack = ::pop)
        return
    }

    MineRouteHost(route = route, onBack = ::pop, onNavigate = ::navigate)
}

@Composable
private fun LegacyNonMineBlocked(route: String, onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "请从原生壳打开", onBack = onBack)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "非 Mine 路由已迁出 Legacy 岛 (#23)\nroute=$route",
                color = DemoColors.TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
