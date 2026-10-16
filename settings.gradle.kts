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
    repositories {
        // mavenLocal()
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
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
