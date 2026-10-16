// feature:ai —— AI 流式对话页 + SSE 接口层（共享 Compose）。
// core.sse（AiStreamEvent / SseParser / AiSseDecoder）住在 :core:network 模块里，
// 所以这里只需要 core.network 一条就能拿到 core.sse.* 与 SseTransport。
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
            api(projects.core.network)
            api(projects.core.router)
            // AiSseApi 公开签名带 JsonObject / buildJsonObject。
            api(libs.kotlinx.serialization.json)
        }
        getByName("commonTest").dependencies {
            implementation(projects.core.network)
        }
    }
}

android {
    namespace = "com.example.my_kmp_project.feature.ai"
}
