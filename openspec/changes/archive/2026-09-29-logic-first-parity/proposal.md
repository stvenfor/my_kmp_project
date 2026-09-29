## Why

Full Parity Phase 刚以 Android Screenshot Diff Gate（含大量 Flutter SoT 位图）把域票 #2–#24 收口，但许多页是「像素对齐、业务空心」——mock/状态机/交互深度仍未与 Flutter `my_ai_project` 同级。继续刷 UI 边际收益低，且会掩盖逻辑债。现在把验收主轴切到 **Shared Presentation Logic**，**暂缓**像素门禁。

## What Changes

- **BREAKING（验收口径）**: 路由「完成」不再要求 Android Screenshot Diff Gate ≤2%；该门禁降级为可选回归，直到本阶段结束再恢复。
- 新增 **Logic Acceptance Packet**：以 Flutter 同域行为为 SoT（状态机、列表/详情数据形状、主交互、toast-only 规则、Soft Auth、诚实 PayGateway），用 `commonTest` + 可复现手工脚本验收。
- 更新 `CONTEXT.md` / ADR 0003 / `parity-inventory` 状态语义：区分 `logic-pass` 与既有 `packet-pass`（像素包）。
- 禁止新增 SoT body bitmap / 像素精修作为本阶段交付；已有证据保留不删。
- Android / iOS / OHOS：开放路径仍须可走；逻辑尽量落在共享 Kotlin（UiState/UseCase/mock engine），壳只渲染或薄桥接。

## Non-goals

- 不重开已关 GitHub 域票的像素证据重测。
- 不接入真 SDK（融云/微信/SSE/OHOS 相机等）——仍走 `platform-gap-registry`。
- 不改 ADR 0002 所有权（Native Shell + Mine Island）。
- 不做新一轮全仓 UI 视觉改版或替换 Mine Island。

## Capabilities

### New Capabilities

- `parity/logic-acceptance`: Logic-first 验收包定义、与像素门禁的关系、inventory 状态与禁止事项。

### Modified Capabilities

- （无现有 `openspec/specs/` 主规格需改；Full Parity 行为以 change notes + CONTEXT 为准，本变更用新 capability 固化逻辑优先口径。）

## Impact

- **Docs / process**: `CONTEXT.md`、ADR 0003 后果、`parity-inventory.md`、可选 GitHub #1 策略评论。
- **Code (apply 阶段)**: 共享 feature 层 mock/engine/ViewModel；`commonTest` 行为用例；不强制改原生壳像素。
- **Android / iOS / OHOS**: 三端打开路径保持；逻辑对齐优先 commonMain，壳侧仅在无共享层时补齐。
- **Tooling**: `scripts/screenshot_diff_gate.py` 保留但非硬门槛。
