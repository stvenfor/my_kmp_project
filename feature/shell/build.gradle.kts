// feature:shell —— 主 Tab 底栏 / 二级 Compose 岛 / Soft Auth 呈现层。
//
// 它是「壳的形状」而不是某个业务域：MainBottomBar 只在 Tab 根出现，SecondaryRouteIsland
// 承载「我的」系二级页，DeferredDestinationStub 是未排期路由的统一占位。放成独立模块后
// iOS/Android/Harmony 三端壳都只依赖这一个形状，不再从 :composeApp 里捞内部符号。
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    sourceSets {
        getByName("commonMain").dependencies {
            api(projects.core.ui)
            api(projects.core.router)
            api(projects.component)
        }
    }
}

android {
    namespace = "com.example.my_kmp_project.feature.shell"
}
