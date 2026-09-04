package com.example.my_kmp_project.core.network

public actual fun platformClientType(): String = "ohos"

public actual fun platformIsReleaseBuild(): Boolean = false

public actual fun platformClientVersion(): String? = null

public actual fun platformClientBuildNumber(): String? = null

public actual fun platformNetworkBootstrap() {
    applyNetworkBootstrap()
}
