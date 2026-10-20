// core:database —— 结构化 / 实体持久化层（与 core:storage 的 KeyValueStore KV 层互补）。
//
// 边界：core:storage = 键值对（session / flag / 偏好）；core:database = 带主键的实体表
// （可查询、可响应式观察）。二者互不依赖，按需取用。
//
// target 集 / android 基础配置 / 公共依赖（coroutines·atomicfu·serialization）/ iosMain·ohosMain
// 层级全部由根 build.gradle.kts 的 `:core:*` 共享接线提供，本文件只补 namespace 与
// 模块级 test 依赖。
//
// 为什么默认是 commonMain 内存实现：OHOS fork 当前没有 Room / SQLDelight 的 klib
// （见 openspec/.../platform-gap-registry.md），强行引 SQL 依赖会挂掉 ohosArm64 / ohosX64。
// 因此这里先落「接口 + 内存 driver + DatabaseProvider 缝」，等平台 SQL driver 就位后经
// DatabaseProvider 替换即可，上层业务无感。
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    sourceSets {
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
