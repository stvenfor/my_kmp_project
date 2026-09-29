package com.example.my_kmp_project.feature.mine

import com.example.my_kmp_project.feature.home.HomeRoutes

/**
 * Flutter `MineController` tap handlers — keyed by catalog **id**, not Chinese label.
 * Root Mine page must only navigate the Flutter-allowed whitelist; undeveloped
 * entries toast (`… 开发中` or fixed menu copy).
 */
internal sealed class MineTapResult {
    data class Toast(val message: String) : MineTapResult()
    data class Navigate(val path: String) : MineTapResult()
    data class RequireLogin(val redirectPath: String) : MineTapResult()
}

internal object MineTapDispatch {
    fun onQuickService(id: String, loggedIn: Boolean): MineTapResult = when (id) {
        "mall" -> authOrNav(loggedIn, MineRoutes.Mall)
        "order" -> authOrNav(loggedIn, MineRoutes.MallOrders)
        "wallet" -> authOrNav(loggedIn, MineRoutes.Wallet)
        else -> {
            val label = MineCatalog.quickServices.firstOrNull { it.id == id }?.label ?: id
            MineTapResult.Toast("$label 开发中")
        }
    }

    fun onFunction(id: String, loggedIn: Boolean): MineTapResult = when (id) {
        "calculator" -> MineTapResult.Navigate(MineRoutes.Calculator)
        "qa" -> MineTapResult.Navigate(MineRoutes.HttpTest)
        "short_video" -> authOrNav(loggedIn, MineRoutes.ShortVideo)
        "used_car" -> authOrNav(loggedIn, HomeRoutes.UsedCar)
        "ledger" -> authOrNav(loggedIn, HomeRoutes.Ledger)
        "after_sales" -> authOrNav(loggedIn, HomeRoutes.AfterSales)
        // Flutter: sms / qr_pay / poster → toast 开发中 (do not open Compose stubs from root).
        else -> {
            val title = MineCatalog.functions.firstOrNull { it.id == id }?.title ?: id
            MineTapResult.Toast("$title 开发中")
        }
    }

    fun onMenu(id: String, loggedIn: Boolean): MineTapResult = when (id) {
        "address" -> authOrNav(loggedIn, MineRoutes.Addresses)
        "settings" -> MineTapResult.Navigate(MineRoutes.Settings)
        "feedback" -> MineTapResult.Toast("意见反馈")
        "fan_group" -> MineTapResult.Toast("粉丝群")
        "invite" -> MineTapResult.Toast("邀请好友")
        "reminder" -> MineTapResult.Toast("提醒事项")
        "cooperation" -> MineTapResult.Toast("商务合作")
        else -> {
            val label = MineCatalog.menuItems.firstOrNull { it.id == id }?.label ?: id
            MineTapResult.Toast("$label 开发中")
        }
    }

    private fun authOrNav(loggedIn: Boolean, path: String): MineTapResult =
        if (loggedIn) MineTapResult.Navigate(path) else MineTapResult.RequireLogin(path)
}
