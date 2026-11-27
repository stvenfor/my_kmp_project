package com.example.my_kmp_project.component.auth

/**
 * iOS 取码能力占位（第 0 批）。
 *
 * 当前 iOS 登录页是原生 SwiftUI，由壳层自行调用微信 / QQ SDK 后把 code 交给
 * `LoginCredentials.ExternalCode`；内置 provider 暂不实现，返回 null。
 */
internal actual fun platformAuthorizationCodeProviderOrNull(
    methodId: String,
): AuthorizationCodeProvider? = null
