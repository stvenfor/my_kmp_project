// core:router —— 路由常量、路径解析、深链与导航状态（无 @Composable，只用到 compose.runtime）。
// target 集 / android 基础配置 / 公共依赖见根 build.gradle.kts 的共享接线。
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    sourceSets {
        getByName("commonMain").dependencies {
            api(compose.runtime)
        }
    }
}

android {
    namespace = "com.example.my_kmp_project.core.router"
}
