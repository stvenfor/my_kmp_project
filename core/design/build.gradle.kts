// core:design —— 设计令牌 SoT + 沉浸式顶栏 + 全部 composeResources（171 个 drawable/font）。
//
// 资源放这里而不是 :composeApp：任何 feature 模块都要用 Res.drawable.*，但它们不能反向
// 依赖 :composeApp。所以资源的唯一归属就是设计系统模块，`publicResClass = true`
// 让生成的 Res 类能被其它模块读到。
// target 集 / android 基础配置 / 公共依赖见根 build.gradle.kts 的共享接线。
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    sourceSets {
        getByName("commonMain").dependencies {
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
            api(compose.components.resources)
        }
    }
}

compose {
    resources {
        publicResClass = true
    }
}

android {
    namespace = "com.example.my_kmp_project.core.design"
}
