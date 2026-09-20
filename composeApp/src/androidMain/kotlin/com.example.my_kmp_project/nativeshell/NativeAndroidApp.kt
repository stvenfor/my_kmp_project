package com.example.my_kmp_project.nativeshell

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.app.AppContainer
import com.example.my_kmp_project.core.account.createPrivacyConsentStore
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.DesignTokens
import com.example.my_kmp_project.core.design.ImmersiveInsets.MainBottomBarHeight
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
import com.example.my_kmp_project.feature.auth.AuthRepository
import com.example.my_kmp_project.feature.auth.LoginScreen
import com.example.my_kmp_project.feature.auth.RegisterScreen
import com.example.my_kmp_project.feature.chat.ChatScreen
import com.example.my_kmp_project.feature.community.CommunityScreen
import com.example.my_kmp_project.feature.home.HomeScreen
import com.example.my_kmp_project.feature.mine.MineHomeContent
import com.example.my_kmp_project.feature.mine.MineIsland
import com.example.my_kmp_project.feature.mine.MineIslandRoute
import com.example.my_kmp_project.feature.shell.DeferredDestinationStub
import com.example.my_kmp_project.feature.shell.MainBottomBar
import com.example.my_kmp_project.feature.shell.SoftAuthPresenter
import kotlinx.coroutines.delay
import my_kmp_project.composeapp.generated.resources.Res
import my_kmp_project.composeapp.generated.resources.bg_splash
import my_kmp_project.composeapp.generated.resources.ic_splash_logo
import org.jetbrains.compose.resources.painterResource

private enum class NativePhase { Splash, Privacy, Main }

private enum class AuthOverlay { None, Login, Register }

private enum class AndroidOverlay {
    None,
    MineIsland,
    DeferredStub,
}

/**
 * Android Native Shell UI (Jetpack Compose in androidMain) — ADR 0002.
 * Mine secondary flows open [MineIsland] inside a full-screen container overlay.
 */
@Composable
fun NativeAndroidApp() {
    AppContainer.get()
    val privacyStore = remember { createPrivacyConsentStore() }
    var phase by remember { mutableStateOf(NativePhase.Splash) }
    var privacyAccepted by remember { mutableStateOf(privacyStore.isAccepted()) }
    val softAuth = remember { SoftAuthPresenter() }

    DisposableEffect(Unit) {
        softAuth.sync()
        NetworkFacade.setTokenExpiredHandler(
            TokenExpiredHandler {
                AuthRepository.logout()
                softAuth.clearLocalSession()
            },
        )
        onDispose { NetworkFacade.setTokenExpiredHandler(null) }
    }

    when (phase) {
        NativePhase.Splash -> NativeSplash {
            phase = if (privacyAccepted) NativePhase.Main else NativePhase.Privacy
        }
        NativePhase.Privacy -> NativePrivacy(
            onAccept = {
                privacyStore.setAccepted(true)
                privacyAccepted = true
                phase = NativePhase.Main
            },
        )
        NativePhase.Main -> NativeMainScaffold(softAuth = softAuth)
    }
}

@Composable
private fun NativeSplash(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1200)
        onFinished()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(Res.drawable.bg_splash),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Image(
            painter = painterResource(Res.drawable.ic_splash_logo),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(96.dp),
        )
    }
}

@Composable
private fun NativePrivacy(onAccept: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .statusBarsPadding()
            .padding(DesignTokens.Spacing.Lg.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "隐私政策",
            color = DemoColors.TextPrimary,
            fontSize = DesignTokens.Typography.DisplayMdSp.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(DesignTokens.Spacing.Md.dp))
        Text(
            text = "请阅读并同意隐私政策后继续使用本应用。",
            color = DemoColors.TextSecondary,
            fontSize = DesignTokens.Typography.BodyMdSp.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(DesignTokens.Spacing.Lg.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.Md.dp)) {
            TextButton(onClick = { /* Flutter blocks without grant */ }) {
                Text("不同意", color = DemoColors.TextSecondary)
            }
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Primary),
            ) {
                Text("同意并继续")
            }
        }
    }
}

