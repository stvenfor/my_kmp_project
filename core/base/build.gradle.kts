// core:base —— 最底层通用工具，被其它模块以 api 传递导出时的依赖靶子（当前为占位骨架）。
// target 集 / android 基础配置 / 公共依赖见根 build.gradle.kts 的共享接线。
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "com.example.my_kmp_project.core.base"
}
