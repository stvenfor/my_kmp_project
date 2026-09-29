## Why

三端后续业务需要统一的 UUID 生成/解析能力，但仓库尚无跨端 UUID 依赖；若各端分别用 `java.util.UUID`、Foundation 或自写实现，行为与调用面会分叉。[CPF-KMP-CMP/uuid](https://gitcode.com/CPF-KMP-CMP/uuid) 已发布带 `ohosArm64` 的 `com.benasher44:uuid` 平行制品，适合作为唯一标准库并经薄封装约束调用方式。

## What Changes

- Catalog 新增 CPF UUID：`com.benasher44:uuid` **`0.8.4-1.0.0`**（eazytec 当前 latest；含 jvm / ios* / `ohosArm64`）。
- `composeApp` `commonMain` 依赖该库；在 `core` 提供**薄封装**（生成随机 UUID、字符串往返），业务禁止直接散落调用底层类型（封装内除外）。
- `commonTest` 增加冒烟测试：生成非空、格式合法、往返一致、两次随机不相等。
- **BREAKING（潜在）**：`ohosX64` 无已发布 klib——若 `commonMain` 直接依赖，`ohosX64` 编译可能失败；设计需与 Coil 同策略（避开或占位），不以 `ohosX64` 冒烟阻塞三端主路径。

## Capabilities

### New Capabilities

- `core-uuid`: 跨端 UUID 契约——统一依赖、薄封装 API、Android/iOS/OHOS arm64 可用、ohosX64 边界、冒烟验收。

### Modified Capabilities

- （无）现有 feature specs 未规定 UUID 实现；行为契约由 `core-uuid` 承载。

## Impact

- **依赖**：`gradle/libs.versions.toml`；`composeApp/build.gradle.kts`（或最终放置封装的模块源集）。
- **代码**：新增薄封装（建议 `core` 包下少量 Kotlin）；暂无现有调用点需迁移。
- **Android / iOS**：解析 `uuid` + `uuid-jvm` / `uuid-ios*`；行为应与 benasher44 上游一致。
- **OHOS**：`ohosArm64` 用 `uuid-ohosarm64`；真机/arm 模拟器可用；`ohosX64` 按 design 规避。
- **外部**：制品来自 [eazytec Maven](https://maven.eazytec-cloud.com/nexus/repository/maven-public/)；源码见 [CPF uuid](https://gitcode.com/CPF-KMP-CMP/uuid)（分支 `main-0.8.4-OH`）。

## Non-goals

- 不引入第二套 UUID 库，不新开独立 Gradle 模块（除非后续抽 `core:uuid` 时自然迁移）。
- 不强制整仓升到其它 CPF `-1.0.0` 工具链（仅钉本库版本）。
- 不做 UUID v1/v6/v7 产品策略、持久化主键迁移、或批量替换业务 ID 字段。
- 不改造 ArkTS/Swift 壳层暴露 UUID API。
