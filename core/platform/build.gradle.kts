// core:platform —— 平台能力抽象（返回键、外部打开、WebView、剪贴板、相机扫码、KV 列表存储）。
//
// 这里放的是「必须碰平台 API」的最小外壳：commonMain 只声明 expect，
// android/ios/ohos 各自 actual。扫码用的 CameraX + MLKit 只在本模块的 androidMain 出现，
// 不污染 :composeApp，也不污染任何 feature。
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
            api(projects.core.design)
            api(projects.core.account)
            implementation(projects.core.base)
            implementation(projects.core.storage)
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
            api(compose.components.resources)
        }
        getByName("androidMain").dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.mlkit.barcode.scanning)
        }
    }
}

android {
    namespace = "com.example.my_kmp_project.core.platform"
}
