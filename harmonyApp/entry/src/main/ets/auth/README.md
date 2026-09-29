# 华为账号一键登录（ArkTS UI + Kotlin BFF）

页面入口：`pages/Index.ets` → `HuaweiQuickLoginPane`。

- `AuthConfig.ets` — 协议 URL 等壳层常量（BFF 基址由 Kotlin `NetworkConfig` 负责）
- `AuthApiClient.ets` — 薄封装：调用 `libentry` → Kotlin `AuthBridge` / `AuthRepository`（kotlinx-serialization）
- `SessionStore.ets` — 壳层展示缓存（非会话真相源；真相源为 Kotlin `AccountFacade`）
- `HuaweiQuickLoginPane.ets` — Account Kit 按钮 + 协议；授权码交给 Kotlin 换票

服务端与 AGC 待办见 Go 仓 `docs/huawei-account-login.md`。
