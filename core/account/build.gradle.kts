// core:account —— 会话/隐私同意本地存储。
//
// KV 实现全部下沉到 :core:storage，所以本模块只剩 commonMain：没有 expect/actual，
// 也没有 per-平台 actual 各写一份持久化（这正是之前 OHOS 落到 /tmp 的原因）。
// accountSettingsMain 中间 source set 随之删除，multiplatform-settings 只在 core:storage 出现。
//
// target 集 / android 基础配置 / 公共依赖 / iosMain·ohosMain 层级见根 build.gradle.kts。
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        getByName("commonMain").dependencies {
            implementation(projects.core.network)
            implementation(projects.core.storage)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}

android {
    namespace = "com.example.my_kmp_project.core.account"
}
