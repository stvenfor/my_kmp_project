package com.example.my_kmp_project.core.router

import com.example.my_kmp_project.feature.community.CommunityRoutes
import com.example.my_kmp_project.feature.content.ContentRoutes
import com.example.my_kmp_project.feature.home.HomeRoutes
import com.example.my_kmp_project.feature.mine.MineRoutes

/**
 * Single label/path → canonical Flutter [AppRoutePath] resolver + shell host class.
 * Used by Native Android / Shared Compose / SecondaryRouteIsland so Mine↔Home↔Content
 * jumps land on the correct overlay (not stuck in the wrong host).
 */
internal enum class ProductRouteHost {
    /** Stay on / switch to a main tab without secondary overlay. */
    Tab,
    Home,
    Mine,
    Community,
    Content,
    AllServices,
    Web,
    Scan,
    Auth,
    /** Flutter toast-only; do not open a page. */
    Toast,
    Unknown,
}

internal data class ProductRouteTarget(
    val path: String,
    val host: ProductRouteHost,
    /** When [host] is [ProductRouteHost.Toast], show this message. */
    val toastMessage: String? = null,
)

internal object ProductRouteDispatch {
    private val toastOnlyLabels = setOf(
        "电子名片", "商务合作", "提醒事项", "邀请好友", "粉丝群",
        "意见反馈", "帮助中心", "头像", "请先登录",
        "切换门店", "切换店铺",
        // Flutter Mine function undeveloped — toast from root / shell mis-routes.
        "短信模板", "店铺收款码", "收款码", "商家海报", "海报",
    )

    private val toastOnlyPaths = setOf(
        "/mine/business_card",
        "/mine/invite",
        "/mine/business",
        "/mine/reminders",
        "/mine/reminder",
        "/mine/fan_group",
        "/mine/feedback",
        "/mine/cooperation",
        // Undeveloped Mine function paths (root must not open; deep-link screens may still exist).
        "/mine/sms_templates",
        "/mine/sms_template",
        "/mine/shop_qr",
        "/mine/store_qr",
        "/mine/poster",
    )

    /**
     * Resolve a UI label, short alias, or absolute path into a navigable target.
     * Returns null only when empty; unknown labels become [ProductRouteHost.Unknown].
     */
    fun resolve(raw: String): ProductRouteTarget? {
        val input = raw.trim()
        if (input.isEmpty()) return null

        if (input in toastOnlyLabels || input in toastOnlyPaths) {
            val msg = when (input) {
                "切换门店", "切换店铺" -> "请在「我的」页头点击门店名称切换"
                "请先登录" -> "请先登录"
                "短信模板", "/mine/sms_templates", "/mine/sms_template" -> "短信模板 开发中"
                "店铺收款码", "收款码", "/mine/shop_qr", "/mine/store_qr" -> "店铺收款码 开发中"
                "商家海报", "海报", "/mine/poster" -> "商家海报 开发中"
                else -> input.removePrefix("/mine/").ifBlank { input }
            }
            return ProductRouteTarget(input, ProductRouteHost.Toast, toastMessage = msg)
        }

        // Absolute / scheme paths
        if (input.startsWith("/") || input.contains("://") ||
            input.startsWith("http://") || input.startsWith("https://")
        ) {
            val path = canonicalizePath(stripToPath(input))
            return ProductRouteTarget(path, hostForPath(path))
        }

        // Explicit shell aliases (Shared HomeScreen / tools row)
        when (input) {
            "search" -> return target(HomeRoutes.Search)
            "strategy" -> return target(HomeRoutes.Strategy)
            "report" -> return target(HomeRoutes.LearningReport)
            "services", "全部服务", "更多" ->
                return ProductRouteTarget(AppRoutePath.homeAllServices, ProductRouteHost.AllServices)
            "web", "H5 调试", "内嵌网页" ->
                return ProductRouteTarget(AppRoutePath.web, ProductRouteHost.Web)
            "scan", "扫一扫" ->
                return ProductRouteTarget("/scan", ProductRouteHost.Scan)
            "media" -> return target(ContentRoutes.MediaEntry)
            "friend" -> return target(ContentRoutes.Friend)
            "live" -> return target(ContentRoutes.Live)
            "classroom" -> return target(AppRoutePath.classroomMyClass)
            "消息" -> return ProductRouteTarget(AppRoutePath.chat, ProductRouteHost.Tab)
        }

        MineRoutes.fromLabel(input)?.let { return target(canonicalizePath(it)) }
        HomeRoutes.fromLabel(input)?.let { return target(canonicalizePath(it)) }
        CommunityRoutes.fromLabel(input)?.let { return target(canonicalizePath(it)) }
        ContentRoutes.fromLabel(input)?.let { return target(canonicalizePath(it)) }

        return ProductRouteTarget(input, ProductRouteHost.Unknown)
    }

    fun hostForPath(path: String): ProductRouteHost {
        val p = canonicalizePath(path)
        return when {
            p == AppRoutePath.homeAllServices -> ProductRouteHost.AllServices
            p == AppRoutePath.web || p.startsWith("http://") || p.startsWith("https://") ->
                ProductRouteHost.Web
            p == "/scan" -> ProductRouteHost.Scan
            p == AppRoutePath.login || p.startsWith("/login") || p.startsWith("/auth") ||
                p == AppRoutePath.register -> ProductRouteHost.Auth
            p == AppRoutePath.home || p == AppRoutePath.main || p == AppRoutePath.chat ||
                p == AppRoutePath.community || p == AppRoutePath.mine -> ProductRouteHost.Tab
            p.startsWith("/home/") -> ProductRouteHost.Home
            p.startsWith("/community/") -> ProductRouteHost.Community
            p.startsWith("/mine") || p.startsWith("/settings") ||
                p.startsWith("/mall") || p.startsWith("/wallet") || p.startsWith("/pay") ->
                ProductRouteHost.Mine
            p.startsWith("/ai") || p.startsWith("/video") || p.startsWith("/classroom") ||
                p.startsWith("/live") || p.startsWith("/friend") || p.startsWith("/music") ||
                p.startsWith("/media") -> ProductRouteHost.Content
            else -> ProductRouteHost.Unknown
        }
    }

    private fun target(path: String): ProductRouteTarget {
        val canon = canonicalizePath(path)
        return ProductRouteTarget(canon, hostForPath(canon))
    }

    private fun canonicalizePath(path: String): String {
        val base = path.substringBefore('?').substringBefore('#')
        return MineRoutes.canonicalize(base)
    }

    private fun stripToPath(uri: String): String {
        var candidate = uri.trim()
        val sep = candidate.indexOf("://")
        if (sep >= 0) {
            candidate = candidate.substring(sep + 3)
            if (!candidate.startsWith("/")) candidate = "/$candidate"
        }
        return candidate.substringBefore('?').substringBefore('#')
    }
}
