
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import android.databinding.tool.ext.capitalizeUS
import java.io.File

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
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
            // Koog requires JDK 17+
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),    
        iosArm64(),   
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true    
        }
    }
    
  // 配置OHOS（华为鸿蒙）多架构目标
    listOf(
        ohosArm64(),   // 真机 arm64
        ohosX64()      // 模拟器/开发机 x64
    ).forEach { ohosTarget ->
        ohosTarget.binaries.sharedLib {
            baseName = "kn"
            // Release 链接阶段的 DevirtualizationAnalysis 对这个 sample 的内存占用过高，容易在 OOM 后失败。
            if (buildType == NativeBuildType.RELEASE) {
                optimized = false
            }
            export(libs.compose.multiplatform.export)
            // Limit CAdapter to compose.export + composeApp. Without filtering, OHOS link
            // NPEs in getKlibModuleOrigin. composeApp must be included so @CName entries
            // stay alive; keep composeApp API surface mostly internal to shrink exports.
            // Format: moduleIncludes={outputModule:[modSubstring;...]}
            binaryOption("outputModule", "kn")
            binaryOption("moduleIncludes", "{kn:[export;composeApp]}")
            // libdemo_net_http.a already linked via cinterop netHttp staticLibraries;
            // do not also pass -ldemo_net_http (lld cannot find it on the sysroot -L path).
            linkerOpts("-lz", "-lavplayer", "-lnet_http")
                // 渲染模式
 	             // 背景：当 libkn.so 为旧编译产物时，其 DT_NEEDED 可能缺少以下库（正确构建时
 	             // NativeTasksConfiguration.kt 已通过 -l 选项将它们写入 DT_NEEDED）。
 	             // 在 build.gradle.kts 中统一补全，避免在 CMakeLists.txt 中硬编码。
 	             val rendererBackend = rootProject.findProperty("rendererBackend")?.toString() ?: "fusion-renderer"
                    if (rendererBackend == "fusion-renderer") {
 	                 linkerOpts(
 	                     "-lnative_drawing",    // OH_Drawing_*（字体、绘制）
 	                     "-limage_source",       // OH_ImageSourceNative_*（图像解码）
 	                     "-lpixelmap",           // OH_PixelMap_*
 	                     "-lpixelmap_ndk.z",     // OH_PixelMapNdk_*
 	                     "-lnative_window",      // OH_NativeWindow_*
 	                     "-lace_napi.z",         // N-API
 	                     "-lhilog_ndk.z",        // HiLog 日志
 	                     "-lhitrace_ndk.z",      // HiTrace 性能追踪
 	                     "-luv",                 // libuv 事件循环
 	                     "-lunwind",             // 栈展开
 	                     "-licu",               // ICU 文本处理
 	                 )
 	             }
        }
        ohosTarget.compilations.getByName("main") {
            val resource by cinterops.creating {
                defFile(file("src/ohosMain/cinterop/resource.def"))
                includeDirs(file("src/ohosMain/cinterop/include"))
            }
            val avplayer by cinterops.creating {
                defFile(file("src/ohosMain/cinterop/avplayer.def"))
                ohosNativeSysroot?.let { sysroot ->
                    includeDirs(
                        sysroot.resolve("usr/include"),
                        sysroot.resolve("usr/include/multimedia/player_framework"),
                    )
                }
            }
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
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.collection)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.okhttp)
        }
        commonMain.dependencies {
            implementation(compose.runtime)                     
            implementation(compose.foundation)                   
            implementation(compose.material)
            implementation(compose.material3)                    
            implementation(compose.ui)                          
            implementation(compose.components.resources)         
            implementation(compose.components.uiToolingPreview)   
            implementation(libs.kotlinx.coroutines.core)          
            implementation(libs.atomicFu)
            implementation(libs.kotlinx.serialization.json)
        }
          // iOS平台共享代码
                val iosMain = sourceSets.create("iosMain").apply {
                    dependsOn(commonMain.get())
                }
                // Ktor-backed network (no OHOS variant on Maven — keep off ohosMain)
                val networkKtorMain = sourceSets.create("networkKtorMain").apply {
                    dependsOn(commonMain.get())
                    dependencies {
                        implementation(libs.ktor.client.core)
                        implementation(libs.ktor.client.content.negotiation)
                        implementation(libs.ktor.serialization.kotlinx.json)
                        implementation(libs.ktor.client.logging)
                    }
                }
                // multiplatform-settings (no OHOS variant — keep off ohosMain)
                val accountSettingsMain = sourceSets.create("accountSettingsMain").apply {
                    dependsOn(commonMain.get())
                    dependencies {
                        implementation(libs.multiplatform.settings)
                    }
                }
                iosMain.dependsOn(networkKtorMain)
                iosMain.dependsOn(accountSettingsMain)
                sourceSets.getByName("androidMain").dependsOn(networkKtorMain)
                sourceSets.getByName("androidMain").dependsOn(accountSettingsMain)
                // iOS平台依赖
                iosMain.dependencies {
                    implementation(libs.ktor.client.darwin)
                    implementation(libs.coil.compose)
                    implementation(libs.coil.network.ktor3)
                }
                // iOS平台变体依赖关系
                listOf("iosX64Main", "iosArm64Main", "iosSimulatorArm64Main").forEach {
                    sourceSets.getByName(it).dependsOn(iosMain)
                }

         // OHOS 共享（对应目录 src/ohosMain/，arm64/x64 共用）
        val ohosMain = sourceSets.create("ohosMain").apply {
            dependsOn(commonMain.get())
        }
        ohosMain.dependencies {
            api(libs.compose.multiplatform.export)
        }
        val ohosArm64Main by getting {
            dependsOn(ohosMain)
        }
        val ohosX64Main by getting {
            dependsOn(ohosMain)
        }

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
    namespace = "com.example.my_kmp_project"                     
    compileSdk = libs.versions.android.compileSdk.get().toInt()  

    defaultConfig {
        applicationId = "com.example.my_kmp_project"                  
        minSdk = libs.versions.android.minSdk.get().toInt()     
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1                                       
        versionName = "1.0"
    }
    buildFeatures {
        buildConfig = false
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false     
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    debugImplementation(libs.compose.ui.tooling)   
}



