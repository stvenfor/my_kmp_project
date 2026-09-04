## Context

参考：`/Users/mac/Desktop/work/tpj-flt/docs/architecture/tpj-flt-architecture.excalidraw`（五层 + 禁止反向依赖）。  
本仓库图：`docs/architecture/my-kmp-architecture.excalidraw`。  
ADR：`docs/adr/0001-production-architecture.md`。

Grill 锁定摘要见 ADR；此处只记实现决策。

## Goals / Non-Goals

**Goals:** 尖刀 I Done = 五层包边界可强制 + Auth Flow SoT + sealed 导航迁出 Home hub + AppContainer 子组件骨架 + Components 接口到位（Push/WebView/Pay/IM port）+ 依赖检查；双人并行不堵文件。  
**Non-Goals:** 尖刀 I 不要求支付/推送真机全绿（属尖刀 II）；不拆完所有 Gradle feature 模块。

## Decisions

### D1 — 五层映射（语义对齐 tpj-flt）
| tpj-flt | KMP |
|---------|-----|
| App Shell | `app/` |
| Feature | `feature/*` |
| Components | `component/*`（Push/WebView/Pay/IM） |
| Commons | `core/*` |
| Platform | android/ios/ohos + iosApp/harmonyApp |

### D2 — 模块策略
空壳 + 规则先；再抽 `:core:network` / `:core:account`；OHOS 用聚合模块链 `libkn.so`。

### D3 — 导航
短期 `sealed AppRoute` + `Navigator`；验证 OHOS Navigation 制品后再评估官方库。

### D4 — DI / 会话
`AppContainer` 分子组件，尽快迁 Koin；`AuthSessionState`（Flow）唯一登录态；AccountFacade→仓储。

### D5 — 双人拆分（尖刀 I）
- 人1：导航 + Auth Flow + AppContainer
- 人2：模块/依赖检查 + Components 搬迁 + OHOS 聚合脚本

### D6 — Must（Q12=D）
Shell/Auth/深链/主 Tab/soft-auth/WebView/扫码/真媒体/沙箱真支付/推送 Token+点击深链。缺则 Fail。

## Risks

- 尖刀 I 满载（Q25=A）依赖 ≥2 人；无人则降级里程碑制
- HomeScreen 迁路由时回归风险高 → 先骨架后逐页迁
- OHOS 聚合脚本可能阻塞 → 人2 优先打通 compile 路径

## Migration

1. ADR + 图 + OpenSpec（本 change）
2. 尖刀 I 按 tasks
3. 尖刀 II：Pay 沙箱闭环、Push B 口径、聚合发布
4. 解冻对等，且仅允许新边界内改
