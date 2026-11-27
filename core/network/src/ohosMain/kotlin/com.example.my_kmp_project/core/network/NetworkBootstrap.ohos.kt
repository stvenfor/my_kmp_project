package com.example.my_kmp_project.core.network

public actual fun platformClientType(): String = "ohos"

/**
 * HarmonyOS 没有运行时 debug 标记：release 打包必须先执行
 * `./gradlew :composeApp:publishReleaseBinariesToHarmonyApp -Papp.release=true -Papp.env=prod`
 * 重新生成并链接，[BuildEnvironment.IS_RELEASE] 才会是 true。
 */
public actual fun platformIsReleaseBuild(): Boolean = BuildEnvironment.IS_RELEASE

public actual fun platformClientVersion(): String? = BuildEnvironment.VERSION_NAME

public actual fun platformClientBuildNumber(): String? =
    BuildEnvironment.VERSION_CODE.toString()

public actual fun platformNetworkBootstrap() {
    applyNetworkBootstrap()
}
