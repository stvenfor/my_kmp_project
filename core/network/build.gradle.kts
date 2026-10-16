import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.io.File

// core:network —— HTTP / SSE / realtime 传输层（唯一持有 Ktor 的模块）。
// target 集 / android 基础配置 / 公共依赖 / iosMain·ohosMain 层级见根 build.gradle.kts；
// 这里只留本模块特有的东西：Ktor 能力 source set、ohosX64 的 net_http cinterop。
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
}

val androidOnly = rootProject.findProperty("androidOnly")?.toString()?.toBoolean() == true

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
    if (!androidOnly) {
        // net_http cinterop only on ohosX64 (no CPF Ktor ohosX64 klib).
        // ohosArm64 uses Ktor CIO — see networkKtorMain + HttpClientFactory.ohosArm64.
        val ohosX64 = targets.getByName("ohosX64") as KotlinNativeTarget
        ohosX64.compilations.getByName("main") {
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
        val commonMain = getByName("commonMain")
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }

        val networkKtorMain = create("networkKtorMain").apply {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.logging)
                // realtime WS（Flutter components/realtime）— CPF 3.3.3-0.3.0 有 ohosArm64 klib。
                implementation(libs.ktor.client.websockets)
            }
        }

        getByName("androidMain").apply {
            dependsOn(networkKtorMain)
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }

        if (!androidOnly) {
            getByName("iosMain").apply {
                dependsOn(networkKtorMain)
                dependencies {
                    implementation(libs.ktor.client.darwin)
                }
            }
            // Arm64: shared Ktor + CIO. X64: cinterop only (design D4 — no ohosX64 Ktor klib).
            getByName("ohosArm64Main").apply {
                dependsOn(networkKtorMain)
                dependencies {
                    implementation(libs.ktor.client.cio)
                }
            }
        }

        getByName("commonTest").dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        getByName("androidUnitTest").dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

android {
    namespace = "com.example.my_kmp_project.core.network"
}
