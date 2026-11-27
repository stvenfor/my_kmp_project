package com.example.my_kmp_project.component.auth

/**
 * Android 取码能力占位（第 0 批）。
 *
 * 微信 / QQ OpenSDK 适配器在后续批次接入；未接入前返回 null，
 * 让策略 `availability()` 如实返回不可用，避免出现必然失败的登录按钮。
 */
internal actual fun platformAuthorizationCodeProviderOrNull(
    methodId: String,
): AuthorizationCodeProvider? = null
