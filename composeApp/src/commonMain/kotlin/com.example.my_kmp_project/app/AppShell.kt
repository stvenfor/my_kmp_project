package com.example.my_kmp_project.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.account.AccountFacade
import com.example.my_kmp_project.core.account.createPrivacyConsentStore
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.ImmersiveInsets.shellContentInsets
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.TokenExpiredHandler
import com.example.my_kmp_project.core.router.AppRoute
import com.example.my_kmp_project.core.router.AppRoutes
import com.example.my_kmp_project.core.router.DeepLinkRouter
import com.example.my_kmp_project.core.router.LocalAppNavigator
import com.example.my_kmp_project.core.router.MainTab
import com.example.my_kmp_project.core.ui.LocalMainTabChrome
import com.example.my_kmp_project.core.ui.MainTabChromeController
import com.example.my_kmp_project.feature.auth.AuthGate
import com.example.my_kmp_project.feature.auth.AuthRepository
import com.example.my_kmp_project.feature.auth.AuthSessionState
import com.example.my_kmp_project.feature.auth.LoginScreen
import com.example.my_kmp_project.feature.auth.RegisterScreen
import com.example.my_kmp_project.feature.chat.ChatScreen
import com.example.my_kmp_project.feature.classroom.ClassroomScreen
import com.example.my_kmp_project.feature.commerce.MembershipScreen
import com.example.my_kmp_project.feature.community.CommunityScreen
import com.example.my_kmp_project.feature.friend.FriendScreen
import com.example.my_kmp_project.feature.home.AllServicesScreen
import com.example.my_kmp_project.feature.home.HomeScreen
import com.example.my_kmp_project.feature.live.LiveScreen
import com.example.my_kmp_project.feature.media.MediaEntryScreen
import com.example.my_kmp_project.feature.mine.MineScreen
import com.example.my_kmp_project.feature.scan.ScanScreen
import com.example.my_kmp_project.feature.shell.MainBottomBar
import com.example.my_kmp_project.feature.web.InAppWebScreen
import kotlinx.coroutines.delay
import my_kmp_project.composeapp.generated.resources.Res
import my_kmp_project.composeapp.generated.resources.bg_splash
import my_kmp_project.composeapp.generated.resources.ic_splash_logo
import org.jetbrains.compose.resources.painterResource

private enum class AppPhase {
    Splash,
    PrivacyConsent,
    Main,
}

private enum class AuthOverlay {
    None,
    Login,
    Register,
}

/**
 * Product shell: splash → privacy consent → four-tab main chrome with soft-auth gate.
 */
@Composable
internal fun AppShell() {
    val privacyConsentStore = remember { createPrivacyConsentStore() }
    var phase by remember { mutableStateOf(AppPhase.Splash) }
    var privacyAccepted by remember { mutableStateOf(privacyConsentStore.isAccepted()) }

    DisposableEffect(Unit) {
        AuthSessionState.sync()
        NetworkFacade.setTokenExpiredHandler(
            TokenExpiredHandler {
                AuthRepository.logout()
            },
        )
        onDispose {
            NetworkFacade.setTokenExpiredHandler(null)
        }
    }

    when (phase) {
        AppPhase.Splash -> {
            SplashPhase(
                onFinished = {
                    phase = if (privacyAccepted) AppPhase.Main else AppPhase.PrivacyConsent
                },
            )
        }
        AppPhase.PrivacyConsent -> {
            PrivacyConsentPhase(
                onAccept = {
                    privacyConsentStore.setAccepted(true)
                    privacyAccepted = true
                    phase = AppPhase.Main
                },
            )
        }
        AppPhase.Main -> MainShell()
    }
}

@Composable
private fun SplashPhase(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1_000)
        onFinished()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(Res.drawable.bg_splash),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .statusBarsPadding()
                .padding(24.dp),
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_splash_logo),
                contentDescription = "App logo",
                modifier = Modifier.size(96.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "正在启动…",
                fontSize = 14.sp,
                color = DemoColors.TextSecondary,
            )
            Spacer(modifier = Modifier.height(20.dp))
            TextButton(onClick = onFinished) {
                Text("进入", color = DemoColors.Primary)
            }
        }
    }
}

@Composable
private fun PrivacyConsentPhase(onAccept: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "隐私政策与用户协议",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = DemoColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "为保障推送、链接跳转等基础服务，我们需要在您同意后初始化相关能力。" +
                "您可在「设置」中查看完整隐私政策。拒绝将无法继续使用本应用。",
            fontSize = 14.sp,
            color = DemoColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(28.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                onClick = { /* stay — Flutter also blocks without grant */ },
                modifier = Modifier.weight(1f),
            ) {
                Text("不同意", color = DemoColors.TextSecondary)
            }
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DemoColors.Primary,
                    contentColor = DemoColors.OnPrimary,
                ),
                modifier = Modifier.weight(1f),
            ) {
                Text("同意并继续")
            }
        }
    }
}

