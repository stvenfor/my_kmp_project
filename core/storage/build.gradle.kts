import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// core:storage —— 按 kmp-cmp-example 的 lib-storage 范式，对照本工程 core:* 现有写法建立。
// 角色：独立存储/持久化模块骨架，当前为占位（无真实实现），后续按业务填充。
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
    namespace = "com.example.my_kmp_project.core.storage"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
