## Why

网络图目前靠 `PlatformNetworkImage`：Android/iOS 用上游 Coil `3.3.0`，OHOS 无 `ohosArm64` 只能本地占位，三端行为不一致。[CPF-KMP-CMP/coil](https://gitcode.com/CPF-KMP-CMP/coil) 已发布带 OH 的平行制品；Ktor 已走 CPF `-0.3.0` 线，现在对齐 Coil 可让公共组件统一封装并真正加载远程图。

## What Changes

- Catalog 将 Coil 从 `3.3.0` 升级为 **CPF OH 后缀**（默认 `3.3.0-0.3.0`，对齐本仓 Kotlin/Ktor `-0.3.0`）。
- 在 `core.ui`（公共组件）封装统一网络图 API（Compose 工具封装，非裸 `AsyncImage` 散落调用）。
- 用该封装替换现有 `PlatformNetworkImage` expect/actual 及所有业务调用点；`ohosArm64` 启用 Coil + `coil-network-ktor3`。
- Android 继续 OkHttp 网络插件；iOS/OH 用 Ktor3 插件，与现有 CPF Ktor 同线。
- **BREAKING（潜在）**：若误升至 `-1.0.0` 且与 Kotlin 线错配会解析失败；默认钉在 `-0.3.0`。`ohosX64` 若无 CPF Coil 变体则保留占位回退。

## Capabilities

### New Capabilities

- `core-image`: 跨端网络图片加载契约——公共封装 API、占位/失败态、OHOS arm64 使用 CPF Coil、与本地 `painterResource` 边界。

### Modified Capabilities

- （无）现有 feature specs 未规定图片加载实现；行为契约由 `core-image` 承载。

## Impact

- **依赖**：`gradle/libs.versions.toml` Coil 坐标；`composeApp/build.gradle.kts` 源集（含 `ohosArm64Main`）。
- **代码**：`PlatformNetworkImage*` → 公共封装；调用方：`HomeRouteHost`、`CommunityRouteHost`、`FriendScreen`、`NativeJetpackTabs` 等。
- **Android / iOS**：版本 bump + 统一走封装；视觉应接近不变，需回归头像/封面/社区预览。
- **OHOS**：arm64 真机/模拟器应能显示远程图；需 `publish*BinariesToHarmonyApp` 后验收。
- **外部**：制品来自 [eazytec Maven](https://maven.eazytec-cloud.com/nexus/repository/maven-public/)；源码见 [CPF Coil](https://gitcode.com/CPF-KMP-CMP/coil)。

## Non-goals

- 不替换本地 `composeResources` / `painterResource`（启动图、图标、SoT 静态体等）。
- 不强制整仓升到 CPF `-1.0.0` 工具链。
- 不引入第二套图片库，不新开独立 Gradle 模块（除非后续抽取 `core:design` 时自然迁移）。
- 不做磁盘缓存策略大改、GIF/视频帧解码产品化。