@Composable
private fun NativeMainScaffold(softAuth: SoftAuthPresenter) {
    var tab by remember { mutableStateOf(MainTab.Home) }
    var authOverlay by remember { mutableStateOf(AuthOverlay.None) }
    var overlay by remember { mutableStateOf(AndroidOverlay.None) }
    var islandRoute by remember { mutableStateOf(MineIslandRoute.Settings) }
    var stubTitle by remember { mutableStateOf("后续开放") }
    val tabChrome = remember { MainTabChromeController() }
    val authState by softAuth.uiState.collectAsState()

    fun selectTab(next: MainTab) {
        if (softAuth.trySelectTab(next)) {
            tab = next
            authOverlay = AuthOverlay.None
            tabChrome.updateBottomBarVisible(true)
        } else {
            authOverlay = AuthOverlay.Login
            tabChrome.updateBottomBarVisible(false)
        }
    }

    val navigator = remember {
        AppContainer.get().navigation.bind { route ->
            when (route) {
                is AppRoute.Tab -> selectTab(route.tab)
                AppRoute.Login -> {
                    authOverlay = AuthOverlay.Login
                    tabChrome.updateBottomBarVisible(false)
                }
                AppRoute.Register -> {
                    authOverlay = AuthOverlay.Register
                    tabChrome.updateBottomBarVisible(false)
                }
                AppRoute.Live,
                AppRoute.Classroom,
                AppRoute.Media,
                AppRoute.Friend,
                AppRoute.Membership,
                AppRoute.Scan,
                AppRoute.AllServices,
                is AppRoute.InAppWeb,
                -> {
                    stubTitle = when (route) {
                        AppRoute.Live -> "直播"
                        AppRoute.Classroom -> "课堂"
                        AppRoute.Media -> "短视频"
                        AppRoute.Friend -> "好友"
                        AppRoute.Membership -> "会员"
                        AppRoute.Scan -> "扫一扫"
                        AppRoute.AllServices -> "全部服务"
                        is AppRoute.InAppWeb -> "网页"
                        else -> "后续开放"
                    }
                    overlay = AndroidOverlay.DeferredStub
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
            pending.tab != null -> selectTab(pending.tab)
        }
    }

    CompositionLocalProvider(
        LocalAppNavigator provides navigator,
        LocalMainTabChrome provides tabChrome,
    ) {
        when (overlay) {
            AndroidOverlay.MineIsland -> {
                MineIsland(
                    initialRoute = islandRoute,
                    onRequestClose = {
                        overlay = AndroidOverlay.None
                        tabChrome.updateBottomBarVisible(true)
                    },
                )
            }
            AndroidOverlay.DeferredStub -> {
                DeferredDestinationStub(
                    title = stubTitle,
                    onBack = {
                        overlay = AndroidOverlay.None
                        tabChrome.updateBottomBarVisible(true)
                    },
                )
            }
            AndroidOverlay.None -> {
                when (authOverlay) {
                    AuthOverlay.Login -> LoginScreen(
                        onBack = {
                            softAuth.dismissGate()
                            authOverlay = AuthOverlay.None
                            tab = MainTab.Home
                            tabChrome.updateBottomBarVisible(true)
                        },
                        onLoginSuccess = {
                            tab = softAuth.onLoginSucceeded()
                            authOverlay = AuthOverlay.None
                            tabChrome.updateBottomBarVisible(true)
                        },
                        onOpenRegister = { authOverlay = AuthOverlay.Register },
                    )
                    AuthOverlay.Register -> RegisterScreen(
                        onBack = { authOverlay = AuthOverlay.Login },
                        onRegistered = {
                            tab = softAuth.onLoginSucceeded()
                            authOverlay = AuthOverlay.None
                            tabChrome.updateBottomBarVisible(true)
                        },
                    )
                    AuthOverlay.None -> {
                        Scaffold(
                            containerColor = Color.Transparent,
                            contentWindowInsets = WindowInsets(0),
                            bottomBar = {
                                if (tabChrome.bottomBarVisible) {
                                    MainBottomBar(selected = tab, onSelect = { selectTab(it) })
                                }
                            },
                        ) { padding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding)
                                    .shellContentInsets(
                                        bottomBarVisible = tabChrome.bottomBarVisible,
                                        bottomBarPadding = MainBottomBarHeight,
                                    ),
                            ) {
                                when (tab) {
                                    MainTab.Home -> HomeScreen()
                                    MainTab.Chat -> ChatScreen()
                                    MainTab.Community -> CommunityScreen()
                                    MainTab.Mine -> MineHomeContent(
                                        loggedIn = authState.isLoggedIn,
                                        displayName = authState.displayName,
                                        onLoginClick = {
                                            authOverlay = AuthOverlay.Login
                                            tabChrome.updateBottomBarVisible(false)
                                        },
                                        onLogoutClick = {
                                            AuthRepository.logout()
                                            softAuth.clearLocalSession()
                                        },
                                        onOpenSettings = {
                                            islandRoute = MineIslandRoute.Settings
                                            overlay = AndroidOverlay.MineIsland
                                            tabChrome.updateBottomBarVisible(false)
                                        },
                                        onOpenPersonalized = {
                                            islandRoute = MineIslandRoute.Personalized
                                            overlay = AndroidOverlay.MineIsland
                                            tabChrome.updateBottomBarVisible(false)
                                        },
                                        snackbar = { /* native toast reserved */ },
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
