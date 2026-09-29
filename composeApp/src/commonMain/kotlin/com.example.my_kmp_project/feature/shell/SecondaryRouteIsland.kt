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
import com.example.my_kmp_project.core.router.ProductRouteDispatch
import com.example.my_kmp_project.core.router.ProductRouteHost
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
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
        val target = ProductRouteDispatch.resolve(titleOrPath) ?: return null
        if (target.host == ProductRouteHost.Toast) return null
        if (target.host != ProductRouteHost.Mine && target.host != ProductRouteHost.Unknown) {
            // Non-Mine labels resolve elsewhere; island only keeps Mine paths.
            return if (ProductRouteDispatch.hostForPath(target.path) == ProductRouteHost.Mine) {
                target.path
            } else {
                null
            }
        }
        return when (target.host) {
            ProductRouteHost.Mine -> target.path
            ProductRouteHost.Unknown -> {
                val raw = titleOrPath.trim()
                if (raw.startsWith("/")) MineRoutes.canonicalize(raw) else null
            }
            else -> null
        }
    }

    /** Mine Compose Island ownership (settings / profile / addresses / membership / mall children). */
    fun isMineIslandOwned(route: String): Boolean =
        ProductRouteDispatch.hostForPath(route) == ProductRouteHost.Mine
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
