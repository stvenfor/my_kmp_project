// feature:friend —— 好友通讯录（ImFriendApi HTTP 契约 + 「好友/IM 未接入」占位页）。
// 好友关系后端与融云 SDK 均未接入，不保留本地 mock 好友目录。
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
            api(projects.core.network)
            // :component 以 api 方式带出 core.base / core.router / core.network。
            api(projects.component)
        }
        getByName("commonTest").dependencies {
            implementation(projects.core.network)
        }
    }
}

android { namespace = "com.example.my_kmp_project.feature.friend" }
