package com.example.my_kmp_project.core.router

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * 路由改造第 1 批：Registry 是 hostForPath / parse 前缀判断的唯一来源。
 * 这里锁住收敛后的表与原行为逐条等价（包括「宿主可归类但深链不接受」的差异）。
 */
class RouteRegistryHostTest {

    @Test
    fun mainTabsAndExactPaths() {
        assertEquals(ProductRouteHost.Tab, DefaultRouteRegistry.hostFor("/home"))
        assertEquals(ProductRouteHost.Tab, DefaultRouteRegistry.hostFor("/main"))
        assertEquals(ProductRouteHost.Tab, DefaultRouteRegistry.hostFor("/chat"))
        assertEquals(ProductRouteHost.Tab, DefaultRouteRegistry.hostFor("/community"))
        assertEquals(ProductRouteHost.Tab, DefaultRouteRegistry.hostFor("/mine"))
    }

    @Test
    fun homeAndCommunityChildren() {
        assertEquals(ProductRouteHost.Home, DefaultRouteRegistry.hostFor("/home/search"))
        assertEquals(ProductRouteHost.Community, DefaultRouteRegistry.hostFor("/community/publish"))
    }

    @Test
    fun mineFamilyPrefixes() {
        for (path in listOf("/mine/profile", "/settings/linking_debug", "/mall/detail", "/wallet", "/pay/membership")) {
            assertEquals(ProductRouteHost.Mine, DefaultRouteRegistry.hostFor(path), path)
        }
    }

    @Test
    fun contentFamilyPrefixes() {
        for (path in listOf(
            "/ai/stream", "/ai", "/video/short", "/classroom/my_class",
            "/live/room", "/friend/list", "/music/list", "/media/entry",
        )) {
            assertEquals(ProductRouteHost.Content, DefaultRouteRegistry.hostFor(path), path)
        }
    }

    @Test
    fun webScanAndAuth() {
        assertEquals(ProductRouteHost.Web, DefaultRouteRegistry.hostFor("/web"))
        assertEquals(ProductRouteHost.Web, DefaultRouteRegistry.hostFor("https://cdn.demo.local/a.json"))
        assertEquals(ProductRouteHost.Scan, DefaultRouteRegistry.hostFor("/scan"))
        assertEquals(ProductRouteHost.Auth, DefaultRouteRegistry.hostFor("/auth/login"))
        assertEquals(ProductRouteHost.Auth, DefaultRouteRegistry.hostFor("/login/otp"))
        assertEquals(ProductRouteHost.Auth, DefaultRouteRegistry.hostFor("/register"))
        assertEquals(ProductRouteHost.AllServices, DefaultRouteRegistry.hostFor("/home/all_services"))
    }

    @Test
    fun queryFragmentAndMineAliasesAreNormalizedLikeTheOldApi() {
        assertEquals(ProductRouteHost.Mine, DefaultRouteRegistry.hostFor("/mall/orders?tab=1"))
        assertEquals(ProductRouteHost.Mine, DefaultRouteRegistry.hostFor("/wallet#top"))
        // MineRoutes.canonicalize：/mine/qa → /mine/http_test，归类仍是 Mine。
        assertEquals(ProductRouteHost.Mine, DefaultRouteRegistry.hostFor("/mine/qa"))
    }

    @Test
    fun unknownAndDeepLinkOnlyPaths() {
        assertEquals(ProductRouteHost.Unknown, DefaultRouteRegistry.hostFor("/unknown"))
        // /chat/detail 只在深链里出现，宿主归类与旧实现一致为 Unknown。
        assertEquals(ProductRouteHost.Unknown, DefaultRouteRegistry.hostFor("/chat/detail"))
    }
}

class RouteRegistryDeepLinkTest {

    @Test
    fun exactTargetsKeepTheirCanonicalRoute() {
        assertEquals(RouteMatch(MainTab.Home, "/home"), DefaultRouteRegistry.match("/home"))
        assertEquals(RouteMatch(MainTab.Chat, "/chat"), DefaultRouteRegistry.match("/chat"))
        assertEquals(RouteMatch(MainTab.Chat, "/chat/detail"), DefaultRouteRegistry.match("/chat/detail"))
        assertEquals(RouteMatch(MainTab.Community, "/community"), DefaultRouteRegistry.match("/community"))
        assertEquals(RouteMatch(MainTab.Mine, "/mine"), DefaultRouteRegistry.match("/mine"))
        assertEquals(RouteMatch(null, "/friend"), DefaultRouteRegistry.match("/friend"))
        assertEquals(RouteMatch(null, "/web"), DefaultRouteRegistry.match("/web"))
    }

    @Test
    fun loginAliasResolvesToTheCanonicalAuthPath() {
        assertEquals(RouteMatch(null, AppRoutes.Auth.LOGIN), DefaultRouteRegistry.match("/login"))
        assertEquals(
            RouteMatch(null, AppRoutes.Auth.LOGIN),
            DefaultRouteRegistry.match(AppRoutes.Auth.LOGIN),
        )
        assertEquals(RouteMatch(null, "/login/password"), DefaultRouteRegistry.match("/login/password"))
        assertEquals(RouteMatch(null, "/login/otp"), DefaultRouteRegistry.match("/login/otp"))
        assertEquals(RouteMatch(null, "/register"), DefaultRouteRegistry.match("/register"))
    }

