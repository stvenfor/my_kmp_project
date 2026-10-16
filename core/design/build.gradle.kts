// core:design —— 设计令牌 SoT（配色/尺寸，配 SoT 测试）。
// target 集 / android 基础配置 / 公共依赖见根 build.gradle.kts 的共享接线。
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "com.example.my_kmp_project.core.design"
}
