# feature 拆模块 · agent 共用作业说明（P4-b）

读这一段就够，不用再翻仓库历史。目标：把 `composeApp/src/**/feature/<name>/` 变成一个真实的
Gradle 模块 `feature/<name>/`（模块名 `:feature:<name>`），因为用户要求「feature 模块互不依赖、
按需引入、可独立测试」。

## 必须遵守

1. **只碰分给你的 feature 目录**（源码 + 对应 commonTest）。禁止改 `settings.gradle.kts`、
   `composeApp/**`、`core/**`、`component/**`、根 `build.gradle.kts`、脚本 —— 这些由主 agent 串行接线。
2. **不要跑 `./gradlew`**。Gradle 在本仓必须串行，编译验收由主 agent 统一跑一轮门禁。
   你的自检只能是静态检查（见「自检」）。
3. `git mv` 保留历史；包名一律不变（`com.example.my_kmp_project.feature.<name>`）。
4. 移动后把目录里**所有**顶层 `internal` 声明改成 public（跨模块后 internal 不可见，这是两轮已踩的坑）。
   同文件内的私有辅助可以留 internal，但拿不准就 public。
5. 若某文件 import 了 `com.example.my_kmp_project.feature.<别的 feature>`：**停下并上报**，
   不要自己造端口（门禁 R7 已保证现状为 0 条，出现即说明你搬错了目录）。

## 模块目录形态

```
feature/<name>/
  build.gradle.kts
  src/commonMain/kotlin/com.example.my_kmp_project/feature/<name>/*.kt
  src/commonTest/kotlin/com.example.my_kmp_project/feature/<name>/*.kt
```

`build.gradle.kts` 模板（`:feature:*` 已被根 `subprojects{}` 覆盖：target 集、android 基础配置、
coroutines/atomicfu/kotlin-test 公共依赖都自动到位，所以模块文件只写这些）：

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    sourceSets {
        getByName("commonMain").dependencies {
            api(projects.core.ui)      // 传递带出 core.platform -> core.design / core.account
            // ↓ 按你的清单增删，不要全量塞
            api(projects.core.router)
            api(projects.core.network)
            api(projects.component)
        }
        getByName("commonTest").dependencies {
            implementation(projects.core.network)
        }
    }
}

android { namespace = "com.example.my_kmp_project.feature.<name>" }
```

注意：`api(compose.*)` 已由 `:core:ui` 以 api 方式传递，不要在 feature 里重复声明 compose。

## 自检（静态，不许跑 gradle）

- `find feature/<name> -name '*.kt' | wc -l` 与搬运前一致；
- `grep -rn "^internal " feature/<name>` 应为 0；
- `grep -rn "import com.example.my_kmp_project.app\." feature/<name>` 应为 0；
- `grep -rn "generated.resources" feature/<name>`：Res 必须来自 `core.design`，若出现
  `composeapp.generated.resources` 说明漏改，改成 `my_kmp_project.core.design.generated.resources`。

## 回报格式（≤12 行）

搬动文件数 / 测试文件数、internal→public 处数、模块依赖清单、你判断需要但清单里没有的依赖、
静态自检四条的输出结果。
