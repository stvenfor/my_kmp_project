package com.example.my_kmp_project.core.network

/**
 * Applies platform identity and build-mode defaults into [NetworkConfig], then rebuilds client.
 */
internal fun applyNetworkBootstrap(
    clientType: String = platformClientType(),
    isRelease: Boolean = platformIsReleaseBuild(),
    clientVersion: String? = platformClientVersion(),
    clientBuildNumber: String? = platformClientBuildNumber(),
) {
    NetworkConfig.clientType = clientType
    NetworkConfig.clientVersion = clientVersion
    NetworkConfig.clientBuildNumber = clientBuildNumber
    if (isRelease) {
        NetworkConfig.buildMode = BuildMode.Release
        NetworkConfig.netEnvironment = NetEnvironment.Product
        NetworkConfig.baseUrlOverride = ""
    } else {
        NetworkConfig.buildMode = BuildMode.Debug
    }
    NetworkFacade.rebuildClient()
}

internal expect fun platformNetworkBootstrap()

internal expect fun platformClientType(): String

internal expect fun platformIsReleaseBuild(): Boolean

internal expect fun platformClientVersion(): String?

internal expect fun platformClientBuildNumber(): String?
