package com.example.my_kmp_project.nativeshell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.my_kmp_project.app.AppContainer
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.DesignTokens
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.TokenExpiredHandler
import com.example.my_kmp_project.core.platform.DefaultWebBridgeHost
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.router.AppRoutePath
import com.example.my_kmp_project.core.router.AppRoutes
import com.example.my_kmp_project.core.router.DeepLinkRouter
import com.example.my_kmp_project.core.router.MainTab
import com.example.my_kmp_project.feature.auth.AuthGate
import com.example.my_kmp_project.feature.auth.AuthRepository
import com.example.my_kmp_project.feature.auth.LoginOtpScreen
import com.example.my_kmp_project.feature.auth.LoginPasswordScreen
import com.example.my_kmp_project.feature.auth.LoginScreen
import com.example.my_kmp_project.feature.auth.RegisterScreen
import com.example.my_kmp_project.component.webview.OfflineWebFixtureUrl
import com.example.my_kmp_project.core.router.webUrlFromDeepLink
import com.example.my_kmp_project.feature.chat.ChatDetailDeepLinkArgs
import com.example.my_kmp_project.feature.chat.chatDetailArgsFromDeepLink
import com.example.my_kmp_project.feature.commerce.MembershipScreen
import com.example.my_kmp_project.feature.community.CommunityRouteHost
import com.example.my_kmp_project.feature.community.CommunityRoutes
import com.example.my_kmp_project.feature.content.ContentRouteHost
import com.example.my_kmp_project.feature.content.ContentRoutes
import com.example.my_kmp_project.feature.home.AllServicesScreen
import com.example.my_kmp_project.feature.home.HomeRouteHost
import com.example.my_kmp_project.feature.home.HomeRoutes
import com.example.my_kmp_project.feature.home.HomeWebHandlers
import com.example.my_kmp_project.feature.mine.MineIsland
import com.example.my_kmp_project.feature.mine.MineIslandRoute
import com.example.my_kmp_project.feature.mine.MineRouteHost
import com.example.my_kmp_project.feature.mine.MineRoutes
import com.example.my_kmp_project.feature.scan.ScanScreen
import com.example.my_kmp_project.feature.shell.MainBottomBar
import com.example.my_kmp_project.feature.shell.SoftAuthPresenter
import com.example.my_kmp_project.feature.web.InAppWebScreen
/**
 * Android Jetpack shell (ADR 0002 / Android UI Split).
 * Tab roots + auth + deferred stubs are Jetpack in androidMain.
 * Shared CMP is only [MineIsland].
 */
