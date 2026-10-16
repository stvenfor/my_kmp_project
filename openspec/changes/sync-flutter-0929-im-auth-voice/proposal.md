# Proposal: 同步 my_ai_project 2026-09-24 → 09-29 增量（IM 真连 / 语音 / 鉴权 401-403 / LAN 分流）

## Why

Flutter 源仓 `my_ai_project`（SoT）自上次同步点（KMP `b4d6501`，对应 Flutter ~`0a0f6a9` 之前的能力面）之后合入 5 个业务提交，KMP 尚未对齐：

| Flutter commit | 内容 | KMP 现状 |
|---|---|---|
| `b70315c` | 真连融云 IMLib、好友页与业务备份客户端 | 全 mock（`MockImEngine`），无 IM HTTP 契约 |
| `90c7b46` | SDK 会话收发、门店群补拉与自由群 UI | 无群 API、无补拉、无 SDK 分支 |
| `0fbfe18` | Chat UI 打磨 + 语音录放自检 + 相机权限门闸 | 输入面板纯文本；无语音录放；无权限门闸 |
| `10654a9` | 后端地址按设备分流，机器 IP 不再入库 | `LanHost.fallback = "172.16.0.34"` 写死入库（正是该提交要修的问题） |
| `d7876a1` | 统一 401/403 会话决策与 wys_network forbidden | `mapAuthFailure` 自行猜 code；无统一决策枚举 |

按 `parity-flutter-to-kmp` 规则：Flutter 侧已上**真 SDK** 的能力（融云），KMP 未接 SDK 期间须以「mock 同级 + 契约对齐 + 缺口登记」推进，不得伪称 ready。

## What Changes

- **鉴权决策核心（纯共享逻辑）**：移植 `AuthBizCode` / `AuthFailure` / `AuthHttpDecision.classifyAuthHttp` / `isAuthHttpHardFailure` 到 commonMain，接入 `mapAuthFailure` 与网络层错误分类；单测对齐 Flutter `forbidden_status_test.dart`。
- **LAN 分流**：`LanHost` 机器 IP 出库（fallback 置空），注入走 `NetworkConfig.baseUrlOverride` / 环境自定义地址；真机无注入时告警不阻断。
- **IM 引擎契约扩展**：`ImEngine` 增 `sendVoice(durationSeconds)`、`deleteMessage`、语音时长字段；消息预览/气泡对齐 `[语音] n"`。
- **IM HTTP 契约（BFF）**：`ImFriendApi` / `ImGroupApi` / `ImBackupApi` 端点契约（搜索/好友/申请/同意/建群/门店群同步/自由群/备份上下行）落 commonMain，走 `NetworkFacade`；离线/未注册时回退 mock 数据（对齐 Flutter「旧后端无接口不阻断」容错）。
- **语音管线**：`VoiceRecorder` / `VoicePlayer` / `VoicePipelineSelfCheck` 逻辑层（状态机、阈值：minVoiceMs=600、60s 上限、取消判定、文件校验 ≥100B）+ expect/actual 平台适配（Android 真实现，iOS/OHOS stub → 缺口登记）；按住说话 HUD + 语音气泡（88+3.2·s dp 宽、3 条波纹动画）。
- **聊天 UI**：会话空态 `ConversationListEmpty`（连接态提示 / 去通讯录 / 下拉也可刷新）、输入面板四模式（文本/语音/表情/更多）、更多面板（相册/拍摄/语音自检）。
- **权限门闸**：`CameraPermissionGate` 等价物（`PermissionGate` 扩展：拒绝 → 引导系统设置文案对齐）。

## Non-goals

- 不在本变更内接融云真 SDK（Android/iOS/OHOS 三端 SDK 接入单独立项，先登记 `platform-gap-registry`）。
- 不迁移 wys_network / GetX / Dio 拦截器字面 API；以 core/network 等价语义实现。
- 不做 Realtime（wys_realtime）与 JPush 增量（已有 push 桥接，行为面无变化）。
- 不回迁 FanGroup / tfent 等历史模块。

## Impact

- **Android**：语音录制/播放走真实 `MediaRecorder`/`MediaPlayer`；权限门闸生效；LAN 注入生效。
- **iOS / OHOS**：语音平台适配 stub（缺口登记，不挡逻辑验收）；鉴权/LAN/IM 契约/聊天 UI 共享层三端同步生效。
- **代码**：`composeApp/src/commonMain`（auth/chat/friend + core 平台门闸）、`core/network`（LanHost）、`androidMain`/`iosMain`/`ohosMain` actual；`platform-gap-registry.md` / `parity-inventory.md` 更新。
- **验证**：`./gradlew :composeApp:compileDebugKotlinAndroid` + `:composeApp:testDebugUnitTest`（新增单测覆盖决策矩阵、语音阈值、LAN 分流、好友 API 容错回退）。
