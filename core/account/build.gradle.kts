// core:account —— 会话/隐私同意本地存储（KV，基于 multiplatform-settings）。
// target 集 / android 基础配置 / 公共依赖 / iosMain·ohosMain 层级见根 build.gradle.kts；
// 这里只留本模块特有的 accountSettingsMain 能力 source set（OHOS 无 settings 产物，
// 所以刻意不挂到 ohosMain）。
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
}

val androidOnly = rootProject.findProperty("androidOnly")?.toString()?.toBoolean() == true

kotlin {
    sourceSets {
        getByName("commonMain").dependencies {
            implementation(projects.core.network)
            implementation(libs.kotlinx.serialization.json)
        }

        val accountSettingsMain = create("accountSettingsMain").apply {
            dependsOn(getByName("commonMain"))
            dependencies {
                implementation(libs.multiplatform.settings)
            }
        }

        getByName("androidMain").dependsOn(accountSettingsMain)

        if (!androidOnly) {
            getByName("iosMain").dependsOn(accountSettingsMain)
        }
    }
}

android {
    namespace = "com.example.my_kmp_project.core.account"
}
