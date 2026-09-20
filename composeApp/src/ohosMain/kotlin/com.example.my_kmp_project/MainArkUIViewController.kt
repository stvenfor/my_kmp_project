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
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.initMainHandler
import platform.ArkTS.ArkTS_Napi_NativeModule.napi_env
import platform.ArkTS.ArkTS_Napi_NativeModule.napi_value
import kotlin.experimental.ExperimentalNativeApi

/** Set true to verify ComposeArkUIViewController + NAPI without product UI. */
private const val OhosSmokeUiOnly: Boolean = false

/**
 * Mine Compose Island host for Harmony Native Shell (ADR 0002).
 * ArkTS owns splash/tabs/Mine Root; this controller is pushed only for secondary Mine pages.
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
                        initialRoute = MineIslandRoute.Settings,
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

@OptIn(ExperimentalNativeApi::class)
@CName("KnAudioOnPageHide")
fun KnAudioOnPageHide() {
    // Demo: no media playlist lifecycle.
}

@Composable
private fun OhosSmokeRoot() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text("KMP Demo OHOS Compose OK")
    }
}
