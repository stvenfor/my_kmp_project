import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { /* library — no framework binary */ }

    listOf(
        ohosArm64(),
        ohosX64(),
    ).forEach { /* library — linked via :composeApp / future :ohosAggregate */ }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.network)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.atomicFu)
            implementation(libs.kotlinx.serialization.json)
        }

        val accountSettingsMain = sourceSets.create("accountSettingsMain").apply {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.multiplatform.settings)
            }
        }

        val iosMain = sourceSets.create("iosMain").apply {
            dependsOn(commonMain.get())
            dependsOn(accountSettingsMain)
        }
        sourceSets.getByName("androidMain").dependsOn(accountSettingsMain)
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

android {
    namespace = "com.example.my_kmp_project.core.account"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
