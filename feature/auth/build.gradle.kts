// feature:auth —— 登录 / 注册 / OTP、会话引导与 token 刷新、用户资料契约。
//
// 只依赖 core.* 与 component，不引用任何别的 feature，也不引用 :composeApp 的 app/ 壳
// （AppShell 反向依赖本模块的 AuthBridge / LoginScreen）。
// target 集 / android 基础配置 / coroutines·atomicfu·kotlin-test 公共依赖见根 build.gradle.kts。
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    // @Serializable 契约（AuthApiContracts / UserProfileContracts）需要序列化插件。
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        getByName("commonMain").dependencies {
            api(projects.core.ui)      // 传递带出 core.platform -> core.design / core.account
            // ↓ 按我的清单增删，不要全量塞
            api(projects.core.network)
            api(projects.component)
            // core.network 对 serialization 是 implementation，不向下游传递，本模块直接
            // import kotlinx.serialization.*，必须自备。
            implementation(libs.kotlinx.serialization.json)
        }
        getByName("commonTest").dependencies {
            implementation(projects.core.network)
            // 契约测试引用 core.router 的 MainTab（经 component 传递可见，这里显式声明）。
            implementation(projects.core.router)
        }
    }
}

android { namespace = "com.example.my_kmp_project.feature.auth" }
