# 同步 my_ai_project 全量内容到 my_kmp_project（Full Parity Sync）

## Why

`my_ai_project`（Flutter 唯一产品真相源）HEAD 已到 `d7876a1`，相对 KMP 侧 `parity-flutter-to-kmp` 基线又出现演进：真融云 IMLib（`b70315c`/`90c7b46`）、401/403 会话决策统一（`d7876a1`）、新车跟进成交 UI（`fc99691`）、iHome 品牌文案（`622c7cb`）、LAN 后端按设备分流（`10654a9`）。同时前序 change 仍余 90 项待办（以 iOS/Harmony 展开与真 SDK 为主）。

本 change 以 **Flutter 路由/入口表为唯一待办真相源**（`commons/wys_router/lib/src/route/route_path.dart`，103 条），把 my_ai_project 全量产品面按 KMP 架构与语法要求同步落地。

## What Changes

1. **全量路由清单**（`notes/route-inventory.md`）：103 条 RoutePath → Flutter 页面/类 → 模块 → KMP 落点 → 三端状态。
2. **超细任务拆分**（`tasks-ultra-fine.md`）：按「可点路径 + 数据契约 + 三端」定义完成，逐路由展开 UI 区块 / 数据契约 / 交互 / 导航 / 平台 actual / 证据。
3. **Flutter drift 同步**（新增 L 段）：真 IM 收发与门店群补拉、401/403 与 token 刷新、LAN 后端分流、环境服务、发票页、iHome 文案。
4. **厂商 SDK 缺口**逐条登记 `platform-gap-registry`（沿用前序表），不伪称 ready。
5. 设计令牌/通用 chrome 对齐 Flutter `AppTheme`，替换 Demo 色板残留。

## Boundaries（硬规则，逐项任务前置）

- **R1** 非「我的」页 → 各端原生壳：Android Compose 壳 / iOS SwiftUI / OHOS ArkTS + `NativeFeatureCatalog`。
- **R2** 「我的」二级及子页 → 三端共享 Compose 岛（`MineIsland` / `SecondaryRouteIsland`）。
- **R3** 共享业务逻辑 → `commonMain` + expect/actual；页面状态机不默认算进 UI。
- **R4** 分层多模块（`core/*` 与 `feature/*` 与 `:composeApp` 同级注册）：feature 模块之间禁止相互依赖、按需引入、可独立跑单测；但 `libkn.so` / iOS framework 的**产出者仍只能是 `:composeApp`**，拆模块不得破坏 OHOS 链接与 `MainArkUIViewController` 符号。旧「单 `:composeApp`」口径自 2026-10-16 架构决策起作废。
- **R5** 平台能力缺 → gap registry 登记 + blocker + follow_up；不得静默降级为完成。
- **R6** Flutter 侧为 mock 的能力允许 KMP 同等 mock（深度对齐）过逻辑验收；真 SDK 另列。
- **R7** `scripts/check-layer-deps.sh` 必须通过（层依赖门禁）。
- **R8** 视觉真相源 = Flutter 实机/截图与 token；误差 ≤2%（logic-first 阶段为可选回归，阶段结束后恢复硬门禁）。

## Acceptance

- **Logic Acceptance Packet（当前硬验收）**：Android/iOS/Harmony 主路径可开（含 Soft Auth）；主交互与 Flutter 同级；`commonTest` 或可复现脚本覆盖关键行为；gap registry 已更新。
- **Route Acceptance Packet** = Logic Packet +（阶段结束后）Android Screenshot Diff Gate。
- 每条路由完成 = inventory 状态推进到 `logic-pass`（或 `packet-pass`），并留证据 `notes/evidence/{module}/{Platform}/{route}.md`。

## 波次

| Wave | 范围 | 出口 |
|---|---|---|
| W0 | A 壳/鉴权/桥（三端） | 登录 resume 证据 + gap 登记 |
| W1 | B.1–B.3 + C + D.1–D.2 | 四 Tab 主路径无挡板 |
| W2 | B.10–B.15 + E | Home/Mine 二级可点 |
| W3 | F + G | 商城/钱包/会员 |
| W4 | H + I + J.1 | 内容模块 |
| W5 | W0–W4 的 iOS | 同出口 |
| W6 | W0–W4 的 Harmony | 同出口 |
| W7 | L drift + 真 SDK（IM/支付/推送/OHOS 相机/播放器/SSE）+ K 收尾 | 全量对齐 |

## Non-goals

- bfui 17 条演示路由（`n/a-out-of-scope`，除非产品书面纳入）。
- `/settings/{dialog_demo,linking_debug,realtime_debug,im_debug,bluetooth_demo}` 调试页（默认 n/a；发票两页待产品定 scope）。
- 不发明 Flutter 未用的后端 API；不保留 GetX/Dio/MethodChannel 字面 API。