// Harmony App 输出目录（支持命令行 --harmonyAppPath）
val harmonyAppDir: File = run {
    val cliPath = project.findProperty("harmonyAppPath") as String?
    if (cliPath.isNullOrBlank()) {
        // 默认：项目根目录 /harmonyApp
        rootProject.file("harmonyApp")
    } else {
        // 命令行传入的路径
        file(cliPath)
    }
}

// 字符串首字母大写工具函数
fun String.capitalizeUS(): String = this.replaceFirstChar { 
    if (it.isLowerCase()) it.titlecase() else it.toString() 
}


// 为不同类型(debug、release)OHOS构建注册Copy任务并发布到Harmony App目录
// CPF 对齐：真机 arm64 用 publish*BinariesToHarmonyApp；
// 模拟器 x86_64 用 publish*BinariesToHarmonyAppX64（需 linkDebugSharedOhosX64 成功）。
// 参见 https://gitcode.com/CPF-KMP-CMP/kmp-cmp-example
arrayOf("debug", "release").forEach { type ->
    tasks.register<Copy>("publish${type.capitalizeUS()}BinariesToHarmonyApp") {
        group = "harmony"
        dependsOn("link${type.capitalizeUS()}SharedOhosArm64")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        into(harmonyAppDir)
        from("build/bin/ohosArm64/${type}Shared/libkn_api.h") {
            into("entry/src/main/cpp/include/arm64-v8a/")
        }
        from(project.file("build/bin/ohosArm64/${type}Shared/libkn.so")) {
            into("entry/libs/arm64-v8a/")
        }
        val composeResourcePackage =
            "${rootProject.name.lowercase()}.${project.name.lowercase()}.generated.resources"
        from("src/commonMain/composeResources") {
            into("entry/src/main/resources/rawfile/composeResources/$composeResourcePackage/")
        }
    }

    tasks.register<Copy>("publish${type.capitalizeUS()}BinariesToHarmonyAppX64") {
        group = "harmony"
        description =
            "Publish ohosX64 libkn to harmonyApp (x86_64 emulator). " +
                "Currently blocked by toolchain: Konan_cxa_demangle on linkDebugSharedOhosX64."
        dependsOn("link${type.capitalizeUS()}SharedOhosX64")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        into(harmonyAppDir)
        from("build/bin/ohosX64/${type}Shared/libkn_api.h") {
            into("entry/src/main/cpp/include/x86_64/")
        }
        from(project.file("build/bin/ohosX64/${type}Shared/libkn.so")) {
            into("entry/libs/x86_64/")
        }
        val composeResourcePackage =
            "${rootProject.name.lowercase()}.${project.name.lowercase()}.generated.resources"
        from("src/commonMain/composeResources") {
            into("entry/src/main/resources/rawfile/composeResources/$composeResourcePackage/")
        }
    }
}
