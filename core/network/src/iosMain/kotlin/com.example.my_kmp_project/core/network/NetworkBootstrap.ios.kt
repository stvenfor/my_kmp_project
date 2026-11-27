package com.example.my_kmp_project.core.network

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform
import platform.Foundation.NSBundle

public actual fun platformClientType(): String = "ios"

@OptIn(ExperimentalNativeApi::class)
public actual fun platformIsReleaseBuild(): Boolean =
    !Platform.isDebugBinary || BuildEnvironment.IS_RELEASE

public actual fun platformClientVersion(): String? =
    NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
        ?: BuildEnvironment.VERSION_NAME

public actual fun platformClientBuildNumber(): String? =
    NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as? String
        ?: BuildEnvironment.VERSION_CODE.toString()

public actual fun platformNetworkBootstrap() {
    applyNetworkBootstrap()
}
