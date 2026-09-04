# Spike II — 仍 Partial / 未关门项

便于 architect 关门对照。权威 Must 表见 [`must-matrix.md`](must-matrix.md)；退出审查见 [`spike-i-exit-review.md`](spike-i-exit-review.md)。

| 项 | tasks | 状态 | 说明 |
|----|-------|------|------|
| Pay sandbox ≥1 channel | 4.1 | **Partial** | `SandboxPayChannelAdapter` + flags；真 WeChat/Alipay OpenSDK android 仍 TODO null；非生产扣款 |
| Push token + click → deep link | 4.2 | **Partial** | common `PushBridge` + 三端 `PlatformPushSdk` stub；**厂商 SDK 未接线** ⇒ Must 未满 |
| `:core:network` + `:core:account` + OHOS aggregate | 4.3 | **Open** | Gradle 抽模块 / OHOS 聚合未完成（Modules 轨） |
| DesignSystem alias（Mine/Membership） | 4.4 | Done (lightweight) | 可继续加深采用；非阻塞解冻 |
| Flutter parity unfreeze | 4.5 | **Done（政策）** | 见 [`parity-unfreeze.md`](parity-unfreeze.md)；非完整 1A |
| Home 层违规 / hub 迁出 | 2.4 遗留 | **Partial** | `check-layer-deps` 与 Home/Mine hub 清理由 Layer 轨继续；新 parity **不得新增** feature→feature |

**关门建议：** Must Pay/Push 在真机 SDK 接线前保持 Partial；Nice 全量 1A 走解冻后的 `parity-flutter-to-kmp`，且必须过 boundary-gate。
