## Why

本仓鸿蒙网络走 `net_http` cinterop，Android/iOS 走 Ktor `3.3.1`（无 OH 变体），三端实现分叉、行为易漂移。[CPF-KMP-CMP/ktor](https://gitcode.com/CPF-KMP-CMP/ktor) 已发布带 `ohosArm64` 的平行制品，具备统一 `ApiClient` 实现栈的条件；需先评估改动面与风险再落地。

## What Changes

- 将 catalog 中 Ktor 从 `3.3.1` 升级为 **CPF OH 后缀版本**（默认 `3.3.3-0.3.0`，与当前 Kotlin `2.2.21-0.3.0` 对齐；可选整线升至 `3.3.3-1.0.0` + Kotlin/CMP `-1.0.0`）。
- 让 `ohosMain`（至少 `ohosArm64`）依赖共享 Ktor 客户端源集与 OH 引擎，用 `KtorApiClient`（或等价）替换 `OhosApiClient` / `OhosHttpTransport`。
- 评估并选定 OH 引擎：**CIO（文档推荐）** vs **Curl（HTTPS/TLS 更可能可用）**；不达标则保留 cinterop 为回退。
- 移除或停用 `net_http` cinterop 与相关链接选项（在 HTTPS/并发验收通过后）。
- Android 继续 OkHttp、iOS 继续 Darwin；版本随 CPF Ktor 线一起对齐。
- **BREAKING（潜在）**：若升至 `-1.0.0` 工具链，整仓 Kotlin/CMP/coroutines/serialization 需同线升级；`ohosX64` 模拟器目标可能暂时无法解析 CPF Ktor（已发布 klib 未见 `ohosX64`）。

## Capabilities

### New Capabilities

- `core-network`: 跨端 HTTP/`ApiClient` 的行为契约——平台引擎选择、OHOS 使用 CPF Ktor（含 HTTPS 验收门槛）、与 cinterop 退场条件。

### Modified Capabilities

- （无）现有 `openspec/specs/*` 未对 OHOS 传输层做需求级约束；本次以新 capability 承载。

## Impact

- **模块**：`:core:network`（源集、cinterop、bootstrap）、`:composeApp` 链接选项（若去掉 `-lnet_http`）、catalog `libs.versions.toml`。
- **Android / iOS**：依赖版本 bump；引擎保持 OkHttp/Darwin，行为应接近不变，需回归登录/业务 API。
- **OHOS**：传输实现替换；二进制体积（尤其 Curl+OpenSSL）与 TLS/并发特性变化；`publish*BinariesToHarmonyApp` 后需真机验收。
- **依赖生态**：Coil 若上 OH，须对齐 CPF `coil-*-0.3.0/1.0.0` 与同线 Ktor；否则继续排除 OH Coil。
- **外部**：制品来自 [eazytec Maven](https://maven.eazytec-cloud.com/nexus/repository/maven-public/)；依据 [CPF 三方库文档](https://gitcode.com/CPF-KMP-CMP/docs/blob/main/zh-cn/深入开发/三方库.md)。

## Non-goals

- 不在本变更中强制升级整仓到 CPF `-1.0.0`（仅作为可选路径写入 design）。
- 不把 Ktor Server、WebSocket/SSE 新产品化（除非现有调用已依赖）。
- 不改造 ArkTS 壳或 NAPI 入口。
- 不一次性适配全部 CPF 三方库（Settings/Coil 等另案）。
