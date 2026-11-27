package com.example.my_kmp_project.component.auth

/**
 * 鸿蒙取码能力占位（第 0 批）。
 *
 * - 华为账号：Account Kit 只能从 ArkTS 发起，现有入口仍在
 *   `harmonyApp/.../HuaweiQuickLoginPane.ets`，取到 code 后直接调 Kotlin；
 * - 微信 / QQ：OHOS 无官方 SDK，按 `platform-gap-registry` 登记 missing，不做伪实现。
 *
 * 两条路径都不需要 Kotlin 侧 provider，因此这里返回 null。
 */
internal actual fun platformAuthorizationCodeProviderOrNull(
    methodId: String,
): AuthorizationCodeProvider? = null
