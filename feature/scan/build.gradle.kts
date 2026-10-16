// feature:scan —— 扫码页（共享 Compose；相机/条码能力由 core.platform 提供，经 core.ui 传递带出）。
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
            // core:ui -> core.platform（PlatformBarcodeScanner / CameraPermissionStatus）
            //        -> core.design（DemoColors / MineTopBar）
            api(projects.core.ui)
        }
    }
}

android {
    namespace = "com.example.my_kmp_project.feature.scan"
}
