## Why

将 Flutter 工程 `my_ai_project`（`module_sample`）完整迁移到本仓库的 KMP + Compose Multiplatform，统一 Android / iOS / HarmonyOS 三端体验与模块边界，替代当前仅 Home+Mine 的 Demo 壳，建立可扩展的多模块产品基线。

## What Changes

- **BREAKING**：以四 Tab 主壳（首页 / 聊天 / 社区 / 我的）替换当前 `DemoApp` 双 Tab Demo；路由与沉浸式壳按源工程能力对齐并三端一致。
- 引入 **Gradle 模块化**：`core-*`（network / account / design / router / platform）与 `feature-*`（auth / home / chat / community / settings / …）边界清晰，`composeApp` 仅作组装与平台入口。
- 迁移源工程能力（分阶段交付，最终覆盖完整产品面）：auth、home、chat（含 IM 抽象）、community、settings、friend、live、pay、video、classroom、music，以及 WebView/桥、推送/深链、支付等平台能力。
- 三端一致性：同一 `commonMain` UI/领域逻辑；平台差异仅经 `expect`/`actual` 或薄壳（`iosApp` / `harmonyApp`）；验收以三端同路径行为为准。
- 保留并扩展现有 `NetworkFacade` / `AccountFacade` / 沉浸式设计基线；OHOS 继续走非 Ktor HTTP 源集策略。

## Non-goals

- 不回迁 FanGroup / tfent / T-Family / paopao 等已清理业务与品牌资源。
- 不在本 change 内实现 Flutter 调试套件（DoKit）与纯展示模板包（`module_bfui`）的对等移植——可作为后续可选 change。
- 不强制保留 GetX / Dio / MethodChannel 字面 API；契约与行为对齐，实现可重写。
- 不把真实 RongCloud / 腾讯人脸等 SDK 绑死为第一阶段硬依赖（先以接口 + mock/可插拔适配器落地）。

## Capabilities

### New Capabilities

- `app-shell`: 启动、隐私同意、主壳四 Tab、沉浸式与三端一致导航
- `modularity`: Gradle/feature 模块边界、依赖规则、组装方式
- `user-auth`: 登录/注册/会话/强制登出与 Tab 鉴权门闸
- `feature-home`: 首页及核心二级页（学习报告、服务入口、搜索等）
- `feature-chat`: 聊天列表/详情与 IM 引擎抽象（含 mock）
- `feature-community`: 社区动态、发布、媒体预览
- `feature-settings`: 我的/设置及环境与能力演示入口的产品面
- `feature-media`: 音视频播放与短视频/音乐相关能力的跨端抽象
- `feature-commerce`: 支付/会员相关能力的跨端抽象与平台 SDK 适配
- `platform-bridges`: WebView/JS Bridge、深链/推送、扫码等平台桥的统一契约

### Modified Capabilities

- （无：`openspec/specs/` 当前为空，本 change 全部为新能力）

## Impact

- **Android / iOS / OHOS**：共享 UI 与领域层；原生壳与 cinterop/NAPI、支付/推送/WebView 需分端适配；OHOS 网络仍隔离于 `ohosMain`。
- **代码**：`composeApp` 拆模块、扩展路由与 Tab；可能新增多 Gradle 子模块与 catalog 依赖。
- **源工程参照**：`/Users/mac/Desktop/github/my_ai_project`（`lib/`、`features/`、`commons/`、`components/`）。
- **依赖**：Ktor/Settings（A/iOS）、OHOS HTTP、导航、图片、媒体、支付 SDK 等按阶段引入。
