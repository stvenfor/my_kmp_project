
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import android.databinding.tool.ext.capitalizeUS
import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

// GitHub Actions Android CI: ./gradlew ... -PandroidOnly=true (skip iOS/OHOS targets)
val androidOnly = rootProject.findProperty("androidOnly")?.toString()?.toBoolean() == true

// ---------------------------------------------------------------------------
// Release 签名 / 版本号：本机私有配置，不写进仓库。
//
// 读取优先级：-P<key>=... > local.properties > 环境变量。
// local.properties 已被 .gitignore 排除，是推荐的落盘位置；CI 用环境变量注入。
// 四项签名参数齐全才创建 release signingConfig，缺任何一项时 release 仍可
// assemble（产出未签名包，与改动前行为一致），不会让本机构建直接失败。
//
// 键名：release.storeFile / release.storePassword / release.keyAlias / release.keyPassword
// 版本：app.versionCode / app.versionName
// ---------------------------------------------------------------------------
val privateProperties: Properties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}

fun privateValue(key: String, env: String): String? =
    (rootProject.findProperty(key) as String?)?.takeIf { it.isNotBlank() }
        ?: privateProperties.getProperty(key)?.takeIf { it.isNotBlank() }
        ?: System.getenv(env)?.takeIf { it.isNotBlank() }

val releaseStoreFilePath = privateValue("release.storeFile", "ANDROID_RELEASE_STORE_FILE")
val releaseStorePasswordValue = privateValue("release.storePassword", "ANDROID_RELEASE_STORE_PASSWORD")
val releaseKeyAliasValue = privateValue("release.keyAlias", "ANDROID_RELEASE_KEY_ALIAS")
val releaseKeyPasswordValue = privateValue("release.keyPassword", "ANDROID_RELEASE_KEY_PASSWORD")
val releaseKeystoreConfigured = listOf(
    releaseStoreFilePath, releaseStorePasswordValue, releaseKeyAliasValue, releaseKeyPasswordValue,
).all { it != null }

val appVersionCode: Int = privateValue("app.versionCode", "APP_VERSION_CODE")?.toIntOrNull() ?: 1
val appVersionName: String = privateValue("app.versionName", "APP_VERSION_NAME") ?: "1.0"

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

    if (!androidOnly) {
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
                // Do NOT use moduleIncludes/outputModule here: that makes libkn DT_NEEDED on
                // libruntime.so + libstd.so without shipping those SOs, so HAP dlopen fails.
                // (emitRuntime/emitStdlib also dropped @CName / CAdapter exports in 2.2.21-0.3.0.)
                // Prefer one self-contained libkn.so with KN runtime linked in.
                linkerOpts("-lz", "-lavplayer")
                // net_http only needed when :core:network still uses cinterop (ohosX64 emulator).
                // ohosArm64 uses CPF Ktor CIO — no -lnet_http.
                if (ohosTarget.name == "ohosX64") {
                    linkerOpts("-lnet_http")
                }
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
                // netHttp cinterop moved to :core:network
            }
        }
    }

    sourceSets {
        // 能力型依赖全部下沉：CPF uuid → :core:base，Coil 网络图 → :core:ui，
        // CameraX/MLKit 扫码 → :core:platform，Vico 图表 → :feature:home。
        // :composeApp 只剩 app/ 装配 + 三端入口，所以这里不再创建任何中间 source set。
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.splashscreen)
        }
        commonMain.dependencies {
            implementation(projects.component)
            implementation(projects.core.network)
            implementation(projects.core.account)
            implementation(projects.core.router)
            implementation(projects.core.design)
            implementation(projects.core.base)
            implementation(projects.core.analytics)
            implementation(projects.core.storage)
            implementation(projects.core.platform)
            implementation(projects.core.ui)
            // app/ 是唯一允许看见全部 feature 的装配层；这条列表与 scripts/check-layer-deps.sh
            // 的「feature 不得互相依赖」配套：跨功能跳转只能在这里接线。
            implementation(projects.feature.ai)
            implementation(projects.feature.auth)
            implementation(projects.feature.chat)
            implementation(projects.feature.classroom)
            implementation(projects.feature.commerce)
            implementation(projects.feature.community)
            implementation(projects.feature.content)
            implementation(projects.feature.friend)
            implementation(projects.feature.home)
            implementation(projects.feature.live)
            implementation(projects.feature.media)
            implementation(projects.feature.mine)
            implementation(projects.feature.pay)
            implementation(projects.feature.scan)
            implementation(projects.feature.shell)
            implementation(projects.feature.wallet)
            implementation(projects.feature.web)
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
            ohosMain.dependencies {
                api(libs.compose.multiplatform.export)
            }
            val ohosArm64Main by getting {
                dependsOn(ohosMain)
            }
            val ohosX64Main by getting {
                dependsOn(ohosMain)
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        sourceSets.getByName("androidUnitTest").dependencies {
            implementation(libs.kotlin.test)
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
        // 版本号来自本机私有配置（见文件顶部），默认 1 / "1.0"
        versionCode = appVersionCode
        versionName = appVersionName
    }
    buildFeatures {
        buildConfig = false
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        if (releaseKeystoreConfigured) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFilePath!!)
                storePassword = releaseStorePasswordValue
                keyAlias = releaseKeyAliasValue
                keyPassword = releaseKeyPasswordValue
            }
        }
    }
    buildTypes {
        getByName("release") {
            // 未配置 keystore 时为 null → 产出未签名包（与改动前一致），不阻断构建
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            // 不开启 shrinkResources：Compose Multiplatform 资源在 assets 里
            // 运行时按名字读取，资源收缩可能误删。
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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


// OHOS rawfile 资源投递清单：composeResources 已经归属各库模块（:core:design 持有全部
// drawable/font），:composeApp 自己不再有 src/commonMain/composeResources。
// 生成的 Res 包名 = <rootProject>.<模块名>.generated.resources，harmonyApp 侧按这个目录名
// 读资源，所以「模块目录」和「包名」必须成对维护；将来 feature 自带资源就往这个表里加一行。
val harmonyResourceRoots: List<Pair<String, File>> = listOf(
    "my_kmp_project.core.design.generated.resources" to
        rootProject.file("core/design/src/commonMain/composeResources"),
)

// 为不同类型(debug、release)OHOS构建注册Copy任务并发布到Harmony App目录
// CPF 对齐：真机 arm64 用 publish*BinariesToHarmonyApp；
// 模拟器 x86_64 用 publish*BinariesToHarmonyAppX64（需 linkDebugSharedOhosX64 成功）。
// 参见 https://gitcode.com/CPF-KMP-CMP/kmp-cmp-example
if (!androidOnly) {
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
            harmonyResourceRoots.forEach { (pkg, dir) ->
                from(dir) {
                    into("entry/src/main/resources/rawfile/composeResources/$pkg/")
                }
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
            harmonyResourceRoots.forEach { (pkg, dir) ->
                from(dir) {
                    into("entry/src/main/resources/rawfile/composeResources/$pkg/")
                }
            }
        }
    }
}
