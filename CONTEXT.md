# My KMP Product UI Ownership

三端产品表现层与共享逻辑的边界语境。记录「谁画像素、谁共享逻辑」，不记录具体框架版本或文件路径。

## Language

**Native Shell UI（原生壳层 UI）**:
各端自行实现的启动、合规门闸、主导航/Tab 容器，以及非「我的 Compose 岛」的全部产品页面（含「我的」主页）。
_Avoid_: 共享 Compose 壳、commonMain 产品壳

**Mine Root（我的主页）**:
「我的」Tab 的根页面；归属 Native Shell UI，不用共享 Compose。
_Avoid_: 我的模块整包、Mine 岛

**Mine Compose Island（我的 Compose 岛）**:
仅「我的」模块下的二级页面及其子页面；三端共用一份 Compose UI。
_Avoid_: 全 App Compose、我的主页、其它 Tab 的页面

**Shared Business Logic（共享业务逻辑）**:
跨端共用的领域规则与数据通道（会话、接口、用例等），不负责像素级绘制。
_Avoid_: 共享 UI、把页面状态机默认算进 UI

**Visual Source of Truth（视觉真相源）**:
Flutter 源仓 `my_ai_project` 的实机/截图与其设计 token 文档；现有 KMP 界面仅作参考，冲突时以 Flutter 为准并改掉 KMP。
_Avoid_: 以当前 KMP 为真、Flutter 与 KMP 双 SoT

**UI Parity Bar（UI 还原门槛）**:
相对视觉真相源，布局/字号/色值/圆角/间距等误差 ≤ 2%（即还原度 ≥ 98%）；以对照截图与 token 的人工（或辅助）验收为准，不要求先上自动化截图 diff 才算过。
_Avoid_: 还原度不低于 2%、仅“感觉像”、无阈值的差不多就行

**Phase-1 Main Path（一期主路径）**:
第一期必交：闪屏→隐私→四 Tab 壳 + 各 Tab 根页（「我的」主页为原生）+ 「我的」二级及子页的 Compose 岛按 Flutter 对齐；直播/课堂/支付等其余 feature 不进一期，后续分票。
_Avoid_: 一期全量三端、先单端样板再铺开（已否决为默认一期策略）

**Shared Presentation Logic（共享表现逻辑）**:
跨端共用的 UseCase 与可观察 UiState/ViewModel（页面状态机）；各端（及 Compose 岛）只负责把同一份状态渲染为像素。不含像素布局本身，也不默认共享完整导航图。
_Avoid_: 仅 Repository 层共享、把路由表/导航图默认算进共享层

**Android UI Split（Android 双 Compose 分工）**:
壳与四 Tab 根页（含「我的」主页）用 Android Jetpack Compose（本端）；「我的」二级及子页用共享 Compose Multiplatform，与 iOS/Harmony 同一份岛。
_Avoid_: Android 全走共享 CMP、把 Jetpack 与 CMP 混称为同一种而不划分模块

**Auth UI（认证 UI）**:
登录、注册，以及聊天/社区等处的未登录门闸，均属 Native Shell UI，不进入「我的」Compose 岛。
_Avoid_: 把登录注册默认放进 Mine Compose Island

**Mine Island Hosting（我的岛宿主导航）**:
从「我的」主页进入二级时，由原生壳 push 一个 Compose 容器页承载岛；岛内自管二级/三级跳转；系统返回先弹出岛内页，栈尽再关闭容器回到「我的」主页。
_Avoid_: 岛内自管与原生壳脱节的双栈并行、无容器边界的裸嵌

**Legacy Shared Compose（遗留共享 Compose）**:
一期允许暂时保留现有 commonMain 产品 Compose，作对照并行；主路径以原生 +「我的」岛为准并对齐 Flutter；对齐完成后再删除遗留共享 UI。
_Avoid_: 一期立刻清空所有非岛 Compose、长期双主路径并存且不设删除点

**Phase-1 Tab Roots（一期 Tab 根页深度）**:
首页 / 聊天 / 社区根页按 Flutter 做满并对齐还原门槛；从根页进入的非「我的岛」深链业务页可后置。
_Avoid_: 仅骨架占位作为一期验收、三 Tab 完成度故意不齐

**Shared Design Tokens（共享设计 Token）**:
色、字号、间距等数值来自一份共享源，供 Android Jetpack、SwiftUI、ArkTS 与 Compose 岛共同消费；不以各端私有色板为真相。
_Avoid_: 各端对照 DESIGN 各自抄写、仅岛内共享 token

**Deferred Destination Stub（后置目标占位）**:
根页上存在、但目标 feature 不在一期的入口：视觉按 Flutter 保留；点击走原生占位（或安全 no-op），不进入「我的」Compose 岛，也不在一期实现完整目标页。
_Avoid_: 一期隐藏入口导致根页对不齐、一期强行做完全部深页
