package com.example.my_kmp_project.component.auth

import kotlin.concurrent.Volatile

/**
 * 第三方授权码获取结果。
 *
 * 取消与失败分开：用户主动关闭微信 / QQ 授权页不应弹错误文案。
 */
public sealed interface AuthorizationCodeResult {

    public data class Success(
        public val code: String,
    ) : AuthorizationCodeResult

    public data object Cancelled : AuthorizationCodeResult

    public data class Unavailable(
        public val reason: String,
    ) : AuthorizationCodeResult

    public data class Failure(
        public val message: String,
    ) : AuthorizationCodeResult
}

/**
 * 一种第三方登录方式的原生取码能力（微信 / QQ / 华为…）。
 *
 * 放在 `component:auth` 而不是 feature：平台 SDK 适配器需要 Android Activity /
 * iOS URL scheme 等宿主能力，feature 层不应直接依赖它们。
 */
public interface AuthorizationCodeProvider {

    /** 与 [LoginStrategy.id] 对应（如 `wechat`）。 */
    public val methodId: String

    /** 拉起原生授权页并等待 code；实现必须支持协程取消。 */
    public suspend fun requestCode(): AuthorizationCodeResult
}

/**
 * 平台取码能力注册表。
 *
 * 两种来源，优先级从高到低：
 * 1. 应用装配层显式 [register]（Android 拿到 Activity、iOS 拿到 URL 回调后注册）；
 * 2. 平台 actual 内置适配器（第 0 批全部返回 null）。
 *
 * 注册表本身不是登录方式清单——是否展示由策略的 [LoginStrategy.availability] 决定。
 */
public object PlatformLoginCapabilities {

    @Volatile
    private var registered: Map<String, AuthorizationCodeProvider> = emptyMap()

    public fun register(provider: AuthorizationCodeProvider) {
        registered = registered + (provider.methodId to provider)
    }

    public fun unregister(methodId: String) {
        registered = registered - methodId
    }

    public fun providerOrNull(methodId: String): AuthorizationCodeProvider? =
        registered[methodId] ?: platformAuthorizationCodeProviderOrNull(methodId)

    /** 测试 / 热重载清理显式注册项（内置适配器不受影响）。 */
    internal fun clear() {
        registered = emptyMap()
    }
}

/**
 * 平台内置取码适配器。
 *
 * 第 0 批只落契约：三端 actual 一律返回 null，策略因此会展示为「不可用」，
 * 而不是渲染一个注定失败的按钮。微信 / QQ SDK 在后续批次接入后覆盖对应 actual。
 */
internal expect fun platformAuthorizationCodeProviderOrNull(
    methodId: String,
): AuthorizationCodeProvider?
