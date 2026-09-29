# ADR 0004 — Kotlin-owned BFF JSON via kotlinx-serialization

- **Status:** Accepted (grill 2026-09-29)
- **Date:** 2026-09-29

## Context

CPF `kotlinx-serialization`（`1.9.1-0.3.0`）已覆盖 OHOS Kotlin。仓库里网络层与 Auth 仍大量手解 `JsonElement`，且 Harmony Auth UI 在 ArkTS 用 `JSON.parse` 平行打 BFF，与 iOS「壳 UI → AuthBridge → Kotlin」不一致。看起来像「Harmony 用不了 serialization」，实际是语言边界被当成了业务分叉。

## Decision

1. **全部** BFF JSON（标准信封、请求体、业务 `data`）在共享 Kotlin 用 `@Serializable` + 共用 `NetworkJson`；`ApiClient` 以 reified 编解码为主路径，调用方不碰 `JsonElement`。
2. 非标准信封（OAuth/401 等）只保留 Kotlin 侧 **Envelope Fallback**，不在壳层解析。
3. Native Shell（含 Harmony）经 `AuthBridge`（或等价导出）登录；ArkTS 可保留 Account Kit 拿授权码，但 `huawei/login` 等 BFF 仍进 Kotlin。
4. **Account Session SoT** 在 Kotlin；停掉壳内平行权威会话存储。
5. 唯一「实在用不了」：纯 ArkTS/Swift 运行时无法加载 Kotlin 序列化库——用「调用进 Kotlin」解决，而不是再写一套 JSON。

## Consequences

- Harmony `AuthApiClient.ets` 的 BFF 路径应退役；与 iOS 对齐到 Bridge。
- 信封特例逻辑仍集中在 `core:network`，业务 feature 只声明 DTO。
