package com.example.my_kmp_project.feature.mine

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.my_kmp_project.core.router.ProductRouteDispatch
import com.example.my_kmp_project.core.router.ProductRouteHost
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import com.example.my_kmp_project.feature.content.ContentRouteHost
import com.example.my_kmp_project.feature.home.HomeRouteHost
import kotlinx.coroutines.delay

@Composable
internal fun MineScreen(
    loggedIn: Boolean,
    displayName: String?,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    var destination by remember { mutableStateOf<String?>(null) }
    var snackMessage by remember { mutableStateOf<String?>(null) }
    val showSnack: (String) -> Unit = { snackMessage = it }

    fun go(raw: String) {
        if (raw == "请先登录") {
            showSnack("请先登录")
            return
        }
        val target = ProductRouteDispatch.resolve(raw) ?: return
        when (target.host) {
            ProductRouteHost.Toast -> showSnack(target.toastMessage ?: raw)
            else -> destination = target.path
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val dest = destination) {
            null -> {
                ReportMainTabRoot(isRoot = true)
                MineHomeContent(
                    loggedIn = loggedIn,
                    displayName = displayName,
                    onLoginClick = onLoginClick,
                    onLogoutClick = onLogoutClick,
                    onOpenSettings = { destination = MineRoutes.Settings },
                    onOpenPersonalized = { destination = MineRoutes.PersonalizedSettings },
                    onNavigate = ::go,
                    snackbar = showSnack,
                )
            }
            else -> {
                ReportMainTabRoot(isRoot = false)
                if (dest == "/mine/about") {
                    MineAboutScreen(onBack = { destination = MineRoutes.Settings })
                } else {
                    when (ProductRouteDispatch.hostForPath(dest)) {
                        ProductRouteHost.Home -> HomeRouteHost(
                            route = dest,
                            onBack = { destination = null },
                            onNavigate = ::go,
                        )
                        ProductRouteHost.Content -> ContentRouteHost(
                            route = dest,
                            onBack = { destination = null },
                            onNavigate = ::go,
                        )
                        else -> MineRouteHost(
                            route = dest,
                            onBack = {
                                destination = when (dest) {
                                    MineRoutes.AddressEdit -> MineRoutes.Addresses
                                    MineRoutes.PersonalizedSettings -> null
                                    MineRoutes.Settings, MineRoutes.SettingsLegacy -> null
                                    else -> null
                                }
                            },
                            onNavigate = { next ->
                                when (next) {
                                    "logout" -> {
                                        destination = null
                                        onLogoutClick()
                                    }
                                    else -> go(next)
                                }
                            },
                        )
                    }
                }
            }
        }

        val message = snackMessage
        if (message != null) {
            LaunchedEffect(message) {
                delay(1800)
                if (snackMessage == message) snackMessage = null
            }
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            ) {
                Text(text = message)
            }
        }
    }
}
