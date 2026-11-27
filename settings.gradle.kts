rootProject.name = "My_kmp_project"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        // mavenLocal()
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // 仓库只允许在 settings 这一处声明；模块内再写 repositories {} 直接失败。
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    // 顺序必须钉死：
    // - eazytec 独有 OHOS fork 坐标（org.jetbrains.androidx.*:*-0.4.0 等），必须排第一；
    //   但 eazytec 不代理 AGP 二进制（aapt2 会 socket 报错），所以排除 com.android.tools.build。
    // - 阿里云镜像作为通用回退；google() / mavenCentral() 兜底。
    repositories {
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/") {
            mavenContent { excludeGroup("com.android.tools.build") }
        }
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        google()
        mavenCentral()
    }
}

include(":composeApp")
include(":core:network")
include(":core:account")
include(":core:router")
include(":core:design")
include(":core:base")
include(":core:analytics")
include(":core:storage")
include(":core:platform")
include(":core:ui")
include(":core:audio")
include(":core:database")
include(":component")

// feature 叶子模块：模块之间禁止相互依赖（R7 + check-layer-deps.sh 第 1/3 条），
// 每个都能单独 `./gradlew :feature:x:testDebugUnitTest`。
listOf(
    "content", "web", "live", "scan", "pay", "wallet",
    "ai", "classroom", "friend", "commerce",
    "community", "media", "chat", "shell",
    "auth", "mine", "home",
).forEach { include(":feature:$it") }
include(":ohosAggregate")
