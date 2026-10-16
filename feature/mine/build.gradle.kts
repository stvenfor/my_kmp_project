plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    // @Serializable 模型（address/calculator/dealinvoice/usedcar 子包）需要序列化插件。
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        getByName("commonMain").dependencies {
            api(projects.core.ui)      // 传递带出 core.platform -> core.design / core.account
            // ↓ 按我的清单增删，不要全量塞
            api(projects.core.router)
            api(projects.core.network)
            api(projects.component)
            // core.network 对 serialization 是 implementation，不向下游传递，本模块直接
            // import kotlinx.serialization.*，必须自备。
            implementation(libs.kotlinx.serialization.json)
        }
        getByName("commonTest").dependencies {
            implementation(projects.core.network)
        }
    }
}

android { namespace = "com.example.my_kmp_project.feature.mine" }
