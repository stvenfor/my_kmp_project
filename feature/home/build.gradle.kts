// feature:home —— 首页 + 待办/积分/流水/新车跟进/售后 + Vico 图表。
// target 集 / android 基础配置 / coroutines·atomicfu·serialization·kotlin-test 见根 build.gradle.kts。
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    sourceSets {
        // Vico 只在 Android + iOS 可用（CPF 无 ohos 产物），所以图表实现放中间 source set，
        // commonMain 只留 expect；ohosMain 用原生占位 actual。
        val chartsVicoMain = maybeCreate("chartsVicoMain").apply {
            dependsOn(getByName("commonMain"))
            dependencies {
                // Vico 2.3 传递的是 stock Compose 1.9.2；工程自己的 compose.* 由 commonMain 胜出。
                implementation(libs.vico.compose)
            }
        }
        getByName("androidMain").dependsOn(chartsVicoMain)
        getByName("androidMain").dependencies {
            // 数据分析 gRPC：okhttp 传输 + stub unary 调用 + javalite 手写编解码（无 protoc）
            implementation(libs.grpc.okhttp)
            implementation(libs.grpc.stub)
            implementation(libs.protobuf.javalite)
        }
        // iosMain 由根工程共享接线创建，这里只挂中间 source set（androidOnly 时它不存在）。
        matching { it.name == "iosMain" }.all { dependsOn(chartsVicoMain) }

        getByName("commonMain").dependencies {
            // core:ui 传递带出 core.platform → core.design / core.account + compose.*
            api(projects.core.ui)
            api(projects.core.router)
            api(projects.core.network)      // 含 core.sse 包
            api(projects.component)         // component.webview / component.routing
        }
        getByName("commonTest").dependencies {
            implementation(projects.core.network)
        }
    }
}

android { namespace = "com.example.my_kmp_project.feature.home" }
