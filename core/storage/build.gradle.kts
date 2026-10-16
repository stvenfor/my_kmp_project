// core:storage —— 全工程唯一的 KV 持久化模块（KeyValueStore）。
//
// settingsMain 是按产物缺口切的中间 source set：multiplatform-settings 只给 android/ios 用。
// 官方 1.3.0 没有 ohos klib，CPF 的 -0.3.0 线又没发 android AAR（实测
// multiplatform-settings-android-1.3.0-0.3.0.aar → 404），没有任何一条线能同时覆盖六个
// target，所以 OHOS 两个架构都由 ohosMain 直接实现 KeyValueStore 接口（OH_Preferences NDK），
// 而不是 arm64 一套、x64 另一套。Settings 类型只活在 settingsMain 里，
// core:account / core:platform 只看得到 KeyValueStore。
//
// target 集 / android 基础配置 / 公共依赖 / iosMain·ohosMain 层级见根 build.gradle.kts。
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    sourceSets {
        val settingsMain = create("settingsMain").apply {
            dependsOn(getByName("commonMain"))
            dependencies {
                implementation(libs.multiplatform.settings)
            }
        }

        getByName("androidMain").dependsOn(settingsMain)

        // androidOnly CI 没有 ios target，所以用惰性匹配而不是 getByName。
        matching { it.name == "iosMain" }.all { dependsOn(settingsMain) }
    }
}

android {
    namespace = "com.example.my_kmp_project.core.storage"
}
