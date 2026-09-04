# ⚠ Boundary gate — 恢复 parity 实施前必读

> **在继续本 change 的像素/行为对等实施之前，必须满足 architect 五层边界。**

尖刀期对等 UI 曾冻结。尖刀 II 后已**解冻**，但仅允许在新边界内改。

| 必读 | 路径 |
|------|------|
| **解冻政策（权威）** | [`../architect-production-foundation/notes/parity-unfreeze.md`](../../architect-production-foundation/notes/parity-unfreeze.md) |
| ADR | `docs/adr/0001-production-architecture.md` |
| 包归属 | `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/MODULES.md` |
| 依赖门禁 | `bash scripts/check-layer-deps.sh`（feature↛feature） |

## 一句话

跨 feature 用 `AppNavigator` / `component/*` / `core`；支付与推送不进 feature；tokens 走 `DesignSystem` / `core.design`；**禁止**回到 Home 本地 hub 堆 feature import。

未读解冻政策、未跑通 `check-layer-deps.sh` ⇒ **不得**声称 parity 任务可合入。
