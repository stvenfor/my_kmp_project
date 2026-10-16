// core:base —— 最底层通用件：文本工具（core.text）+ UUID（core.uuid，expect/actual）。
// target 集 / android 基础配置 / 公共依赖 / iosMain·ohosMain 层级见根 build.gradle.kts。
//
// uuidCpfMain 是 OHOS 产物缺口造成的中间 source set：CPF 只发了 android/ios/ohosArm64
// 的 uuid klib，ohosX64 没有，所以那边用 stub actual（AppUuid.ohosX64.kt）。
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

val androidOnly = rootProject.findProperty("androidOnly")?.toString()?.toBoolean() == true

kotlin {
    sourceSets {
        val uuidCpfMain = create("uuidCpfMain").apply {
            dependsOn(getByName("commonMain"))
            dependencies {
                implementation(libs.uuid)
            }
        }
        getByName("androidMain").dependsOn(uuidCpfMain)
        if (!androidOnly) {
            getByName("iosMain").dependsOn(uuidCpfMain)
            getByName("ohosArm64Main").dependsOn(uuidCpfMain)
        }
    }
}

android {
    namespace = "com.example.my_kmp_project.core.base"
}
