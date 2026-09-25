package com.example.my_kmp_project.feature.shell

import com.example.my_kmp_project.feature.mine.MineIslandRoute

/**
 * Harmony ArkTS sets this before creating MainArkUIViewController.
 * - mode 0: Mine island ([mineRoute])
 * - mode 1: Secondary RoutePath island ([secondaryRoute])
 */
object OhosComposeHostRequest {
    var mode: Int = 0
    var mineRoute: MineIslandRoute = MineIslandRoute.Settings
    var secondaryRoute: String = "/home/search"
}
