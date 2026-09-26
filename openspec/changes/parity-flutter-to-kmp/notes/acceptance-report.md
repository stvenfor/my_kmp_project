# Acceptance report

Status: **ADR 0002 原生二级全量接入** — 2026-09-26.

## 所有权（强制）

| 层级 | iOS | Harmony | Compose |
|---|---|---|---|
| 壳 / Tab 根 | SwiftUI | ArkTS | 禁止 |
| 产品二级（Home/Mine/内容…） | SwiftUI `NativeFeatureHost` | ArkTS `NativeFeaturePane` | 禁止 |
| Mine 设置/个性化/关于/会员 | — | — | **仅 island** |

## 路由表

- iOS: `NativeFeatureCatalog.swift` + `NativeRouteResolver`
- OHOS: `NativeFeatureCatalog.ets` + `resolveNativePath`
- 覆盖：二手车/生活服务/新车/数据/直播/Club/策略/学习报告/签到商城/配音/热榜/台账/待办/售后/商城/订单/钱包/短视频/直播/好友/AI/课堂/音乐/社区发布搜索/扫一扫/网页/购车计算器/短信模板/收款码/问答/海报/商务/提醒/反馈等

无「一期后置 stub」产品入口；未知路径仍走原生列表壳（非 Compose）。

## Sign-off

| role | date | ADR 0002 | 二级原生 | 厂商 SDK |
|---|---|---|---|---|
| engineering | 2026-09-26 | Pass | Pass（mock UI） | Fail — registry |
