package com.example.my_kmp_project.core.network

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform
import platform.Foundation.NSBundle

internal actual fun platformClientType(): String = "ios"

@OptIn(ExperimentalNativeApi::class)
internal actual fun platformIsReleaseBuild(): Boolean = !Platform.isDebugBinary

internal actual fun platformClientVersion(): String? =
    NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String

internal actual fun platformClientBuildNumber(): String? =
    NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as? String

internal actual fun platformNetworkBootstrap() {
    applyNetworkBootstrap()
}
