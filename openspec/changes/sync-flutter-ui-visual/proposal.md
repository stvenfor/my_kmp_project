## Why

当前 KMP UI 与 Flutter SoT（`/Users/mac/Desktop/github/my_ai_project`）视觉差距极大：不是单点 bug，而是**资源未全量同步 + 布局用文字/色块近似替代产品组件**。例如 `composeResources/drawable` 仅约 7 个（闪屏/底栏），而 Flutter `features/home/assets`  alone ~78、`pay` ~31；社区等页仍是 Material 卡片骨架，而非 Flutter 信息流视觉。现有 `parity-flutter-to-kmp` 已推进能力接线，但 1A 像素层未真正落地，需单独收口「视觉全量对齐」。

## What Changes

- 建立 Flutter→KMP **资源清单与同步流水线**（图标/插画/九宫格/Tab/会员素材等），落入 `composeResources`（及必要的 Android/iOS/OHOS 壳资源）。
- 按 Flutter 模块 **逐表面重画** 共享 Compose UI：壳/首页/我的/聊天/社区/设置/会员/媒体入口及已有二级页；禁止用首字母色块、纯文字行冒充图标与卡片。
- 对齐 **设计令牌**（`AppTheme` 色板、字号阶梯、圆角、间距、分割线）与沉浸式 chrome，沿用现有 `DemoColors` / Immersive* 约束并校正漂移。
- 用既有 pixel SOP 做 **Flutter vs KMP 并排证据**；未对齐表面不得标 Pass。
- 与 `parity-flutter-to-kmp` **分工**：本 change 只做 1A 视觉/资源；2A SDK、支付真机、推送等仍归 parity / registry。

## Capabilities

### New Capabilities

- `ui-visual-parity`: Flutter 资源同步契约、逐表面像素对等、视觉验收证据与「禁止骨架冒充产品 UI」规则

### Modified Capabilities

- `app-shell`: 壳/Tab/闪屏/隐私对话框视觉与资源必须对等 Flutter chrome
- `feature-home`: 首页与二级（服务/搜索/报告/策略等）布局与资源对等
- `feature-settings`: 我的根页 + 设置/个性化视觉对等（含菜单图标资源）
- `feature-chat`: 会话列表/详情视觉对等（不含强制换 IM SDK）
- `feature-community`: 信息流/发布/预览视觉对等（含媒体占位资源规则）
- `feature-commerce`: 会员/支付页视觉与 Flutter `module_pay` 资源对等（仍不伪造支付成功）
- `feature-media`: 音视频入口/播放壳视觉对等

## Impact

- **代码**：`composeApp/.../composeResources/**`、`core/design`、各 `feature/*` Compose 屏；可能调整 `harmonyApp`/`iosApp`/`android` 启动图标与闪屏若 SoT 变更。
- **Android / iOS / OHOS**：共享 Compose 视觉一次落地三端；壳层资源各自核对。
- **依赖**：原则上不新增业务 SDK；仅资源与 UI。
- **并存**：`parity-flutter-to-kmp` 继续管 2A/缺口清单；本 change 完成后更新其 matrix 中视觉相关 Partial 行。

## Non-goals

- 不接入微信/支付宝/推送/关系链等真实 SDK（仍属 2A）。
- 不恢复 FanGroup / tfent / T-Family / paopao 等品牌或业务域。
- 不把 DoKit / bfui / BLE / invoice demo 纳入交付（保持 n/a-out-of-scope）。
- 不要求本 change 内完成三端设备全量签字；但 Android 至少完成主表面并排证据，iOS/OHOS 列入任务并允许 registry Partial。
