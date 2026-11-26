// feature:media —— 短视频 / 跟读 / 音乐三个岛 + 平台播放器 expect-actual。
// android·ios·ohos 的 MediaPlayer actual 已经跟着模块搬进各自 source set，:composeApp 不再持有。
// 目标集、android 基础配置、coroutines / atomicfu / serialization / kotlin-test 由根
// build.gradle.kts 的 :feature:* 共享接线提供，本文件只声明模块特有的依赖边。
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    sourceSets {
        getByName("commonMain").dependencies {
            api(projects.core.ui)      // 传递带出 core.platform -> core.design / core.account
            // ↓ 按我的清单增删，不要全量塞
            api(projects.core.router)
            api(projects.core.network)
        }
        getByName("androidMain").dependencies {
            implementation(libs.composemediaplayer.video)
        }
        // iosMain 由根工程共享接线创建；-PandroidOnly=true（CI）时它不存在，用 matching 兼容两者。
        matching { it.name == "iosMain" }.all {
            dependencies { implementation(libs.composemediaplayer.video) }
        }
        getByName("commonTest").dependencies {
            implementation(projects.core.network)
        }
    }
}

android { namespace = "com.example.my_kmp_project.feature.media" }
