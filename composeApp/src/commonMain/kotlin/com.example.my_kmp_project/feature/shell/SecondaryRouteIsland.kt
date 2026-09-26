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
import com.example.my_kmp_project.component.webview.OfflineWebFixtureUrl
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.router.AppRoutePath
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import com.example.my_kmp_project.feature.chat.ChatScreen
import com.example.my_kmp_project.feature.community.CommunityRouteHost
import com.example.my_kmp_project.feature.community.CommunityRoutes
import com.example.my_kmp_project.feature.community.CommunityScreen
import com.example.my_kmp_project.feature.content.ContentRouteHost
import com.example.my_kmp_project.feature.content.ContentRoutes
import com.example.my_kmp_project.feature.home.AllServicesScreen
import com.example.my_kmp_project.feature.home.HomeRouteHost
import com.example.my_kmp_project.feature.home.HomeRoutes
import com.example.my_kmp_project.feature.mine.MineRouteHost
import com.example.my_kmp_project.feature.mine.MineRoutes
import com.example.my_kmp_project.feature.scan.ScanScreen
import com.example.my_kmp_project.feature.web.InAppWebScreen

/**
 * Shared secondary-route island for iOS / OHOS native shells (ADR 0002).
 * Resolves Flutter RoutePath strings or Chinese labels used by tab roots.
 */
object SecondaryRouteResolver {
    fun resolve(titleOrPath: String): String? {
        val raw = titleOrPath.trim()
        if (raw.isEmpty()) return null
        if (raw.startsWith("/")) return MineRoutes.canonicalize(raw)
        if (raw.startsWith("http://") || raw.startsWith("https://")) return AppRoutePath.web
        when (raw) {
            "全部服务", "更多" -> return AppRoutePath.homeAllServices
            "扫一扫" -> return "/scan"
            "H5 调试", "内嵌网页" -> return AppRoutePath.web
            "消息" -> return AppRoutePath.chat
            "电子名片" -> return MineRoutes.Invite
            "商务合作" -> return MineRoutes.Cooperation
            "好友", "切换门店" -> return MineRoutes.Invite
            "粉丝群" -> return MineRoutes.FanGroup
            "帮助中心" -> return MineRoutes.Feedback
            "订单中心" -> return MineRoutes.MallOrders
            "会员续费" -> return MineRoutes.Membership
        }
        HomeRoutes.fromLabel(raw)?.let { return it }
        CommunityRoutes.fromLabel(raw)?.let { return it }
        MineRoutes.fromLabel(raw)?.let { return it }
        ContentRoutes.fromLabel(raw)?.let { return it }
        return null
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
        stack = stack + resolved
    }

    fun pop() {
        if (stack.size <= 1) {
            onRequestClose()
        } else {
            stack = stack.dropLast(1)
        }
    }

    when {
        route == AppRoutePath.homeAllServices || route == "/home/all_services" ->
            AllServicesScreen(onBack = ::pop, onOpen = ::navigate)
        route == "/scan" ->
            ScanScreen(
                onBack = ::pop,
                onScanResult = { payload ->
                    if (payload.startsWith("http")) navigate(payload)
                },
            )
        route == AppRoutePath.web || route.startsWith("http://") || route.startsWith("https://") ->
            InAppWebScreen(
                url = if (route.startsWith("http")) route else OfflineWebFixtureUrl,
                onBack = ::pop,
            )
        route == AppRoutePath.chat || route == AppRoutePath.chatDetail ->
            ChatScreen()
        route == AppRoutePath.community || route == "/community" ->
            CommunityScreen()
        route.startsWith("/home/") ->
            HomeRouteHost(route = route, onBack = ::pop, onNavigate = ::navigate)
        route.startsWith("/community/") ->
            CommunityRouteHost(route = route, onBack = ::pop, onNavigate = ::navigate)
        route.startsWith("/mine") ||
            route.startsWith("/mall") ||
            route.startsWith("/wallet") ||
            route.startsWith("/pay") ||
            route == AppRoutePath.settings ||
            route.startsWith("/settings") ->
            MineRouteHost(route = route, onBack = ::pop, onNavigate = ::navigate)
        route.startsWith("/video") ||
            route.startsWith("/classroom") ||
            route.startsWith("/live") ||
            route.startsWith("/friend") ||
            route.startsWith("/music") ||
            route.startsWith("/ai") ||
            route.startsWith("/media") ->
            ContentRouteHost(route = route, onBack = ::pop, onNavigate = ::navigate)
        else ->
            UnresolvedSecondaryRoute(route = route, onBack = ::pop)
    }
}

@Composable
private fun UnresolvedSecondaryRoute(route: String, onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "未映射入口", onBack = onBack)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "route=$route",
                color = DemoColors.TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