@Composable
private fun MainShell() {
    var tab by remember { mutableStateOf(MainTab.Home) }
    var authOverlay by remember { mutableStateOf(AuthOverlay.None) }
    var shellRoute by remember { mutableStateOf<AppRoute?>(null) }
    val tabChrome = remember { MainTabChromeController() }
    var sessionEpoch by remember { mutableStateOf(0) }
    val loggedIn = remember(sessionEpoch) { AccountFacade.current().isLoggedIn }

    fun clearShellRoute() {
        // Membership overlays Mine Settings (secondary); web/services return to Home root.
        val restoreBottomBar = shellRoute != AppRoute.Membership
        shellRoute = null
        tabChrome.updateBottomBarVisible(restoreBottomBar)
    }

    val navigator = remember {
        AppContainer.get().navigation.bind { route ->
            when (route) {
                is AppRoute.InAppWeb,
                AppRoute.AllServices,
                AppRoute.Membership,
                AppRoute.Scan,
                AppRoute.Media,
                AppRoute.Live,
                AppRoute.Friend,
                AppRoute.Classroom,
                -> {
                    shellRoute = route
                    authOverlay = AuthOverlay.None
                    tabChrome.updateBottomBarVisible(false)
                }
                else -> Unit
            }
        }
    }

    LaunchedEffect(Unit) {
        val pending = DeepLinkRouter.consumePending() ?: return@LaunchedEffect
        when {
            pending.route == AppRoutes.Auth.LOGIN -> {
                authOverlay = AuthOverlay.Login
                tabChrome.updateBottomBarVisible(false)
            }
            pending.tab != null -> {
                // Soft-auth still applies via selectTab when user can interact;
                // apply tab if public, otherwise stash for login.
                val target = pending.tab
                if (AuthGate.requiresAuth(target) && !AccountFacade.current().isLoggedIn) {
                    AuthGate.rememberPending(target)
                    authOverlay = AuthOverlay.Login
                    tabChrome.updateBottomBarVisible(false)
                } else {
                    tab = target
                }
            }
        }
    }

    fun refreshSession() {
        AuthSessionState.sync()
        sessionEpoch += 1
    }

    fun selectTab(next: MainTab) {
        if (AuthGate.requiresAuth(next) && !AccountFacade.current().isLoggedIn) {
            AuthGate.rememberPending(next)
            shellRoute = null
            authOverlay = AuthOverlay.Login
            tabChrome.updateBottomBarVisible(false)
            return
        }
        tab = next
        shellRoute = null
        authOverlay = AuthOverlay.None
        AuthGate.clearPending()
        tabChrome.updateBottomBarVisible(true)
    }

    fun afterAuthSuccess() {
        refreshSession()
        val resume = AuthGate.consumePending() ?: MainTab.Home
        shellRoute = null
        authOverlay = AuthOverlay.None
        tab = resume
        tabChrome.updateBottomBarVisible(true)
    }

    val shellOverlayActive = shellRoute != null
    val showBottomBar = authOverlay == AuthOverlay.None &&
        !shellOverlayActive &&
        tabChrome.bottomBarVisible

    CompositionLocalProvider(
        LocalMainTabChrome provides tabChrome,
        LocalAppNavigator provides navigator,
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(DemoColors.Background),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showBottomBar) {
                    MainBottomBar(
                        selected = tab,
                        onSelect = { next -> selectTab(next) },
                    )
                }
            },
            content = { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shellContentInsets(
                            bottomBarVisible = showBottomBar,
                            bottomBarPadding = paddingValues.calculateBottomPadding(),
                        ),
                ) {
                    // Keep tab content under shell overlays so Mine local pages survive Membership.
                    when (authOverlay) {
                        AuthOverlay.Login -> LoginScreen(
                            onLoginSuccess = { afterAuthSuccess() },
                            onOpenRegister = { authOverlay = AuthOverlay.Register },
                            onBack = {
                                authOverlay = AuthOverlay.None
                                AuthGate.clearPending()
                                tabChrome.updateBottomBarVisible(true)
                            },
                        )
                        AuthOverlay.Register -> RegisterScreen(
                            onRegistered = { afterAuthSuccess() },
                            onBack = { authOverlay = AuthOverlay.Login },
                        )
                        AuthOverlay.None -> when (tab) {
                            MainTab.Home -> HomeScreen()
                            MainTab.Chat -> ChatScreen()
                            MainTab.Community -> CommunityScreen()
                            MainTab.Mine -> MineScreen(
                                loggedIn = loggedIn,
                                displayName = AccountFacade.current().displayName,
                                onLoginClick = {
                                    AuthGate.rememberPending(MainTab.Mine)
                                    shellRoute = null
                                    authOverlay = AuthOverlay.Login
                                    tabChrome.updateBottomBarVisible(false)
                                },
                                onLogoutClick = {
                                    AuthRepository.logout()
                                    refreshSession()
                                    shellRoute = null
                                    tab = MainTab.Home
                                    tabChrome.updateBottomBarVisible(true)
                                },
                            )
                        }
                    }

                    when (val route = shellRoute) {
                        is AppRoute.InAppWeb -> InAppWebScreen(
                            url = route.url,
                            onBack = { clearShellRoute() },
                        )
                        AppRoute.AllServices -> AllServicesScreen(
                            onBack = { clearShellRoute() },
                        )
                        AppRoute.Membership -> MembershipScreen(
                            onBack = { clearShellRoute() },
                        )
                        AppRoute.Scan -> ScanScreen(
                            onBack = { clearShellRoute() },
                        )
                        AppRoute.Media -> MediaEntryScreen(
                            onBack = { clearShellRoute() },
                        )
                        AppRoute.Live -> LiveScreen(
                            onBack = { clearShellRoute() },
                        )
                        AppRoute.Friend -> FriendScreen(
                            onBack = { clearShellRoute() },
                        )
                        AppRoute.Classroom -> ClassroomScreen(
                            onBack = { clearShellRoute() },
                        )
                        else -> Unit
                    }
                }
            },
        )
    }
}
