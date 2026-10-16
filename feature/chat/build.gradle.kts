// feature:chat —— IM 会话 / 群组 / 资料 / 消息备份 API + 聊天页。
// 引擎抽象（ImEngine / MockImEngine / ImEngineStore）在 :component，本模块只做 API 与 UI。
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
            // :component 以 api 方式带出 core.base（AppUuid 供 ImSessionApi 生成会话 id）
            // 与 core.router / core.network，所以这里不再单列。
            api(projects.component)
        }
        getByName("commonTest").dependencies {
            implementation(projects.core.network)
        }
    }
}

android { namespace = "com.example.my_kmp_project.feature.chat" }
