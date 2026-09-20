package com.example.my_kmp_project

import androidx.compose.ui.window.ComposeUIViewController
import com.example.my_kmp_project.core.network.platformNetworkBootstrap
import com.example.my_kmp_project.feature.mine.MineIsland
import com.example.my_kmp_project.feature.mine.MineIslandRoute
import platform.UIKit.UIViewController

/**
 * Legacy full-app Compose entry — retained for reference only (ADR 0002).
 * iOS product shell is SwiftUI; use [MineIslandViewController] for the island.
 */
@Deprecated("Native SwiftUI shell owns the app; use MineIslandViewController for Mine secondary UI")
fun MainViewController() = run {
    platformNetworkBootstrap()
    ComposeUIViewController { App() }
}

/**
 * Mine Compose Island host for SwiftUI navigation (Mine Island Hosting).
 * [route]: settings | personalized | about
 */
fun MineIslandViewController(route: String = "settings"): UIViewController {
    platformNetworkBootstrap()
    val initial = when (route.lowercase()) {
        "personalized" -> MineIslandRoute.Personalized
        "about" -> MineIslandRoute.About
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
