package com.example.my_kmp_project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeArkUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.initMainHandler
import platform.ArkTS.ArkTS_Napi_NativeModule.napi_env
import platform.ArkTS.ArkTS_Napi_NativeModule.napi_value
import kotlin.experimental.ExperimentalNativeApi

/** Set true to verify ComposeArkUIViewController + NAPI without DemoApp. */
private const val OhosSmokeUiOnly: Boolean = false

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName("MainArkUIViewController")
fun MainArkUIViewController(env: napi_env): napi_value {
    return try {
        println("DemoKN: MainArkUIViewController enter")
        initMainHandler(env)
        if (!OhosSmokeUiOnly) {
            com.example.my_kmp_project.core.network.platformNetworkBootstrap()
        }
        ComposeArkUIViewController(env) {
            if (OhosSmokeUiOnly) {
                OhosSmokeRoot()
            } else {
                App()
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
