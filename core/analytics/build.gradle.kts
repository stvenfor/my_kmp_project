// core:analytics —— 埋点上报（当前为占位骨架，真实 gRPC 上报见 M4）。
// target 集 / android 基础配置 / 公共依赖见根 build.gradle.kts 的共享接线。
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "com.example.my_kmp_project.core.analytics"
}
