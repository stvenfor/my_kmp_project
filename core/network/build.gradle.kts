import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
}

val ohosNativeSysroot: File? = run {
    val localLines = rootProject.file("local.properties").takeIf { it.exists() }?.readLines().orEmpty()
    val fromLocal = localLines.firstOrNull { it.startsWith("local.ohos.native=") }
        ?.substringAfter("=")?.trim()?.let { File(it) }
    fromLocal
        ?: System.getenv("OHOS_NATIVE_SYSROOT")?.let { File(it) }
        ?: File("/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/sysroot")
            .takeIf { it.exists() }
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
    ).forEach { ohosTarget ->
        ohosTarget.compilations.getByName("main") {
            val netHttp by cinterops.creating {
                defFile(file("src/ohosMain/cinterop/net_http.def"))
                includeDirs(file("src/ohosMain/cinterop/include"))
                ohosNativeSysroot?.let { sysroot ->
                    includeDirs(sysroot.resolve("usr/include"))
                }
                compilerOpts("-I${file("src/ohosMain/cinterop/include").absolutePath}")
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.atomicFu)
            implementation(libs.kotlinx.serialization.json)
        }

        val networkKtorMain = sourceSets.create("networkKtorMain").apply {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.logging)
            }
        }

        val iosMain = sourceSets.create("iosMain").apply {
            dependsOn(commonMain.get())
            dependsOn(networkKtorMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
        sourceSets.getByName("androidMain").dependsOn(networkKtorMain)
        sourceSets.getByName("androidMain").dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        listOf("iosX64Main", "iosArm64Main", "iosSimulatorArm64Main").forEach {
            sourceSets.getByName(it).dependsOn(iosMain)
        }

        val ohosMain = sourceSets.create("ohosMain").apply {
            dependsOn(commonMain.get())
        }
        sourceSets.getByName("ohosArm64Main").dependsOn(ohosMain)
        sourceSets.getByName("ohosX64Main").dependsOn(ohosMain)

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        sourceSets.getByName("androidUnitTest").dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

android {
    namespace = "com.example.my_kmp_project.core.network"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
