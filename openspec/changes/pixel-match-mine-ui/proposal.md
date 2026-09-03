## Why

用户附 Flutter「我的」真机截图作为 SoT；当前 KMP Mine 根页在布局密度、卡片阴影、彩色服务图标、个人功能双列卡片与顶栏图标上仍与 Flutter 差极大（`sync-flutter-ui-visual` 只做了资源粗同步与部分 chrome，未按本截图像素收口）。说明：文案写「首页」，附件实为底部 Tab「我的」——本 change **以截图为准对齐 Mine**；Home Tab 另开 change。

## What Changes

- 以 `notes/evidence/flutter/mine_root.png` 为 1A 金标，重画 KMP `Mine` 根页：大标题「我的」+ 右侧四图标、资料卡、四列统计、常用服务四宫格（含 HOT）、个人功能双列卡片（含计算器数值）。
- 对齐 Flutter `MineTheme`（色/圆角/阴影/字号）与 `module_settings` mine widgets 结构。
- 补齐/引用 Mine 服务与功能图标（Material/本地 drawable；OHOS 禁用扩展 icons 包时用已有 PNG 或自定义矢量）。
- Android 先产出 KMP vs Flutter 并排证据；iOS/OHOS 列入任务可 Partial。

## Capabilities

### New Capabilities

- `mine-visual-parity`: Mine 根页相对 Flutter 截图的像素/结构验收契约

### Modified Capabilities

- `feature-settings`: Mine 根页视觉与交互 chrome MUST 对等 Flutter Mine（设置二级页不在本截图范围则可增量）

## Impact

- **代码**：`feature/mine/*`（尤其 `MineHomeContent`）、`DemoColors`/Mine 局部 tokens、必要 drawable。
- **Android / iOS / OHOS**：共享 Compose 一次改三端；底栏已选中样式随截图核对。
- **并存**：不替代 `parity-flutter-to-kmp` 的 2A；不重开 Home 仪表盘像素（除非用户后续指定）。

## Non-goals

- 不改 Chat/Community/Home 根页像素（Home 另议）。
- 不接入真实电子名片/短信/计算器 SDK；可 toast/占位。
- 不恢复 FanGroup 等品牌；不交付 bfui/DoKit。
- 不要求本 change 内完成 Flutter 全量侧截图包以外的三端签字。
