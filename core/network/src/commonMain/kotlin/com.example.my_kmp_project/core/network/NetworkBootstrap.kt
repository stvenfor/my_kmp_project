package com.example.my_kmp_project.core.network

/**
 * Applies platform identity and build-mode defaults into [NetworkConfig], then rebuilds client.
 *
 * 环境来自构建期生成的 [BuildEnvironment.ENV]（`gradle/app.env.properties` /
 * `-Papp.env`），不再由构建类型硬编码；release 只做 https 兜底（见
 * [NetworkConfig.effectiveBaseUrl]）。
 */
public fun applyNetworkBootstrap(
    clientType: String = platformClientType(),
    isRelease: Boolean = platformIsReleaseBuild() || BuildEnvironment.IS_RELEASE,
    clientVersion: String? = platformClientVersion(),
    clientBuildNumber: String? = platformClientBuildNumber(),
) {
    NetworkConfig.clientType = clientType
    NetworkConfig.clientVersion = clientVersion
    NetworkConfig.clientBuildNumber = clientBuildNumber
    NetworkConfig.netEnvironment = BuildEnvironment.environment()
    if (isRelease) {
        NetworkConfig.buildMode = BuildMode.Release
        NetworkConfig.baseUrlOverride = ""
    } else {
        NetworkConfig.buildMode = BuildMode.Debug
    }
    NetworkFacade.rebuildClient()
}

public expect fun platformNetworkBootstrap()

public expect fun platformClientType(): String

public expect fun platformIsReleaseBuild(): Boolean

public expect fun platformClientVersion(): String?

public expect fun platformClientBuildNumber(): String?
