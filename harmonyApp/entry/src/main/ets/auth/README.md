# 华为账号一键登录（ArkTS）

页面入口：`pages/Index.ets` → `HuaweiQuickLoginPane`。

- `AuthConfig.ets` — BFF 基址（对齐 `.env.lan`）
- `AuthApiClient.ets` — `POST /api/v1/user/huawei/login`
- `SessionStore.ets` — token / session 本地持久化
- `HuaweiQuickLoginPane.ets` — Account Kit 按钮 + 协议

服务端与 AGC 待办见 Go 仓 `docs/huawei-account-login.md`。
