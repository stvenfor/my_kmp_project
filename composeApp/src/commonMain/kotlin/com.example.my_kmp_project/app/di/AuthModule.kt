package com.example.my_kmp_project.app.di

import com.example.my_kmp_project.component.auth.DefaultLoginStrategyRegistry
import com.example.my_kmp_project.component.auth.LoginController
import com.example.my_kmp_project.component.auth.LoginStrategyRegistry
import com.example.my_kmp_project.feature.auth.HuaweiLoginStrategy
import com.example.my_kmp_project.feature.auth.LoginOrchestrator
import com.example.my_kmp_project.feature.auth.PasswordLoginStrategy
import com.example.my_kmp_project.feature.auth.PhoneOtpLoginStrategy
import com.example.my_kmp_project.feature.auth.UserAuthHttpApi
import com.example.my_kmp_project.feature.auth.WechatLoginStrategy

/**
 * 登录策略装配（策略模式第 1 批）。
 *
 * 显式静态注册，不用反射 / ServiceLoader；可用性由每个策略的 `availability()` 决定。
 * 当前注册密码、短信、微信（契约层，未配置授权通道时不可用）、华为；QQ 后续加入。
 *
 * 依赖从 [CoreModule] 取，和 `AuthSessionBootstrap` 安装的是同一份 client / session。
 */
internal object AuthModule {

    private val api: UserAuthHttpApi by lazy {
        UserAuthHttpApi(CoreModule.network().api())
    }

    private val password: PasswordLoginStrategy by lazy {
        PasswordLoginStrategy(api)
    }

    private val phoneOtp: PhoneOtpLoginStrategy by lazy {
        PhoneOtpLoginStrategy(api)
    }

    private val huawei: HuaweiLoginStrategy by lazy {
        HuaweiLoginStrategy(api)
    }

    private val wechat: WechatLoginStrategy by lazy {
        WechatLoginStrategy(api)
    }

    private val registry: LoginStrategyRegistry by lazy {
        DefaultLoginStrategyRegistry(listOf(password, phoneOtp, wechat, huawei))
    }

    private val controller: LoginController by lazy {
        LoginOrchestrator(
            registry = registry,
            repository = CoreModule.accountRepository(),
            state = CoreModule.sessionState(),
        )
    }

    fun registry(): LoginStrategyRegistry = registry

    fun loginController(): LoginController = controller
}
