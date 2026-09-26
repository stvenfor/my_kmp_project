# Acceptance report

Status: **ADR 0002 所有权纠偏进行中** — 2026-09-26.

> **硬规则（ADR 0002）**
> - iOS 产品壳 / Tab 根 / 一期后置入口 = **SwiftUI**
> - Harmony 产品壳 / Tab 根 / 一期后置入口 = **ArkTS**
> - Compose 岛 **仅** Mine 二级（settings / personalized / about / membership）
> - 禁止把 Home/Chat/Community/Web/Friend 等塞进 `SecondaryRouteIsland` 冒充三端齐

## 当前产品面

| 面 | Android | iOS | OHOS |
|---|---|---|---|
| Splash / Privacy / Tab shell | Jetpack | SwiftUI | ArkTS |
| Home / Chat / Community / Mine **根** | Jetpack | SwiftUI | ArkTS |
| Chat 详情发送 | Jetpack | SwiftUI | ArkTS |
| Mine 设置岛 | CMP island | CMP island | CMP island |
| Home 二级 / 商城 / 视频等 | Jetpack 已实现 | **SwiftUI 一期 stub** | **ArkTS 一期 stub** |
| 厂商 SDK | stub/missing | stub/missing | stub/missing |

## 纠偏相对上一轮

上一轮用 Compose `SecondaryRouteIsland` 在 iOS/OHOS 打开二级路径 —— **违反 ADR 0002**。已改回：

- iOS：`onDeferred` → `NativeDeferredStubView`（SwiftUI）；Tab 深链切页；Mine 仍 `MineIslandViewController`
- OHOS：`openDeferred` → ArkTS stub；不再 `SetOhosSecondaryRoute` 拉全量 Compose；Mine 仍 `openMineIsland`

## Sign-off

| role | date | ADR 0002 所有权 | 二级页原生补齐 | 厂商 SDK |
|---|---|---|---|---|
| engineering | 2026-09-26 | Pass（壳/根） | In progress（stub→原生） | Fail — registry |
| product |  |  |  | 待签 |
