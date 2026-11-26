// core:database —— 结构化 / 实体持久化层（与 core:storage 的 KeyValueStore KV 层互补）。
//
// 边界：core:storage = 键值对（session / flag / 偏好）；core:database = 带主键的实体表
// （可查询、可响应式观察）。二者互不依赖，按需取用。
//
// target 集 / android 基础配置 / 公共依赖（coroutines·atomicfu·serialization）/ iosMain·ohosMain
// 层级全部由根 build.gradle.kts 的 `:core:*` 共享接线提供，本文件只补 namespace 与
// 模块级 test 依赖。
//
// 为什么用 KV 落盘而不是 SQL：OHOS fork 当前没有 Room / SQLDelight 的 klib
// （见 openspec/.../platform-gap-registry.md），强行引 SQL 依赖会挂掉 ohosArm64 / ohosX64。
// PersistentDatabase 把每行实体写成 JSON 存进 core:storage 的 KeyValueStore（三端都落
// 应用沙箱），接口 / 上层业务不变；SQL driver 就位后经 DatabaseProvider 替换即可。
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    sourceSets {
        // 落盘 driver 复用 core:storage 的 KeyValueStore（三端各自的持久化后端）。
        getByName("commonMain").dependencies {
            api(projects.core.storage)
        }
        // commonMain 的 coroutines 是 implementation（不外泄），commonTest 读 StateFlow/Flow
        // 需要显式补一条 coroutines-core（runBlocking / flow.first 都来自它，全 target 有 klib）。
        getByName("commonTest").dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

android {
    namespace = "com.example.my_kmp_project.core.database"
}
