// feature:classroom —— 课堂列表 / 路由宿主（共享 Compose，无网络层）。
// target 集 / android 基础配置 / coroutines·atomicfu·kotlin-test 公共依赖见根 build.gradle.kts。
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    sourceSets {
        getByName("commonMain").dependencies {
            // core:ui 传递带出 core.platform -> core.design（DemoColors / MineTopBar / Res）
            api(projects.core.ui)
            api(projects.core.router)
        }
    }
}

android {
    namespace = "com.example.my_kmp_project.feature.classroom"
}
