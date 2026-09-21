package com.example.my_kmp_project.nativeshell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.app.AppContainer
import com.example.my_kmp_project.core.account.AccountFacade
import com.example.my_kmp_project.core.account.LoggedInUser
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.DesignTokens
import com.example.my_kmp_project.core.design.ImmersiveInsets.MainBottomBarHeight
import com.example.my_kmp_project.core.design.ImmersiveInsets.shellContentInsets
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.TokenExpiredHandler
import com.example.my_kmp_project.core.router.MainTab
import com.example.my_kmp_project.feature.auth.AuthRepository
import com.example.my_kmp_project.feature.auth.AuthSessionState
import com.example.my_kmp_project.feature.mine.MineIsland
import com.example.my_kmp_project.feature.mine.MineIslandRoute
import com.example.my_kmp_project.feature.shell.SoftAuthPresenter

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
        NetworkFacade.setTokenExpiredHandler(
            TokenExpiredHandler {
                AuthRepository.logout()
                softAuth.clearLocalSession()
            },
        )
        onDispose { NetworkFacade.setTokenExpiredHandler(null) }
    }

    var tab by remember { mutableStateOf(MainTab.Home) }
    var authOverlay by remember { mutableStateOf(AuthOverlay.None) }
    var overlay by remember { mutableStateOf(ShellOverlay.None) }
    var islandRoute by remember { mutableStateOf(MineIslandRoute.Settings) }
    var deferredTitle by remember { mutableStateOf("后续开放") }
    var bottomBarVisible by remember { mutableStateOf(true) }
    val authState by softAuth.uiState.collectAsState()

    fun selectTab(next: MainTab) {
        if (softAuth.trySelectTab(next)) {
            tab = next
            authOverlay = AuthOverlay.None
            bottomBarVisible = true
        } else {
            authOverlay = AuthOverlay.Login
            bottomBarVisible = false
        }
    }

    fun openDeferred(title: String) {
        deferredTitle = title
        overlay = ShellOverlay.DeferredStub
        bottomBarVisible = false
    }

    when (overlay) {
        ShellOverlay.MineIsland -> {
            MineIsland(
                initialRoute = islandRoute,
                onRequestClose = {
                    overlay = ShellOverlay.None
                    bottomBarVisible = true
                },
            )
        }
        ShellOverlay.DeferredStub -> {
            JetpackDeferredStub(
                title = deferredTitle,
                onBack = {
                    overlay = ShellOverlay.None
                    bottomBarVisible = true
                },
            )
        }
        ShellOverlay.None -> when (authOverlay) {
            AuthOverlay.Login -> JetpackLogin(
                onBack = {
                    softAuth.dismissGate()
                    authOverlay = AuthOverlay.None
                    tab = MainTab.Home
                    bottomBarVisible = true
                },
                onSuccess = {
                    tab = softAuth.onLoginSucceeded()
                    authOverlay = AuthOverlay.None
                    bottomBarVisible = true
                },
            )
            AuthOverlay.None -> {
                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0),
                    bottomBar = {
                        if (bottomBarVisible) {
                            JetpackBottomBar(selected = tab, onSelect = { selectTab(it) })
                        }
                    },
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .shellContentInsets(
                                bottomBarVisible = bottomBarVisible,
                                bottomBarPadding = MainBottomBarHeight,
                            ),
                    ) {
                        when (tab) {
                            MainTab.Home -> JetpackHomeRoot(onDeferred = { openDeferred(it) })
                            MainTab.Chat -> JetpackChatRoot()
                            MainTab.Community -> JetpackCommunityRoot()
                            MainTab.Mine -> JetpackMineRoot(
                                loggedIn = authState.isLoggedIn,
                                onLogin = {
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
                                onDeferred = { openDeferred(it) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class AuthOverlay { None, Login }
private enum class ShellOverlay { None, MineIsland, DeferredStub }

@Composable
private fun JetpackBottomBar(selected: MainTab, onSelect: (MainTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(MainBottomBarHeight)
            .background(DemoColors.Toolbar.copy(alpha = 0.95f))
            .border(width = 0.5.dp, color = DemoColors.Divider),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            MainTab.Home to "首页",
            MainTab.Chat to "聊天",
            MainTab.Community to "社区",
            MainTab.Mine to "我的",
        ).forEach { (tab, label) ->
            val active = selected == tab
            Text(
                text = label,
                color = if (active) DemoColors.Accent else DemoColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(tab) }
                    .padding(vertical = 12.dp),
            )
        }
    }
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

@Composable
private fun JetpackLogin(onBack: () -> Unit, onSuccess: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("登录", color = DemoColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "演示登录（共享会话规则由 SoftAuthPresenter 驱动）",
            color = DemoColors.TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                AccountFacade.setSession(
                    LoggedInUser(
                        displayName = "演示用户",
                        token = "demo-token",
                        userId = "demo",
                    ),
                )
                AuthSessionState.sync()
                onSuccess()
            },
            colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Primary),
        ) { Text("登录") }
        TextButton(onClick = onBack) { Text("关闭", color = DemoColors.TextSecondary) }
    }
}

