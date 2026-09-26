package com.example.my_kmp_project

import androidx.compose.ui.window.ComposeUIViewController
import com.example.my_kmp_project.core.network.platformNetworkBootstrap
import com.example.my_kmp_project.core.router.DeepLinkRouter
import com.example.my_kmp_project.feature.mine.MineIsland
import com.example.my_kmp_project.feature.mine.MineIslandRoute
import com.example.my_kmp_project.feature.shell.SecondaryRouteIsland
import platform.UIKit.UIViewController

/**
 * Legacy full-app Compose entry — reference only (ADR 0002).
 */
@Deprecated("Native SwiftUI shell owns the app; use MineIslandViewController / SecondaryRouteViewController")
fun MainViewController() = run {
    platformNetworkBootstrap()
    ComposeUIViewController { App() }
}

/**
 * Mine Compose Island host for SwiftUI (ADR 0002 Mine Island Hosting).
 * [route]: settings | personalized | about | membership
 *
 * Secondary RoutePath Compose host remains for Android / reference only —
 * iOS/OHOS shells must NOT open it for product navigation (native stubs instead).
 */
fun MineIslandViewController(route: String = "settings"): UIViewController {
    platformNetworkBootstrap()
    val initial = when (route.lowercase()) {
        "personalized" -> MineIslandRoute.Personalized
        "about" -> MineIslandRoute.About
        "membership" -> MineIslandRoute.Membership
        else -> MineIslandRoute.Settings
    }
    lateinit var controller: UIViewController
    controller = ComposeUIViewController {
        MineIsland(
            initialRoute = initial,
            onRequestClose = {
                controller.dismissViewControllerAnimated(true, completion = null)
            },
        )
    }
    return controller
}

/**
 * Secondary RoutePath host — Home/Mine/Content/Community/Web/Scan Compose screens
 * shared with Android NativeAndroidMain overlays.
 *
 * [routeOrLabel]: Flutter path (`/home/search`) or Chinese label (`搜索`).
 */
fun SecondaryRouteViewController(routeOrLabel: String): UIViewController {
    platformNetworkBootstrap()
    lateinit var controller: UIViewController
    controller = ComposeUIViewController {
        SecondaryRouteIsland(
            initialRoute = routeOrLabel,
            onRequestClose = {
                controller.dismissViewControllerAnimated(true, completion = null)
            },
        )
    }
    return controller
}

/** Accept `myai://…` / `xiaomao://…` from SwiftUI and return the pending route string if any. */
fun AcceptDeepLinkFromIos(uri: String): String? {
    val parsed = DeepLinkRouter.accept(uri) ?: return null
    return parsed.route
}
