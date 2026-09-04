package com.example.my_kmp_project.core.network

/**
 * Applies platform identity and build-mode defaults into [NetworkConfig], then rebuilds client.
 */
public fun applyNetworkBootstrap(
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

public expect fun platformNetworkBootstrap()

public expect fun platformClientType(): String

public expect fun platformIsReleaseBuild(): Boolean

public expect fun platformClientVersion(): String?

public expect fun platformClientBuildNumber(): String?
