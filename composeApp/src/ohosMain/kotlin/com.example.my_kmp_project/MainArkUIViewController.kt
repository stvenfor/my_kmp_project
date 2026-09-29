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
import kotlinx.cinterop.set
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
 * kind: 0 = Mine island
 *   routeCode: 0=settings, 1=personalized, 2=membership, 3=about,
 *   4=profile, 5=addresses, 6=calculator, 7=deal_invoice, 8=deal_invoice_upload,
 *   9=address_edit
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
            3 -> MineIslandRoute.About
            4 -> MineIslandRoute.Profile
            5 -> MineIslandRoute.Addresses
            6 -> MineIslandRoute.Calculator
            7 -> MineIslandRoute.DealInvoiceDemo
            8 -> MineIslandRoute.DealInvoiceUpload
            9 -> MineIslandRoute.AddressEdit
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
 * Sync ArkTS display-only session into shared [AccountFacade] (e.g. cold-start
 * shell cache). Prefer [KnAuthLogin*] / Huawei BFF via Kotlin for new logins.
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

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
private fun copyUtf8(
    value: String,
    buf: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?,
    cap: Int,
): Int {
    if (buf == null || cap <= 0) return -1
    val bytes = value.encodeToByteArray()
    val n = minOf(bytes.size, cap - 1)
    var i = 0
    while (i < n) {
        buf[i] = bytes[i]
        i++
    }
    buf[n] = 0
    return n
}

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
private fun ensureOhosAuthReady() {
    com.example.my_kmp_project.core.network.platformNetworkBootstrap()
}

/** 0 = success, 1 = failure (message in errBuf). Blocking — call from NAPI async work only. */
@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName("KnAuthLoginWithPasswordBlocking")
fun KnAuthLoginWithPasswordBlocking(
    accountPtr: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?,
    passwordPtr: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?,
    errBuf: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?,
    errCap: Int,
): Int {
    ensureOhosAuthReady()
    val result = kotlinx.coroutines.runBlocking {
        com.example.my_kmp_project.feature.auth.AuthRepository.loginWithPassword(
            accountPtr?.toKString().orEmpty(),
            passwordPtr?.toKString().orEmpty(),
        )
    }
    return result.fold(
        onSuccess = { 0 },
        onFailure = {
            copyUtf8(it.message ?: "登录失败，请稍后重试", errBuf, errCap)
            1
        },
    )
}

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName("KnAuthLoginWithOtpBlocking")
fun KnAuthLoginWithOtpBlocking(
    phonePtr: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?,
    codePtr: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?,
    errBuf: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?,
    errCap: Int,
): Int {
    ensureOhosAuthReady()
    val result = kotlinx.coroutines.runBlocking {
        com.example.my_kmp_project.feature.auth.AuthRepository.loginWithOtp(
            phonePtr?.toKString().orEmpty(),
            codePtr?.toKString().orEmpty(),
        )
    }
    return result.fold(
        onSuccess = { 0 },
        onFailure = {
            copyUtf8(it.message ?: "登录失败，请稍后重试", errBuf, errCap)
            1
        },
    )
}

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName("KnAuthSendPhoneOtpBlocking")
fun KnAuthSendPhoneOtpBlocking(
    phonePtr: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?,
    errBuf: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?,
    errCap: Int,
): Int {
    ensureOhosAuthReady()
    val result = kotlinx.coroutines.runBlocking {
        com.example.my_kmp_project.feature.auth.AuthRepository.sendPhoneOtp(
            phonePtr?.toKString().orEmpty(),
        )
    }
    return result.fold(
        onSuccess = { 0 },
        onFailure = {
            copyUtf8(it.message ?: "发送失败", errBuf, errCap)
            1
        },
    )
}

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName("KnAuthRegisterBlocking")
fun KnAuthRegisterBlocking(
    emailPtr: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?,
    passwordPtr: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?,
    displayNamePtr: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?,
    errBuf: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?,
    errCap: Int,
): Int {
    ensureOhosAuthReady()
    val result = kotlinx.coroutines.runBlocking {
        com.example.my_kmp_project.feature.auth.AuthRepository.register(
            emailPtr?.toKString().orEmpty(),
            passwordPtr?.toKString().orEmpty(),
            displayNamePtr?.toKString().orEmpty(),
        )
    }
    return result.fold(
        onSuccess = { 0 },
        onFailure = {
            copyUtf8(it.message ?: "注册失败", errBuf, errCap)
            1
        },
    )
}

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName("KnAuthLoginWithHuaweiBlocking")
fun KnAuthLoginWithHuaweiBlocking(
    codePtr: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?,
    deviceIdPtr: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?,
    errBuf: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?,
    errCap: Int,
): Int {
    ensureOhosAuthReady()
    val result = kotlinx.coroutines.runBlocking {
        com.example.my_kmp_project.feature.auth.AuthRepository.loginWithHuaweiCode(
            codePtr?.toKString().orEmpty(),
            deviceIdPtr?.toKString().orEmpty(),
        )
    }
    return result.fold(
        onSuccess = { 0 },
        onFailure = {
            copyUtf8(it.message ?: "登录失败", errBuf, errCap)
            1
        },
    )
}

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName("KnAuthCopyToken")
fun KnAuthCopyToken(
    buf: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?,
    cap: Int,
): Int = copyUtf8(com.example.my_kmp_project.feature.auth.AuthBridge.token(), buf, cap)

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName("KnAuthCopyDisplayName")
fun KnAuthCopyDisplayName(
    buf: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?,
    cap: Int,
): Int = copyUtf8(com.example.my_kmp_project.feature.auth.AuthBridge.displayName(), buf, cap)

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName("KnAuthCopyUserId")
fun KnAuthCopyUserId(
    buf: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?,
    cap: Int,
): Int = copyUtf8(com.example.my_kmp_project.feature.auth.AuthBridge.userId(), buf, cap)

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName("KnAuthCopyPhone")
fun KnAuthCopyPhone(
    buf: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?,
    cap: Int,
): Int = copyUtf8(com.example.my_kmp_project.feature.auth.AuthBridge.phone(), buf, cap)

@OptIn(ExperimentalNativeApi::class)
@CName("KnAuthIsLoggedIn")
fun KnAuthIsLoggedIn(): Int =
    if (com.example.my_kmp_project.feature.auth.AuthBridge.isLoggedIn()) 1 else 0

@OptIn(ExperimentalNativeApi::class)
@CName("KnAuthLogout")
fun KnAuthLogout() {
    ensureOhosAuthReady()
    com.example.my_kmp_project.feature.auth.AuthBridge.logout()
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
