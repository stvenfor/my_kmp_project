// core:audio —— OHOS 音频原生绑定（AVPlayer + AVSession）。
//
// 为什么不放 :core:platform：cinterop 与 Compose 依赖同模块时，KGP 的 KLIB
// resolver 解析不到 skiko（cinterop 任务拿不到 commonized klib），绑定会直接失败。
// 本模块刻意不依赖 Compose，只暴露 Kotlin 桥；target 集由根 build.gradle.kts 统一接线。
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.io.File

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
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
        listOf("ohosArm64", "ohosX64").forEach { targetName ->
            val target = targets.getByName(targetName) as KotlinNativeTarget
            target.compilations.getByName("main") {
                val avplayer by cinterops.creating {
                    defFile(file("src/ohosMain/cinterop/avplayer.def"))
                    ohosNativeSysroot?.let { sysroot ->
                        includeDirs(
                            sysroot.resolve("usr/include"),
                            sysroot.resolve("usr/include/multimedia/player_framework"),
                        )
                    }
                }
                val avsession by cinterops.creating {
                    defFile(file("src/ohosMain/cinterop/avsession.def"))
                    ohosNativeSysroot?.let { sysroot ->
                        includeDirs(
                            sysroot.resolve("usr/include"),
                            sysroot.resolve("usr/include/multimedia/av_session"),
                        )
                    }
                }
            }
        }
    }
}

android { namespace = "com.example.my_kmp_project.core.audio" }
