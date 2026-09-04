## Why

Grill 共识：本仓库目标从「demo/对等车」升级为**可上架生产底座**（三端含 OHOS 一等公民、团队可并行）。现状 monomodule + Home 上帝路由 + Facade 双轨会话无法支撑。对齐 tpj-flt 五层依赖语义，用 KMP 技术栈落地。

## What Changes

- 五层：`app` / `feature` / `component` / `core` / platform shells；禁止反向依赖
- 多模块（Android/iOS）+ OHOS fat 聚合 `libkn.so`
- 类型安全导航（短期 sealed Navigator；OHOS Nav 制品就绪后可迁官方）
- `AppContainer`（分子组件）→ 再迁 Koin；`AuthSessionState` Flow 为登录态唯一源
- Must 门禁写死（含沙箱真支付、推送 Token+点击深链）
- 尖刀 I（2 人×2 周赌局）/ 尖刀 II（支付·推送·聚合收口）

## Capabilities

- `modularity`: 五层与依赖规则、模块空壳、OHOS 聚合策略
- `app-shell`: 单一导航图、底栏与深链
- `user-auth`: AuthSessionState 唯一源
- `platform-bridges`: component 层 Push/WebView/Pay/IM port

## Impact

- Android / iOS / OHOS：结构与门禁三端同等；OHOS 缺 Must = Fail
- 对等 UI 冻结至尖刀 II 结束（之后必须走新边界）

## Non-goals

- 本 change 不追求 Flutter 全量像素 1A
- 不在尖刀 I 内完成生产商户号支付（沙箱真 SDK 即可）
- 不引入 GetX；不复刻 FanGroup 品牌业务包
