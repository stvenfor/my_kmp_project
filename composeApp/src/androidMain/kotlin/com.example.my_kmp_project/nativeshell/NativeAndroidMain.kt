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
import androidx.compose.ui.Alignment
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
import com.example.my_kmp_project.core.router.AppRoutes
import com.example.my_kmp_project.core.router.DeepLinkRouter
import com.example.my_kmp_project.core.router.MainTab
import com.example.my_kmp_project.feature.auth.AuthGate
import com.example.my_kmp_project.feature.auth.AuthRepository
import com.example.my_kmp_project.feature.auth.LoginScreen
import com.example.my_kmp_project.feature.auth.RegisterScreen
import com.example.my_kmp_project.feature.commerce.MembershipScreen
import com.example.my_kmp_project.feature.community.CommunityRouteHost
import com.example.my_kmp_project.feature.community.CommunityRoutes
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
import com.example.my_kmp_project.feature.commerce.MembershipScreen

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
    var webUrl by remember { mutableStateOf("https://example.com") }
    var homeRoute by remember { mutableStateOf<String?>(null) }
    var homeRouteStack by remember { mutableStateOf<List<String>>(emptyList()) }
    var communityRoute by remember { mutableStateOf<String?>(null) }
    var communityPreviewUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var communityPreviewIndex by remember { mutableStateOf(0) }
    var communityVideoUrl by remember { mutableStateOf<String?>(null) }
    var mineRoute by remember { mutableStateOf<String?>(null) }
    var mineRouteStack by remember { mutableStateOf<List<String>>(emptyList()) }
    var islandRoute by remember { mutableStateOf(MineIslandRoute.Settings) }
    var deferredTitle by remember { mutableStateOf("后续开放") }
    var bottomBarVisible by remember { mutableStateOf(true) }
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

    fun openDeferred(title: String) {
        val homeMapped = HomeRoutes.fromLabel(title)
        val communityMapped = CommunityRoutes.fromLabel(title)
        val mineMapped = MineRoutes.fromLabel(title)
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
                webUrl = if (title.startsWith("http")) title else "https://example.com"
                overlay = ShellOverlay.InAppWeb
                bottomBarVisible = false
            }
            mineMapped != null -> {
                if (mineMapped == HomeRoutes.UsedCar || mineMapped == HomeRoutes.CheckInMall) {
                    openHomeRoute(mineMapped)
                } else {
                    openMineRoute(mineMapped)
                }
            }
            communityMapped != null -> openCommunityRoute(communityMapped)
            homeMapped != null -> openHomeRoute(homeMapped)
            else -> {
                deferredTitle = title
                overlay = ShellOverlay.DeferredStub
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
        overlay = ShellOverlay.None
        bottomBarVisible = authOverlay == AuthOverlay.None
    }

    LaunchedEffect(Unit) {
        val pending = DeepLinkRouter.consumePending() ?: return@LaunchedEffect
        when {
            pending.route == AppRoutes.Auth.LOGIN -> {
                authOverlay = AuthOverlay.Login
                bottomBarVisible = false
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

    when (overlay) {
        ShellOverlay.MineIsland -> {
            MineIsland(
                initialRoute = islandRoute,
                onRequestClose = { closeOverlay() },
            )
        }
        ShellOverlay.AllServices -> {
            AllServicesScreen(onBack = { closeOverlay() })
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
        ShellOverlay.DeferredStub -> {
            JetpackDeferredStub(
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
                                        )
                                        MainTab.Chat -> JetpackChatRoot()
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
                                                islandRoute = MineIslandRoute.Settings
                                                overlay = ShellOverlay.MineIsland
                                                bottomBarVisible = false
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

private enum class AuthOverlay { None, Login, Register }
private enum class ShellOverlay {
    None,
    MineIsland,
    DeferredStub,
    AllServices,
    Membership,
    InAppWeb,
    Scan,
    HomeRoute,
    CommunityRoute,
    MineRoute,
}

@Composable
private fun JetpackDeferredStub(title: String, onBack: () -> Unit) {
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
        Text("一期后置 · 原生占位", color = DemoColors.TextSecondary, fontSize = 15.sp)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Primary)) {
            Text("返回")
        }
    }
}
