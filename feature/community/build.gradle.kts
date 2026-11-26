// feature:community —— 社区信息流 / 发帖 / 搜索（统一 HTTP 仓储，已无进程内 mock）。
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
            implementation(projects.core.base) // AppUuid：评论 client_msg_id（唯一 UUID 设施）
        }
        getByName("commonTest").dependencies {
            implementation(projects.core.network)
        }
    }
}

android { namespace = "com.example.my_kmp_project.feature.community" }