    @Test
    fun prefixTargetsKeepTheIncomingPath() {
        assertEquals(RouteMatch(MainTab.Home, "/home/search"), DefaultRouteRegistry.match("/home/search"))
        assertEquals(RouteMatch(MainTab.Chat, "/chat/peer"), DefaultRouteRegistry.match("/chat/peer"))
        assertEquals(
            RouteMatch(MainTab.Community, "/community/publish"),
            DefaultRouteRegistry.match("/community/publish"),
        )
        assertEquals(RouteMatch(MainTab.Mine, "/mall/orders"), DefaultRouteRegistry.match("/mall/orders"))
        assertEquals(RouteMatch(MainTab.Mine, "/wallet"), DefaultRouteRegistry.match("/wallet"))
        assertEquals(RouteMatch(MainTab.Mine, "/pay/membership"), DefaultRouteRegistry.match("/pay/membership"))
        assertEquals(RouteMatch(MainTab.Mine, "/settings/x"), DefaultRouteRegistry.match("/settings/x"))
        assertEquals(RouteMatch(MainTab.Mine, "/mine/about"), DefaultRouteRegistry.match("/mine/about"))
        assertEquals(RouteMatch(null, "/ai/stream"), DefaultRouteRegistry.match("/ai/stream"))
        assertEquals(RouteMatch(null, "/video/short"), DefaultRouteRegistry.match("/video/short"))
        assertEquals(RouteMatch(null, "/music/list"), DefaultRouteRegistry.match("/music/list"))
        assertEquals(RouteMatch(null, "/classroom/my_class"), DefaultRouteRegistry.match("/classroom/my_class"))
        assertEquals(RouteMatch(null, "/live/room"), DefaultRouteRegistry.match("/live/room"))
    }

    @Test
    fun hostOnlyPrefixesDoNotBecomeDeepLinkTargets() {
        assertNull(DefaultRouteRegistry.match("/scan"))
        assertNull(DefaultRouteRegistry.match("/main"))
        assertNull(DefaultRouteRegistry.match("/media/entry"))
        assertNull(DefaultRouteRegistry.match("/friend/requests"))
        // 旧 parse 只接受 /ai/ 前缀，裸 /ai 不接受。
        assertNull(DefaultRouteRegistry.match("/ai"))
        assertNull(DefaultRouteRegistry.match("/unknown"))
    }
}

class LegacyRoutingDelegationTest {

    @Test
    fun hostForPathStaysByteIdenticalForRepresentativePaths() {
        val paths = listOf(
            "/home", "/main", "/chat", "/chat/detail", "/community", "/mine",
            "/home/search", "/community/publish", "/mine/profile", "/settings/x",
            "/mall/detail", "/wallet", "/pay/membership", "/ai/stream", "/ai",
            "/video/short", "/classroom/my_class", "/live/room", "/friend/list",
            "/music/list", "/media/entry", "/web", "https://cdn.demo.local/a.json",
            "/scan", "/auth/login", "/login/otp", "/register", "/home/all_services",
            "/mine/qa", "/unknown",
        )
        for (path in paths) {
            assertEquals(
                DefaultRouteRegistry.hostFor(path),
                ProductRouteDispatch.hostForPath(path),
                path,
            )
        }
    }

    @Test
    fun deepLinkParseKeepsOldTargets() {
        assertEquals(
            ParsedDeepLink("myai://home", MainTab.Home, "/home"),
            DeepLinkRouter.parse("myai://home"),
        )
        assertEquals(
            ParsedDeepLink("myai:///chat", MainTab.Chat, "/chat"),
            DeepLinkRouter.parse("myai:///chat"),
        )
        assertEquals(
            ParsedDeepLink("myai://auth/login", null, "/auth/login"),
            DeepLinkRouter.parse("myai://auth/login"),
        )
        assertEquals(
            ParsedDeepLink("myai://login", null, "/auth/login"),
            DeepLinkRouter.parse("myai://login"),
        )
        assertEquals(
            ParsedDeepLink("myai://mall/orders", MainTab.Mine, "/mall/orders"),
            DeepLinkRouter.parse("myai://mall/orders"),
        )
        assertEquals(
            ParsedDeepLink("myai://app/home/search", MainTab.Home, "/home/search"),
            DeepLinkRouter.parse("myai://app/home/search"),
        )
        assertNull(DeepLinkRouter.parse("myai://scan"))
        assertNull(DeepLinkRouter.parse(""))
    }

    @Test
    fun defaultAppRouterDelegatesToTheSameSources() {
        assertEquals(ProductRouteHost.Home, DefaultAppRouter.hostFor("/home/search"))
        assertEquals(
            DeepLinkRouter.parse("myai://mall/orders"),
            DefaultAppRouter.parseDeepLink("myai://mall/orders"),
        )
        val target = assertNotNull(DefaultAppRouter.resolve("小视频"))
        assertEquals("/video/short", target.path)
        assertEquals(ProductRouteHost.Content, target.host)
    }
}
