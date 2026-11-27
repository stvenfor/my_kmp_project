package com.example.my_kmp_project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeArkUIViewController
import com.example.my_kmp_project.core.design.DemoTheme
import com.example.my_kmp_project.core.ui.LocalVideoPositionMemory
import com.example.my_kmp_project.feature.mine.MineIsland
import com.example.my_kmp_project.core.router.MineIslandRoute
import com.example.my_kmp_project.feature.shell.OhosComposeHostRequest
import com.example.my_kmp_project.feature.shell.SecondaryRouteIsland
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cstr
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.toKString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.initMainHandler
import kotlinx.coroutines.launch
import platform.ArkTS.ArkTS_Napi_NativeModule.napi_env
import platform.ArkTS.ArkTS_Napi_NativeModule.napi_value
import kotlin.experimental.ExperimentalNativeApi

private const val OhosSmokeUiOnly: Boolean = false

/** App-lifetime background scope for N-API auth so the ArkUI thread never blocks. */
private val OhosAuthScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
            // 幂等：401 静默刷新装机 + 会话水合，须早于 ArkTS 侧业务请求。
            com.example.my_kmp_project.app.ShellBootstrap.install()
        }
        ComposeArkUIViewController(env) {
            DemoTheme {
                CompositionLocalProvider(
                    LocalVideoPositionMemory provides com.example.my_kmp_project.app.AppContainer.get().videoPositions,
                ) {
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
                                    api = com.example.my_kmp_project.app.AppContainer.get().network.api(),
                                )
                            }
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("已关闭")
                            }
                        }
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
    // 幂等：401 静默刷新装机 + 会话水合（ArkTS 任意入口都可能首个触发）。
    com.example.my_kmp_project.app.ShellBootstrap.install()
}

/** Auth operations understood by [KnAuthRunAsync] — keep in sync with the C++ `AuthOp` enum. */
private const val AUTH_OP_PASSWORD = 0
private const val AUTH_OP_OTP = 1
private const val AUTH_OP_SEND_OTP = 2
private const val AUTH_OP_REGISTER = 3
private const val AUTH_OP_HUAWEI = 4
private const val AUTH_OP_LOGIN_WITH_METHOD = 5

/**
 * Non-blocking auth entry for the OHOS N-API layer.
 *
 * Starts the suspend request on [AuthBridge]'s background scope and returns immediately —
 * the ArkUI main thread is never blocked. Completion is reported exactly once through
 * [callback] as `(userData, code, message)`:
 * - `code == 0`: success, `message` is null.
 * - `code != 0`: failure, `message` is a NUL-terminated UTF-8 text owned by this call.
 *
 * [AuthBridge] commits the session into `AccountFacade` before invoking the callback, so
 * ArkTS `sessionFromKotlin()` reads the fresh token as soon as the promise resolves.
 */
@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName("KnAuthRunAsync")
fun KnAuthRunAsync(
    op: Int,
    arg0Ptr: CPointer<ByteVar>?,
    arg1Ptr: CPointer<ByteVar>?,
    arg2Ptr: CPointer<ByteVar>?,
    callback: COpaquePointer?,
    userData: COpaquePointer?,
) {
    val cb = callback?.reinterpret<CFunction<(COpaquePointer?, Int, CPointer<ByteVar>?) -> Unit>>()
    val arg0 = arg0Ptr?.toKString().orEmpty()
    val arg1 = arg1Ptr?.toKString().orEmpty()
    val arg2 = arg2Ptr?.toKString().orEmpty()
    fun reportSuccess() {
        if (cb != null) cb(userData, 0, null)
    }
    fun reportFailure(message: String) {
        val fn = cb ?: return
        val text = message.ifBlank { "登录失败，请稍后重试" }
        memScoped { fn(userData, 1, text.cstr.ptr) }
    }
    OhosAuthScope.launch {
        try {
            ensureOhosAuthReady()
            when (op) {
                AUTH_OP_PASSWORD ->
                    com.example.my_kmp_project.feature.auth.AuthBridge
                        .loginWithPassword(arg0, arg1, ::reportSuccess, ::reportFailure)
                AUTH_OP_OTP ->
                    com.example.my_kmp_project.feature.auth.AuthBridge
                        .loginWithOtp(arg0, arg1, ::reportSuccess, ::reportFailure)
                AUTH_OP_SEND_OTP ->
                    com.example.my_kmp_project.feature.auth.AuthBridge
                        .sendPhoneOtp(arg0, ::reportSuccess, ::reportFailure)
                AUTH_OP_REGISTER ->
                    com.example.my_kmp_project.feature.auth.AuthBridge
                        .register(arg0, arg1, arg2, ::reportSuccess, ::reportFailure)
                AUTH_OP_HUAWEI ->
                    com.example.my_kmp_project.feature.auth.AuthBridge
                        .loginWithHuaweiCode(arg0, arg1, ::reportSuccess, ::reportFailure)
                AUTH_OP_LOGIN_WITH_METHOD ->
                    com.example.my_kmp_project.feature.auth.AuthBridge
                        .loginWithMethod(arg0, arg1, arg2, ::reportSuccess, ::reportFailure)
                else -> reportFailure("未知登录方式")
            }
        } catch (t: Throwable) {
            reportFailure(t.message ?: "登录失败，请稍后重试")
        }
    }
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

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName("KnAuthCopyAvailableLoginMethods")
fun KnAuthCopyAvailableLoginMethods(
    buf: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?,
    cap: Int,
): Int {
    // 该 getter 可能在 Kotlin shell 首帧前被 ArkTS 调用；先幂等安装依赖再读注册表。
    ensureOhosAuthReady()
    return copyUtf8(
        com.example.my_kmp_project.feature.auth.AuthBridge.availableLoginMethodsJson(),
        buf,
        cap,
    )
}

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
