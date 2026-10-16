import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// core:base —— 按 kmp-cmp-example 的 lib-* 模块范式，对照本工程 core:* 现有写法建立。
// 角色：最底层基础工具库（被其它模块以 api 传递导出时的依赖靶子），当前为占位骨架。
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

val androidOnly = rootProject.findProperty("androidOnly")?.toString()?.toBoolean() == true

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    if (!androidOnly) {
        listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { /* library — no framework binary */ }
        listOf(ohosArm64(), ohosX64()).forEach { /* library — linked via :composeApp */ }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.atomicFu)
        }

        if (!androidOnly) {
            val iosMain = sourceSets.create("iosMain").apply {
                dependsOn(commonMain.get())
            }
            listOf("iosX64Main", "iosArm64Main", "iosSimulatorArm64Main").forEach {
                sourceSets.getByName(it).dependsOn(iosMain)
            }

            val ohosMain = sourceSets.create("ohosMain").apply {
                dependsOn(commonMain.get())
            }
            sourceSets.getByName("ohosArm64Main").dependsOn(ohosMain)
            sourceSets.getByName("ohosX64Main").dependsOn(ohosMain)
        }
    }
}

android {
    namespace = "com.example.my_kmp_project.core.base"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
