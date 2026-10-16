// feature:content —— 内容路由宿主（R7：目标页归属兄弟 feature，渲染表住在装配层 component.routing）。
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
            // CrossFeatureScreens（装配层路由表）+ compose.runtime 由此传递带出。
            api(projects.component)
            api(projects.core.router)
        }
    }
}

android {
    namespace = "com.example.my_kmp_project.feature.content"
}
