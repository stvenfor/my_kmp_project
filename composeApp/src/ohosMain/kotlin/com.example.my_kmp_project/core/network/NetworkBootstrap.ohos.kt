package com.example.my_kmp_project.core.network

internal actual fun platformClientType(): String = "ohos"

internal actual fun platformIsReleaseBuild(): Boolean = false

internal actual fun platformClientVersion(): String? = null

internal actual fun platformClientBuildNumber(): String? = null

internal actual fun platformNetworkBootstrap() {
    applyNetworkBootstrap()
}
