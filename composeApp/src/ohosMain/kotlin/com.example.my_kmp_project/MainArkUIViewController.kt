package com.example.my_kmp_project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeArkUIViewController
import com.example.my_kmp_project.feature.mine.MineIsland
import com.example.my_kmp_project.feature.mine.MineIslandRoute
import com.example.my_kmp_project.feature.shell.OhosComposeHostRequest
import com.example.my_kmp_project.feature.shell.SecondaryRouteIsland
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.coroutines.initMainHandler
import platform.ArkTS.ArkTS_Napi_NativeModule.napi_env
import platform.ArkTS.ArkTS_Napi_NativeModule.napi_value
import kotlin.experimental.ExperimentalNativeApi

private const val OhosSmokeUiOnly: Boolean = false

/**
 * ADR 0002: Harmony Compose host — Mine island + secondary RoutePath screens.
 * ArkTS owns splash / tabs / Mine root / auth.
 */
@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName("MainArkUIViewController")
fun MainArkUIViewController(env: napi_env): napi_value {
    return try {
        println("DemoKN: MainArkUIViewController enter mode=${OhosComposeHostRequest.mode}")
        initMainHandler(env)
        if (!OhosSmokeUiOnly) {
            com.example.my_kmp_project.core.network.platformNetworkBootstrap()
        }
        ComposeArkUIViewController(env) {
            if (OhosSmokeUiOnly) {
                OhosSmokeRoot()
            } else {
                var closed by remember { mutableStateOf(false) }
                if (!closed) {
                    when (OhosComposeHostRequest.mode) {
                        1 -> SecondaryRouteIsland(
                            initialRoute = OhosComposeHostRequest.secondaryRoute,
                            onRequestClose = { closed = true },
                        )
                        else -> MineIsland(
                            initialRoute = OhosComposeHostRequest.mineRoute,
                            onRequestClose = { closed = true },
                        )
                    }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("已关闭")
                    }
                }
            }
        }
    } catch (t: Throwable) {
        println("DemoKN: MainArkUIViewController FAILED: ${t.message}")
        println(t.stackTraceToString())
        null as napi_value
    }
}

/**
 * kind: 0 = Mine island (routeCode 0=settings, 1=personalized, 2=membership)
 * kind: 1 = Secondary route — [routeCode] ignored; use [KnSetOhosSecondaryRoute] for path.
 */
@OptIn(ExperimentalNativeApi::class)
@CName("KnSetOhosHost")
fun KnSetOhosHost(kind: Int, routeCode: Int) {
    OhosComposeHostRequest.mode = kind
    if (kind == 0) {
        OhosComposeHostRequest.mineRoute = when (routeCode) {
            1 -> MineIslandRoute.Personalized
            2 -> MineIslandRoute.Membership
            else -> MineIslandRoute.Settings
        }
    }
}

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName("KnSetOhosSecondaryRoute")
fun KnSetOhosSecondaryRoute(routePtr: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?) {
    val route = routePtr?.toKString() ?: "/home/search"
    OhosComposeHostRequest.mode = 1
    OhosComposeHostRequest.secondaryRoute = route
}

/**
 * Sync ArkTS [AuthSession] into shared [AccountFacade] so Mine island / secondary
 * Compose hosts see the same login as the native shell.
 */
@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName("KnApplyAuthSession")
fun KnApplyAuthSession(
    tokenPtr: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?,
    userIdPtr: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?,
    displayNamePtr: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?,
    phonePtr: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?,
) {
    val token = tokenPtr?.toKString().orEmpty()
    if (token.isBlank()) return
    com.example.my_kmp_project.core.network.platformNetworkBootstrap()
    com.example.my_kmp_project.feature.auth.AuthBridge.applySession(
        token = token,
        userId = userIdPtr?.toKString().orEmpty(),
        displayName = displayNamePtr?.toKString().orEmpty().ifBlank { "用户" },
        phone = phonePtr?.toKString()?.takeIf { it.isNotBlank() },
    )
}

@OptIn(ExperimentalNativeApi::class)
@CName("KnAudioOnPageHide")
fun KnAudioOnPageHide() = Unit

@Composable
private fun OhosSmokeRoot() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("KMP Demo OHOS Compose OK")
    }
}
