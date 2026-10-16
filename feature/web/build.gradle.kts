// feature:web —— 应用内 WebView 页（共享 Compose；PlatformWebView / WebBridgeHost 来自 core.platform）。
// target 集 / android 基础配置 / coroutines·atomicfu·kotlin-test 公共依赖见根 build.gradle.kts。
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    sourceSets {
        getByName("commonMain").dependencies {
            // core:ui -> core.platform（PlatformWebView / WebBridgeHost / PlatformBackHandler）
            //        -> core.design（DemoColors / MineTopBar）
            api(projects.core.ui)
        }
    }
}

android {
    namespace = "com.example.my_kmp_project.feature.web"
}
