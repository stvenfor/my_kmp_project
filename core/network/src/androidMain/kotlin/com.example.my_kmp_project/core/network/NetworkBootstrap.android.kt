package com.example.my_kmp_project.core.network

public actual fun platformClientType(): String = "android"

public actual fun platformIsReleaseBuild(): Boolean = false

public actual fun platformClientVersion(): String? = "1.0"

public actual fun platformClientBuildNumber(): String? = "1"

public actual fun platformNetworkBootstrap() {
    applyNetworkBootstrap()
}
