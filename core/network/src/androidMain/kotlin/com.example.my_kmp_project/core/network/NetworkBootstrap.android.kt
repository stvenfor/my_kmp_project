package com.example.my_kmp_project.core.network

public actual fun platformClientType(): String = "android"

/**
 * Android 用 library variant 的 `BuildConfig.DEBUG` 判定；CI/打包脚本可用
 * `-Papp.release=true` 强制标记为 release（生成到 [BuildEnvironment.IS_RELEASE]）。
 */
public actual fun platformIsReleaseBuild(): Boolean =
    !BuildConfig.DEBUG || BuildEnvironment.IS_RELEASE

public actual fun platformClientVersion(): String? = BuildEnvironment.VERSION_NAME

public actual fun platformClientBuildNumber(): String? =
    BuildEnvironment.VERSION_CODE.toString()

public actual fun platformNetworkBootstrap() {
    applyNetworkBootstrap()
}
