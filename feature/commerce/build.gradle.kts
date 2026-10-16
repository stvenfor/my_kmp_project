// feature:commerce —— 商城 + 会员售卖页（共享 Compose + HTTP）。
// target 集 / android 基础配置 / coroutines·atomicfu·kotlin-test 公共依赖见根 build.gradle.kts。
// 依赖只允许向下：core.* 与 component；绝不 import 其它 feature（门禁 R7）。
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    // Mall*Models 用 @Serializable，需要序列化编译器插件。
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        getByName("commonMain").dependencies {
            // core:ui 传递带出 core.platform -> core.design / core.account + compose.*
            api(projects.core.ui)
            api(projects.core.network)
            // component.pay（PayGateway / FlaggedPayGateway）
            api(projects.component)
            // JsonElement 出现在公开模型签名里 → api；:core:network 只 implementation。
            api(libs.kotlinx.serialization.json)
        }
        getByName("commonTest").dependencies {
            implementation(projects.core.network)
        }
    }
}

android {
    namespace = "com.example.my_kmp_project.feature.commerce"
}
