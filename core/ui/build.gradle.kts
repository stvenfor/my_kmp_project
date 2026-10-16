// core:ui —— 跨端 UI 原子件（网络图 PlatformNetworkImage、Tab chrome、权限门 PermissionGate）。
//
// Coil 只在 imageCoilMain 挂到 android/ios/ohosArm64：ohosX64 没有 CPF Coil klib，
// 由 PlatformNetworkImage.ohosX64.kt 的占位 actual 顶上。网络图实现放在这里而不是
// :composeApp，feature 模块才能「按需引入」拿到 AsyncImage 而不反向依赖 App。
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
            api(projects.core.platform)
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
            api(compose.components.resources)
        }
        val imageCoilMain = maybeCreate("imageCoilMain").apply {
            dependsOn(getByName("commonMain"))
            dependencies { implementation(libs.coil.compose) }
        }
        getByName("androidMain").apply {
            dependsOn(imageCoilMain)
            dependencies { implementation(libs.coil.network.okhttp) }
        }
        // androidOnly CI 没有 ios/ohos target，所以这两处用惰性匹配而不是 getByName。
        matching { it.name == "iosMain" }.all {
            dependsOn(imageCoilMain)
            dependencies { implementation(libs.coil.network.ktor3) }
        }
        matching { it.name == "ohosArm64Main" }.all {
            dependsOn(imageCoilMain)
            dependencies { implementation(libs.coil.network.ktor3) }
        }
    }
}

android {
    namespace = "com.example.my_kmp_project.core.ui"
}
