// feature:wallet —— 钱包 / 卡包 HTTP 层（纯网络 + 序列化模型，无 Compose UI）。
// target 集 / android 基础配置 / coroutines·atomicfu·serialization·kotlin-test 见根 build.gradle.kts。
// 不挂 compose 插件：本模块一个 @Composable 都没有，挂了会让 compose 编译器在
// 「runtime 不在 classpath」时直接抛 IncompatibleComposeRuntimeVersionException（实测 :feature:pay / :feature:wallet）。
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    // WalletModels 用 @Serializable。
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        getByName("commonMain").dependencies {
            api(projects.core.network)
            // JsonElement 出现在公开模型签名里 → api；:core:network 只 implementation。
            api(libs.kotlinx.serialization.json)
        }
        getByName("commonTest").dependencies {
            implementation(projects.core.network)
        }
    }
}

android {
    namespace = "com.example.my_kmp_project.feature.wallet"
}
