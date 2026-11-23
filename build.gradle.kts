plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}

// 跨平台一键部署/校验任务（借鉴 kmp-cmp-example/deployment.gradle.kts 并按本工程适配）。
//
// 注意：wrapper 已升到 Gradle **9.4.1**，`Project.exec` 及脚本级 `exec` 在 Gradle 9.0 被移除
// （官方迁移指南 "Removal of Project#exec, Project#javaexec, and script-level counterparts"），
// deployment.gradle.kts 内 29 处已全部改为注入的 `ExecOperations` 服务
// （`execOperations.exec { ... }`，API 与旧写法一致；取用方式见该文件顶部 ExecOperationsHolder）。
// 这些任务都是 doLast 里直接起进程，与 configuration cache 不兼容，且依赖真机/模拟器，
// 门禁从不跑它们（三端日常跑用 scripts/run-{android,ios,harmony}.sh）。
apply(from = "deployment.gradle.kts")

// ---------------------------------------------------------------------------
// core:* / feature:* 库模块的共享 KMP 接线
//
// 为什么放根工程而不是 buildSrc：这台机器被 ~/.gradle/init.gradle 钉在阿里云 +
// eazytec 镜像上，`org.gradle.kotlin.kotlin-dsl` 的 plugin marker 在这些源里解析不到
// （已实测 `:help` 直接失败），所以 buildSrc 约定插件这条路走不通。
// 根工程 scripts classpath 上已经有 KGP/AGP（上面 alias(...) apply false 提供），
// 因此可以用类型化 API 直接配置子工程。
//
// 这里只放 100% 共用的部分：target 集 + android 基础配置 + iosMain/ohosMain 层级 +
// 最底层公共依赖。namespace、能力型中间 source set（ktor / coil / uuid / vico /
// settings）、Compose 插件都留给模块自己声明。
// ---------------------------------------------------------------------------
val libraryModulePaths: (Project) -> Boolean = { it.path.startsWith(":core:") || it.path.startsWith(":feature:") }

subprojects {
    if (!libraryModulePaths(this)) return@subprojects

    // 库模块的仓库顺序必须显式钉死，两个源各有对方没有的东西：
    // - OHOS fork 独有坐标（org.jetbrains.androidx.lifecycle:lifecycle-common:2.9.4-0.4.0 这类）
    //   只在 eazytec；阿里云对它是 socket/SSL 报错，而 Gradle 把传输错误判成整次解析失败、
    //   不会接着试下一个仓库，所以 eazytec 必须排第一。
    // - eazytec 不代理 AGP 二进制（aapt2 同样报 socket 错），所以把它对
    //   com.android.tools.build 排除掉，让 aapt2 落到阿里云的 google 镜像。
    // settings 的 dependencyResolutionManagement 默认 PREFER_PROJECT：工程级 repositories 生效。
    repositories {
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/") {
            mavenContent { excludeGroup("com.android.tools.build") }
        }
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        google()
        mavenCentral()
    }

    val skipNative = rootProject.findProperty("androidOnly")?.toString()?.toBoolean() == true

    // VersionCatalogsExtension 要到子工程自己求值时才注册，所以只能在回调体里取。
    fun versionCatalog() = extensions
        .getByType(org.gradle.api.artifacts.VersionCatalogsExtension::class.java)
        .named("libs")

    // 共享接线挂在 com.android.library 应用之后（模块插件块一律
    // kotlinMultiplatform 在前、androidLibrary 在后）：
    // - 挂在 kotlin.multiplatform 上会失败，此时 AGP 还没应用，`androidTarget()` 直接报
    //   "Please apply the Android Gradle Plugin"；
    // - 挂在 afterEvaluate 上也失败，androidTarget 建得太晚，AGP 的 debug/release variant
    //   找不到对应 Kotlin 编译（"KotlinJvmAndroidCompilation with name 'debug' not found"）。
    plugins.withId("com.android.library") {
        val catalog = versionCatalog()
        extensions.configure(com.android.build.gradle.LibraryExtension::class.java) {
            compileSdk = catalog.findVersion("android-compileSdk").get().requiredVersion.toInt()
            defaultConfig {
                minSdk = catalog.findVersion("android-minSdk").get().requiredVersion.toInt()
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }

        val kmp = extensions.getByType(
            org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension::class.java,
        )

        kmp.androidTarget {
            @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
            compilerOptions {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            }
        }

        if (!skipNative) {
            // 库模块一律不产 framework / sharedLib：整个工程只有 :composeApp
            // 是二进制产出方（iOS framework + OHOS libkn.so）。
            kmp.iosX64()
            kmp.iosArm64()
            kmp.iosSimulatorArm64()
            kmp.ohosArm64()
            kmp.ohosX64()
        }

        // 序列化：core:* 与 feature:* 里的 @Serializable 数据契约满地都是（114 行 API 契约表
        // 每一行都要一个请求/响应模型）。以前只有 :composeApp 单独 apply 过插件，拆模块后
        // 每个模块重复声明插件是噪声、漏声明是编译红，所以插件和 json 依赖都在这条共享接线里给。
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
        with(kmp.sourceSets) {
            getByName("commonMain").dependencies {
                implementation(catalog.findLibrary("kotlinx-coroutines-core").get())
                implementation(catalog.findLibrary("atomicFu").get())
                api(catalog.findLibrary("kotlinx-serialization-json").get())
            }
            getByName("commonTest").dependencies {
                implementation(catalog.findLibrary("kotlin-test").get())
            }

            if (!skipNative) {
                val commonMain = getByName("commonMain")
                val iosMain = maybeCreate("iosMain").apply { dependsOn(commonMain) }
                listOf("iosX64Main", "iosArm64Main", "iosSimulatorArm64Main").forEach {
                    getByName(it).dependsOn(iosMain)
                }
                val ohosMain = maybeCreate("ohosMain").apply { dependsOn(commonMain) }
                getByName("ohosArm64Main").dependsOn(ohosMain)
                getByName("ohosX64Main").dependsOn(ohosMain)
            }
        }
    }
}
