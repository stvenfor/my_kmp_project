## Why

上一轮 `migrate-my-ai-to-kmp` 已落地四 Tab 壳与大量 **stub/占位**；相对 Flutter 源仓 `my_ai_project`，业务逻辑与 UI 差距极大。现需按 **业务完全一致 + UI 像素级还原（1A）**、**三端真实 SDK（2A）** 收口；任一平台缺能力视为该能力 **未完成**，但必须登记以便后续补齐。

## What Changes

- 以 Flutter 为唯一产品真相源：路由/状态机/接口契约/页面结构与交互对齐；KMP 实现可替换技术栈，不可降级产品行为。
- **UI 100%（像素级）**：色值、字号、间距、圆角、资源与布局对照 Flutter 截图验收；三端同屏同路径。
- 将 stub（IM 发送、社区发布、媒体播放、支付、WebView/Bridge、扫码、深链投递、推送、远程登录等）升级为真实实现；**A/iOS/OHOS 均须可用**，否则记入缺口清单且验收不通过。
- 建立 **对等验收矩阵** 与 **平台缺口登记表**（能力 × 三端状态 + 证据路径）。
- 对齐设计令牌至 Flutter `AppTheme`（含 `#007AFF` 等），替换当前 Demo 色板偏差。
- **BREAKING（产品面）**：占位文案/假数据/模拟按钮不得再充当「已迁移」完成态。

## Non-goals

- 不回迁 FanGroup / tfent / T-Family / paopao。
- 不强制移植 DoKit、`module_bfui` 纯模板展示（除非产品书面纳入）。
- 不保留 GetX/Dio/MethodChannel 字面 API。
- Flutter 侧本就为 mock 的能力（如当前 RongCloud 包装）允许 KMP 同等 mock，但 UI/流程须像素与行为对等；若后续上真实 SDK，三端同步，并更新缺口表。

## Capabilities

### New Capabilities

- `parity-acceptance`：像素/业务验收流程、黄金路径、完成定义与未完成判定
- `platform-gap-registry`：能力 × Android/iOS/OHOS 缺口登记与后续补齐规则
- `design-tokens-parity`：与 Flutter `AppTheme` 对齐的色板/字阶/通用控件视觉契约
- `feature-friend`：好友列表与入口对等
- `feature-live`：直播列表/房间入口对等
- `feature-classroom`：课堂多页 POC 对等

### Modified Capabilities

- `app-shell`：隐私持久化、壳层像素对等、禁止 stub 冒充完成
- `user-auth`：远程登录/OTP/注册与会话同步；三端真实鉴权链路
- `feature-home`：首页及二级（服务/搜索/报告/攻略/音视频/网页等）全量对等
- `feature-chat`：会话列表/详情/发送与 Flutter 行为对等（含引擎可插拔）
- `feature-community`：Feed/发布/预览真实链路
- `feature-settings`：我的/设置产品面（非 debug-only）对等
- `feature-media`：真实音视频播放适配器替换 stub
- `feature-commerce`：微信/支付宝真实通道三端可用
- `platform-bridges`：WebView+Bridge、深链投递、扫码、推送入口三端真实
- `modularity`：对等实施中的模块边界与平台适配放置规则

## Impact

- **Android / iOS / OHOS**：共享 UI/领域重写与补齐；引入支付、WebView、扫码、推送、播放器等原生依赖；OHOS 缺 SDK 时能力标 **未完成** 并列入 `platform-gap-registry`，不得静默降级为「完成」。
- **代码**：`composeApp` feature/core、三端 actual、`iosApp`/`harmonyApp` 薄壳配置、catalog 依赖。
- **参照**：`/Users/mac/Desktop/github/my_ai_project`；基线归档 `openspec/changes/archive/2026-09-02-migrate-my-ai-to-kmp/`。
