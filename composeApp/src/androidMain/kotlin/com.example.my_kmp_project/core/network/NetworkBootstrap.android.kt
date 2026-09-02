package com.example.my_kmp_project.core.network

internal actual fun platformClientType(): String = "android"

internal actual fun platformIsReleaseBuild(): Boolean = false

internal actual fun platformClientVersion(): String? = "1.0"

internal actual fun platformClientBuildNumber(): String? = "1"

internal actual fun platformNetworkBootstrap() {
    applyNetworkBootstrap()
}
