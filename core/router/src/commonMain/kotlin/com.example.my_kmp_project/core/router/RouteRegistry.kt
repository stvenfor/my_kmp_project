package com.example.my_kmp_project.core.router

/** 深链解析结果（不含原始 URI，由调用方保留）。 */
public data class RouteMatch(
    val tab: MainTab?,
    val route: String,
)

/**
 * 路由表契约（路由改造第 1 批）。
 *
 * 只做「路径 → 宿主 / 深链目标」的纯查询，不持有导航状态；
 * 旧 API（[ProductRouteDispatch.hostForPath] / [DeepLinkRouter.parse]）保留并委托到这里。
 */
public interface RouteRegistry {
    public val specs: List<RouteSpec>

    /** 产品宿主归类；无法归类返回 [ProductRouteHost.Unknown]。 */
    public fun hostFor(path: String): ProductRouteHost

    /**
     * 深链目标；不支持返回 null。
     * [path] 需已完成 [DeepLinkRouter] 的规范化（小写、去尾斜杠、去 `/app` 前缀）。
     */
    public fun match(path: String): RouteMatch?
}

/**
 * 全量路由表默认实现。
 *
 * 顺序即优先级：精确行在前、前缀行在后 —— 保证 `/home/all_services` 先于 `/home/`、
 * `/mine` 先于 `/mine/`、`/home` 先于 `/home/`。行为与原两处 when-chain 逐条对齐。
 */
public object DefaultRouteRegistry : RouteRegistry {

    override val specs: List<RouteSpec> = listOf(
        // ── 精确：宿主归类 + 深链 ─────────────────────────────────────────────
        RouteSpec(AppRoutePath.homeAllServices, ProductRouteHost.AllServices, deepLink = false),
        RouteSpec(AppRoutePath.web, ProductRouteHost.Web),
        RouteSpec("/scan", ProductRouteHost.Scan, deepLink = false),
        RouteSpec(AppRoutes.Auth.LOGIN, ProductRouteHost.Auth),
        RouteSpec(
            path = AppRoutePath.login,
            host = ProductRouteHost.Auth,
            canonicalPath = AppRoutes.Auth.LOGIN,
        ),
        RouteSpec(AppRoutePath.loginPassword, ProductRouteHost.Auth),
        RouteSpec(AppRoutePath.loginOtp, ProductRouteHost.Auth),
        RouteSpec(AppRoutePath.register, ProductRouteHost.Auth),
        RouteSpec(AppRoutes.Home.HOME, ProductRouteHost.Tab, tab = MainTab.Home),
        RouteSpec(AppRoutePath.main, ProductRouteHost.Tab, deepLink = false),
        RouteSpec(AppRoutes.Chat.CHAT, ProductRouteHost.Tab, tab = MainTab.Chat),
        RouteSpec(AppRoutes.Chat.DETAIL, ProductRouteHost.Unknown, tab = MainTab.Chat),
        RouteSpec(AppRoutes.Community.COMMUNITY, ProductRouteHost.Tab, tab = MainTab.Community),
        RouteSpec(AppRoutes.Mine.MINE, ProductRouteHost.Tab, tab = MainTab.Mine),
        RouteSpec(AppRoutePath.friend, ProductRouteHost.Content),

        // ── 前缀：深链目标（宿主与深链都接受）────────────────────────────────
        RouteSpec("/home/", ProductRouteHost.Home, tab = MainTab.Home, pattern = RoutePattern.Prefix),
        RouteSpec("/chat/", ProductRouteHost.Unknown, tab = MainTab.Chat, pattern = RoutePattern.Prefix),
        RouteSpec(
            "/community/",
            ProductRouteHost.Community,
            tab = MainTab.Community,
            pattern = RoutePattern.Prefix,
        ),
        RouteSpec("/mall", ProductRouteHost.Mine, tab = MainTab.Mine, pattern = RoutePattern.Prefix),
        RouteSpec("/wallet", ProductRouteHost.Mine, tab = MainTab.Mine, pattern = RoutePattern.Prefix),
        RouteSpec("/pay", ProductRouteHost.Mine, tab = MainTab.Mine, pattern = RoutePattern.Prefix),
        RouteSpec("/settings", ProductRouteHost.Mine, tab = MainTab.Mine, pattern = RoutePattern.Prefix),
        RouteSpec("/mine/", ProductRouteHost.Mine, tab = MainTab.Mine, pattern = RoutePattern.Prefix),
        RouteSpec("/video", ProductRouteHost.Content, pattern = RoutePattern.Prefix),
        RouteSpec("/music", ProductRouteHost.Content, pattern = RoutePattern.Prefix),
        RouteSpec("/classroom", ProductRouteHost.Content, pattern = RoutePattern.Prefix),
        RouteSpec("/live", ProductRouteHost.Content, pattern = RoutePattern.Prefix),
        RouteSpec("/ai/", ProductRouteHost.Content, pattern = RoutePattern.Prefix),

        // ── 前缀：仅宿主归类（深链旧行为不接受这些前缀）──────────────────────
        RouteSpec("http://", ProductRouteHost.Web, pattern = RoutePattern.Prefix, deepLink = false),
        RouteSpec("https://", ProductRouteHost.Web, pattern = RoutePattern.Prefix, deepLink = false),
        RouteSpec("/auth", ProductRouteHost.Auth, pattern = RoutePattern.Prefix, deepLink = false),
        RouteSpec("/login", ProductRouteHost.Auth, pattern = RoutePattern.Prefix, deepLink = false),
        // 旧 hostForPath 用 startsWith("/mine")，比深链的 "/mine/" 更宽。
        RouteSpec("/mine", ProductRouteHost.Mine, pattern = RoutePattern.Prefix, deepLink = false),
        RouteSpec("/ai", ProductRouteHost.Content, pattern = RoutePattern.Prefix, deepLink = false),
        RouteSpec("/friend", ProductRouteHost.Content, pattern = RoutePattern.Prefix, deepLink = false),
        RouteSpec("/media", ProductRouteHost.Content, pattern = RoutePattern.Prefix, deepLink = false),
    )

    override fun hostFor(path: String): ProductRouteHost {
        val canonical = canonicalize(path)
        return specs.firstOrNull { it.matches(canonical) }?.host ?: ProductRouteHost.Unknown
    }

    override fun match(path: String): RouteMatch? {
        val spec = specs.firstOrNull { it.deepLink && it.matches(path) } ?: return null
        return RouteMatch(tab = spec.tab, route = spec.resolveRoute(path))
    }

    private fun RouteSpec.resolveRoute(value: String): String = when (pattern) {
        RoutePattern.Exact -> canonicalPath
        RoutePattern.Prefix -> value
    }

    /** 与原 `ProductRouteDispatch.hostForPath` 相同的规范化：去 query/fragment + Mine 别名。 */
    private fun canonicalize(path: String): String =
        MineRoutes.canonicalize(path.substringBefore('?').substringBefore('#'))
}
