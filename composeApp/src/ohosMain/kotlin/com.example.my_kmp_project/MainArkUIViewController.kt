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
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.initMainHandler
import platform.ArkTS.ArkTS_Napi_NativeModule.napi_env
import platform.ArkTS.ArkTS_Napi_NativeModule.napi_value
import kotlin.experimental.ExperimentalNativeApi

private const val OhosSmokeUiOnly: Boolean = false

/**
 * ADR 0002: Harmony Compose host is **Mine island only**.
 * ArkTS owns splash / tabs / Mine root / auth / deferred stubs.
 */
@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName("MainArkUIViewController")
fun MainArkUIViewController(env: napi_env): napi_value {
    return try {
        println("DemoKN: MainArkUIViewController (MineIsland) enter")
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
                    MineIsland(
                        initialRoute = OhosComposeHostRequest.mineRoute,
                        onRequestClose = { closed = true },
                    )
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

/** kind ignored for features; routeCode 0=settings, 1=personalized. */
@OptIn(ExperimentalNativeApi::class)
@CName("KnSetOhosHost")
fun KnSetOhosHost(kind: Int, routeCode: Int) {
    OhosComposeHostRequest.mineRoute =
        if (routeCode == 1) MineIslandRoute.Personalized else MineIslandRoute.Settings
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
