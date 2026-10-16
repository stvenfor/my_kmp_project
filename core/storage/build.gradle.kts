// core:storage —— 本地存储（当前为占位骨架，KV 实现见 P3 core:persistence）。
// target 集 / android 基础配置 / 公共依赖见根 build.gradle.kts 的共享接线。
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "com.example.my_kmp_project.core.storage"
}
