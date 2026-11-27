// component —— 平台能力组件层（auth / chat / pay / push / realtime / routing / webview）。
//
// 依赖只允许向下：core.network（含 realtime）、core.base（core.uuid 住在这里）、
// core.account、core.storage（KV 绑定）、core.router；绝不允许 import feature. 或 app.
// （scripts/check-layer-deps.sh 拦截）。
//
// 注意：本模块路径是 `:component`，不在根 build.gradle.kts 的 `:core:*` / `:feature:*`
// 共享接线命中范围内，所以 target 集、android 基础配置、公共依赖都要在这里完整声明
// （-PandroidOnly=true 跳过 native 的分支写法对齐 composeApp/build.gradle.kts）。
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// GitHub Actions Android CI: ./gradlew ... -PandroidOnly=true (skip iOS/OHOS targets)
val androidOnly = rootProject.findProperty("androidOnly")?.toString()?.toBoolean() == true

kotlin {
    androidTarget {
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    if (!androidOnly) {
        // 库模块一律不产 framework / sharedLib：只有 :composeApp 是二进制产出方。
        iosX64()
        iosArm64()
        iosSimulatorArm64()
        ohosArm64()
        ohosX64()
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.network)
            api(projects.core.base)
            api(projects.core.account)
            api(projects.core.storage)
            api(projects.core.router)
            // AppGraph 暴露 MusicSessionPort（AppScope 音乐会话）——端口类型住在 core.ui。
            api(projects.core.ui)
            api(compose.runtime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        if (!androidOnly) {
            val iosMain = maybeCreate("iosMain").apply {
                dependsOn(commonMain.get())
            }
            listOf("iosX64Main", "iosArm64Main", "iosSimulatorArm64Main").forEach {
                getByName(it).dependsOn(iosMain)
            }
            val ohosMain = maybeCreate("ohosMain").apply {
                dependsOn(commonMain.get())
            }
            getByName("ohosArm64Main").dependsOn(ohosMain)
            getByName("ohosX64Main").dependsOn(ohosMain)
        }
    }
}

android {
    namespace = "com.example.my_kmp_project.component"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