@Composable
internal fun NativeAndroidMain() {
    AppContainer.get()
    val softAuth = remember { SoftAuthPresenter() }
    DisposableEffect(Unit) {
        softAuth.sync()
        HomeWebHandlers.register()
        NetworkFacade.setTokenExpiredHandler(
            TokenExpiredHandler {
                AuthRepository.logout()
                softAuth.clearLocalSession()
            },
        )
        onDispose { NetworkFacade.setTokenExpiredHandler(null) }
    }

    var tab by remember { mutableStateOf(MainTab.Home) }
    var keptTabs by remember { mutableStateOf(setOf(MainTab.Home)) }
    var authOverlay by remember { mutableStateOf(AuthOverlay.None) }
    var overlay by remember { mutableStateOf(ShellOverlay.None) }
    var webUrl by remember { mutableStateOf(OfflineWebFixtureUrl) }
    var homeRoute by remember { mutableStateOf<String?>(null) }
    var homeRouteStack by remember { mutableStateOf<List<String>>(emptyList()) }
    var communityRoute by remember { mutableStateOf<String?>(null) }
    var communityPreviewUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var communityPreviewIndex by remember { mutableStateOf(0) }
    var communityVideoUrl by remember { mutableStateOf<String?>(null) }
    var mineRoute by remember { mutableStateOf<String?>(null) }
    var mineRouteStack by remember { mutableStateOf<List<String>>(emptyList()) }
    var contentRoute by remember { mutableStateOf<String?>(null) }
    var contentRouteStack by remember { mutableStateOf<List<String>>(emptyList()) }
    var islandRoute by remember { mutableStateOf(MineIslandRoute.Settings) }
    var deferredTitle by remember { mutableStateOf("后续开放") }
    var bottomBarVisible by remember { mutableStateOf(true) }
    var chatPendingDetail by remember { mutableStateOf<ChatDetailDeepLinkArgs?>(null) }
    var chatMissingDetail by remember { mutableStateOf(false) }
    val authState by softAuth.uiState.collectAsState()

    fun selectTab(next: MainTab) {
        if (softAuth.trySelectTab(next)) {
            tab = next
            keptTabs = keptTabs + next
            authOverlay = AuthOverlay.None
            bottomBarVisible = true
        } else {
            authOverlay = AuthOverlay.Login
            bottomBarVisible = false
        }
    }

    fun afterAuthSuccess() {
        val resume = softAuth.onLoginSucceeded()
        tab = resume
        keptTabs = keptTabs + resume
        authOverlay = AuthOverlay.None
        bottomBarVisible = true
    }

    fun openHomeRoute(route: String) {
        homeRoute = route
        homeRouteStack = listOf(route)
        overlay = ShellOverlay.HomeRoute
        bottomBarVisible = false
    }

    fun pushHomeRoute(route: String) {
        homeRoute = route
        homeRouteStack = homeRouteStack + route
        overlay = ShellOverlay.HomeRoute
        bottomBarVisible = false
    }

    fun popHomeRoute() {
        if (homeRouteStack.size <= 1) {
            homeRoute = null
            homeRouteStack = emptyList()
            overlay = ShellOverlay.None
            bottomBarVisible = authOverlay == AuthOverlay.None
        } else {
            val next = homeRouteStack.dropLast(1)
            homeRouteStack = next
            homeRoute = next.last()
        }
    }

    fun openCommunityRoute(
        route: String,
        previewUrls: List<String> = emptyList(),
        previewIndex: Int = 0,
        videoUrl: String? = null,
    ) {
        communityRoute = route
        communityPreviewUrls = previewUrls
        communityPreviewIndex = previewIndex
        communityVideoUrl = videoUrl
        overlay = ShellOverlay.CommunityRoute
        bottomBarVisible = false
    }

    fun openMineRoute(route: String) {
        mineRoute = route
        mineRouteStack = listOf(route)
        overlay = ShellOverlay.MineRoute
        bottomBarVisible = false
    }

    fun pushMineRoute(route: String) {
        mineRoute = route
        mineRouteStack = mineRouteStack + route
        overlay = ShellOverlay.MineRoute
        bottomBarVisible = false
    }

    fun popMineRoute() {
        if (mineRouteStack.size <= 1) {
            mineRoute = null
            mineRouteStack = emptyList()
            overlay = ShellOverlay.None
            bottomBarVisible = authOverlay == AuthOverlay.None
        } else {
            val next = mineRouteStack.dropLast(1)
            mineRouteStack = next
            mineRoute = next.last()
        }
    }

    fun openContentRoute(route: String) {
        contentRoute = route
        contentRouteStack = listOf(route)
        overlay = ShellOverlay.ContentRoute
        bottomBarVisible = false
    }

    fun pushContentRoute(route: String) {
        contentRoute = route
        contentRouteStack = contentRouteStack + route
        overlay = ShellOverlay.ContentRoute
        bottomBarVisible = false
    }

    fun popContentRoute() {
        if (contentRouteStack.size <= 1) {
            contentRoute = null
            contentRouteStack = emptyList()
            overlay = ShellOverlay.None
            bottomBarVisible = authOverlay == AuthOverlay.None
        } else {
            val next = contentRouteStack.dropLast(1)
            contentRouteStack = next
            contentRoute = next.last()
        }
    }

    fun openDeferred(title: String) {
        val homeMapped = HomeRoutes.fromLabel(title)
        val communityMapped = CommunityRoutes.fromLabel(title)
        val mineMapped = MineRoutes.fromLabel(title)
        val contentMapped = ContentRoutes.fromLabel(title)
        when {
            title == "全部服务" || title == "更多" -> {
                overlay = ShellOverlay.AllServices
                bottomBarVisible = false
            }
            title == "扫一扫" -> {
                overlay = ShellOverlay.Scan
                bottomBarVisible = false
            }
            title.startsWith("http://") || title.startsWith("https://") || title == "H5 调试" || title == "内嵌网页" -> {
                webUrl = if (title.startsWith("http")) title else OfflineWebFixtureUrl
                overlay = ShellOverlay.InAppWeb
                bottomBarVisible = false
            }
            mineMapped != null -> {
                if (
                    mineMapped == HomeRoutes.UsedCar ||
                    mineMapped == HomeRoutes.CheckInMall ||
                    mineMapped == HomeRoutes.Ledger ||
                    mineMapped == HomeRoutes.AfterSales
                ) {
                    openHomeRoute(mineMapped)
                } else if (
                    mineMapped == MineRoutes.Classroom ||
                    mineMapped == MineRoutes.ShortVideo
                ) {
                    openContentRoute(mineMapped)
                } else {
                    openMineRoute(mineMapped)
                }
            }
            homeMapped != null -> openHomeRoute(homeMapped)
            communityMapped != null -> openCommunityRoute(communityMapped)
            contentMapped != null -> openContentRoute(contentMapped)
            else -> {
                // Out-of-scope / unmapped label only — never for in-scope RoutePath.
                deferredTitle = title
                overlay = ShellOverlay.UnmappedEntry
                bottomBarVisible = false
            }
        }
    }

    fun closeOverlay() {
        homeRoute = null
        homeRouteStack = emptyList()
        communityRoute = null
        mineRoute = null
        mineRouteStack = emptyList()
        contentRoute = null
        contentRouteStack = emptyList()
        overlay = ShellOverlay.None
        bottomBarVisible = authOverlay == AuthOverlay.None
    }

    LaunchedEffect(Unit) {
        snapshotFlow { DeepLinkRouter.pendingDeepLink }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { pending ->
                DeepLinkRouter.consumePending()
                when {
                    pending.route == AppRoutes.Auth.LOGIN -> {
                        authOverlay = AuthOverlay.Login
                        bottomBarVisible = false
                    }
                    pending.route == AppRoutePath.loginPassword -> {
                        authOverlay = AuthOverlay.LoginPassword
                        bottomBarVisible = false
                    }
                    pending.route == AppRoutePath.loginOtp -> {
                        authOverlay = AuthOverlay.LoginOtp
                        bottomBarVisible = false
                    }
                    pending.route == AppRoutePath.register -> {
                        authOverlay = AuthOverlay.Register
                        bottomBarVisible = false
                    }
                    pending.route == AppRoutePath.web -> {
                        webUrl = webUrlFromDeepLink(pending.rawUri, OfflineWebFixtureUrl)
                        overlay = ShellOverlay.InAppWeb
                        bottomBarVisible = false
                    }
                    pending.route == AppRoutePath.friend -> {
                        openContentRoute(ContentRoutes.Friend)
                    }
                    pending.route.startsWith("/video") ||
                        pending.route.startsWith("/music") ||
                        pending.route.startsWith("/classroom") ||
                        pending.route.startsWith("/live") ||
                        pending.route.startsWith("/ai/") -> {
                        openContentRoute(pending.route)
                    }
                    pending.route == AppRoutes.Chat.DETAIL ||
                        pending.route == AppRoutePath.chatDetail ||
                        pending.route.startsWith("/chat/") -> {
                        val target = MainTab.Chat
                        val args = chatDetailArgsFromDeepLink(pending.rawUri)
                        fun applyChatDetail() {
                            if (args == null) {
                                chatPendingDetail = null
                                chatMissingDetail = true
                            } else {
                                chatMissingDetail = false
                                chatPendingDetail = args
                            }
                        }
                        if (AuthGate.requiresAuth(target) && !authState.isLoggedIn) {
                            AuthGate.rememberPending(target)
                            authOverlay = AuthOverlay.Login
                            bottomBarVisible = false
                            applyChatDetail()
                        } else {
                            tab = target
                            keptTabs = keptTabs + target
                            applyChatDetail()
                        }
                    }
                    pending.route.startsWith("/home/") -> {
                        tab = MainTab.Home
                        keptTabs = keptTabs + MainTab.Home
                        if (pending.route == "/home/all_services") {
                            overlay = ShellOverlay.AllServices
                            bottomBarVisible = false
                        } else {
                            openHomeRoute(pending.route)
                        }
                    }
                    pending.route.startsWith("/community/") -> {
                        val target = MainTab.Community
                        if (AuthGate.requiresAuth(target) && !authState.isLoggedIn) {
                            AuthGate.rememberPending(target)
                            authOverlay = AuthOverlay.Login
                            bottomBarVisible = false
                        } else {
                            tab = target
                            keptTabs = keptTabs + target
                            openCommunityRoute(pending.route)
                        }
                    }
                    pending.route.startsWith("/mall") ||
                        pending.route.startsWith("/wallet") ||
                        pending.route.startsWith("/pay") -> {
                        tab = MainTab.Mine
                        keptTabs = keptTabs + MainTab.Mine
                        openMineRoute(pending.route)
                    }
                    pending.route == AppRoutePath.settings || pending.route.startsWith("/settings") -> {
                        tab = MainTab.Mine
                        keptTabs = keptTabs + MainTab.Mine
                        openMineRoute(MineRoutes.Settings)
                    }
                    pending.route.startsWith("/mine/") -> {
                        tab = MainTab.Mine
                        keptTabs = keptTabs + MainTab.Mine
                        openMineRoute(pending.route)
                    }
                    pending.tab != null -> {
                        val target = pending.tab
                        if (AuthGate.requiresAuth(target) && !authState.isLoggedIn) {
                            AuthGate.rememberPending(target)
                            authOverlay = AuthOverlay.Login
                            bottomBarVisible = false
                        } else {
                            tab = target
                            keptTabs = keptTabs + target
                        }
                    }
                }
            }
    }

    when (overlay) {
        ShellOverlay.MineIsland -> {
            MineIsland(
                initialRoute = islandRoute,
                onRequestClose = { closeOverlay() },
            )
        }
        ShellOverlay.AllServices -> {
            AllServicesScreen(
                onBack = { closeOverlay() },
                onOpen = { label ->
                    closeOverlay()
                    openDeferred(label)
                },
            )
        }
        ShellOverlay.HomeRoute -> {
            val route = homeRoute ?: HomeRoutes.Search
            HomeRouteHost(
                route = route,
                onBack = { popHomeRoute() },
                onNavigate = { pushHomeRoute(it) },
            )
        }
        ShellOverlay.CommunityRoute -> {
            CommunityRouteHost(
                route = communityRoute ?: CommunityRoutes.Search,
                onBack = { closeOverlay() },
                onNavigate = { openCommunityRoute(it) },
                previewUrls = communityPreviewUrls,
                previewIndex = communityPreviewIndex,
                videoUrl = communityVideoUrl,
            )
        }
        ShellOverlay.MineRoute -> {
            MineRouteHost(
                route = mineRoute ?: MineRoutes.Mall,
                onBack = { popMineRoute() },
                onNavigate = { pushMineRoute(it) },
            )
        }
        ShellOverlay.ContentRoute -> {
            ContentRouteHost(
                route = contentRoute ?: ContentRoutes.MediaEntry,
                onBack = { popContentRoute() },
                onNavigate = { pushContentRoute(it) },
            )
        }
        ShellOverlay.Membership -> {
            MembershipScreen(onBack = { closeOverlay() })
        }
        ShellOverlay.InAppWeb -> {
            InAppWebScreen(
                url = webUrl,
                onBack = { closeOverlay() },
                bridge = remember(webUrl) {
                    DefaultWebBridgeHost(
                        onClose = { closeOverlay() },
                        onOpenNative = { payload ->
                            payload?.let { openDeferred(it) }
                        },
                    )
                },
            )
        }
        ShellOverlay.Scan -> {
            ScanScreen(
                onBack = { closeOverlay() },
                onScanResult = { payload ->
                    val accepted = DeepLinkRouter.accept(payload)
                    if (accepted == null &&
                        (payload.startsWith("http://") || payload.startsWith("https://"))
                    ) {
                        webUrl = payload
                        overlay = ShellOverlay.InAppWeb
                    } else {
                        showPlatformToast(
                            if (accepted != null) "已识别链接" else "扫码结果：$payload",
                        )
                        closeOverlay()
                    }
                },
            )
        }
        ShellOverlay.UnmappedEntry -> {
            JetpackUnmappedEntry(
                title = deferredTitle,
                onBack = { closeOverlay() },
            )
        }
        ShellOverlay.None -> when (authOverlay) {
            AuthOverlay.Login -> LoginScreen(
                onLoginSuccess = { afterAuthSuccess() },
                onOpenRegister = { authOverlay = AuthOverlay.Register },
                onBack = {
                    softAuth.dismissGate()
                    authOverlay = AuthOverlay.None
                    tab = MainTab.Home
                    bottomBarVisible = true
                },
            )
            AuthOverlay.LoginPassword -> LoginPasswordScreen(
                onLoginSuccess = { afterAuthSuccess() },
                onOpenRegister = { authOverlay = AuthOverlay.Register },
                onBack = { authOverlay = AuthOverlay.Login },
            )
            AuthOverlay.LoginOtp -> LoginOtpScreen(
                onLoginSuccess = { afterAuthSuccess() },
                onBack = { authOverlay = AuthOverlay.Login },
            )
            AuthOverlay.Register -> RegisterScreen(
                onRegistered = { afterAuthSuccess() },
                onBack = { authOverlay = AuthOverlay.Login },
            )
            AuthOverlay.None -> {
                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0),
                    bottomBar = {
                        if (bottomBarVisible) {
                            MainBottomBar(selected = tab, onSelect = { selectTab(it) })
                        }
                    },
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    ) {
                        // IndexedStack-style keep-alive for visited tabs.
                        MainTab.entries.forEach { t ->
                            if (t in keptTabs) {
                                val active = t == tab
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .zIndex(if (active) 1f else 0f)
                                        .graphicsLayer {
                                            alpha = if (active) 1f else 0f
                                        },
                                ) {
                                    when (t) {
                                        MainTab.Home -> JetpackHomeRoot(
                                            onDeferred = { openDeferred(it) },
                                            displayName = authState.displayName,
                                        )
                                        MainTab.Chat -> JetpackChatRoot(
                                            onOpenContacts = { openDeferred("通讯录") },
                                            pendingDetail = chatPendingDetail,
                                            showMissingDetailParams = chatMissingDetail,
                                            onPendingDetailConsumed = { chatPendingDetail = null },
                                            onMissingDetailConsumed = { chatMissingDetail = false },
                                            onDetailVisibilityChanged = { open ->
                                                bottomBarVisible = !open && authOverlay == AuthOverlay.None
                                            },
                                        )
                                        MainTab.Community -> JetpackCommunityRoot(
                                            onOpen = { label -> openDeferred(label) },
                                            onPreviewImages = { urls, index ->
                                                openCommunityRoute(
                                                    CommunityRoutes.ImagePreview,
                                                    previewUrls = urls,
                                                    previewIndex = index,
                                                )
                                            },
                                            onPlayVideo = { url ->
                                                openCommunityRoute(
                                                    CommunityRoutes.VideoPlay,
                                                    videoUrl = url,
                                                )
                                            },
                                        )
                                        MainTab.Mine -> JetpackMineRoot(
                                            loggedIn = authState.isLoggedIn,
                                            displayName = authState.displayName,
                                            onLogin = {
                                                AuthGate.rememberPending(MainTab.Mine)
                                                authOverlay = AuthOverlay.Login
                                                bottomBarVisible = false
                                            },
                                            onLogout = {
                                                AuthRepository.logout()
                                                softAuth.clearLocalSession()
                                            },
                                            onOpenSettings = {
                                                openMineRoute(MineRoutes.Settings)
                                            },
                                            onOpenPersonalized = {
                                                islandRoute = MineIslandRoute.Personalized
                                                overlay = ShellOverlay.MineIsland
                                                bottomBarVisible = false
                                            },
                                            onDeferred = { msg -> openDeferred(msg) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class AuthOverlay { None, Login, LoginPassword, LoginOtp, Register }
private enum class ShellOverlay {
    None,
    MineIsland,
    UnmappedEntry,
    AllServices,
    Membership,
    InAppWeb,
    Scan,
    HomeRoute,
    CommunityRoute,
    MineRoute,
    ContentRoute,
}

@Composable
private fun JetpackUnmappedEntry(title: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .statusBarsPadding()
            .padding(DesignTokens.Spacing.Lg.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = DemoColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Text("未映射入口（out-of-scope / 未登记）", color = DemoColors.TextSecondary, fontSize = 15.sp)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Primary)) {
            Text("返回")
        }
    }
}
