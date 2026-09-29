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
相对视觉真相源，布局/字号/色值/圆角/间距等误差 ≤ 2%（即还原度 ≥ 98%）；**全量对齐期**验收以自动化截图 diff（对照 Flutter SoT）为通过门禁，人工抽检为辅。
_Avoid_: 仅“感觉像”、无阈值的差不多就行、只有人工口感无 diff 门禁

**Phase-1 Main Path（一期主路径）**:
已完成的基线：闪屏→隐私→四 Tab 壳 + 各 Tab 根页（「我的」主页为原生）+ 「我的」二级及子页的 Compose 岛起步。**不**再作为「全量迁」的封顶范围。
_Avoid_: 用一期后置占位否决全量对齐期清单

**Full Parity Phase（全量对齐期）**:
在一期基线之上，把 Flutter `my_ai_project` 中全部产品页（按路由/入口）纳入迁移与验收清单；非「我的」页走 Native Shell UI，仅「我的」二级及子页走 Mine Compose Island。厂商真 SDK 另见 Platform Capability Gap。
_Avoid_: 一期后置即免做、用 SecondaryRouteIsland 冒充原生主路径长期共存

**Platform Capability Gap（平台能力缺口）**:
融云 IM、真支付、推送、微信登录、OHOS 相机/播放器、SSE 等需厂商 SDK 或尚未接线的能力。若 Flutter 侧同为 mock，则 KMP 同级 mock UI/行为可过页面对齐验收；真 SDK 未接入须登记在缺口表，不挡「页面同步」验收，但不得伪称 ready。
_Avoid_: 任意 gap 未 ready 就否定全部页面验收、用假 Success 冒充真支付/真 IM

**Parity Inventory（对齐清单）**:
以 Flutter 路由/入口表为唯一待办真相源；剔除产品已标 `n/a-out-of-scope` 与仅属 Platform Capability Gap 的真 SDK 项。KMP 缺页、或未过验收包 = 未完成。
_Avoid_: 只修 KMP 已有 Catalog、把 Flutter 有而 KMP 无的入口当范围外

**Screenshot Diff Gate（截图 Diff 门禁）**:
Android 同机/同分辨率对照 Flutter Android SoT，误差 ≤ UI Parity Bar（≤2%）。**Logic-first 阶段（进行中）**：降级为可选回归，不挡 `logic-pass`。阶段结束后再恢复为硬门禁。工具与历史证据保留。
_Avoid_: 逻辑阶段仍以 mse 挡合并、删掉 diff 工具、无 Android 证据却宣称像素验收通过

**Logic Acceptance Packet（逻辑验收包）**:
**当前硬验收**：Android / iOS / Harmony 均可打开主路径（含 Soft Auth）；主交互与 Flutter 同级（Shared Presentation Logic / mock 深度）；`commonTest` 或可复现脚本覆盖关键行为；platform-gap 表已更新。不要求 Screenshot Diff Gate。
_Avoid_: 只改 SoT 位图/像素皮、无行为证据却勾完成、假 Success 冒充真支付

**Route Acceptance Packet（路由验收包）**:
完整包 = Logic Acceptance Packet +（阶段结束后）Android Screenshot Diff Gate。Logic-first 阶段勾完成以 Logic Acceptance Packet 为准，inventory 记 `logic-pass`；历史 `packet-pass` 保留。
_Avoid_: 仅 Android diff 即合入、三端未通就勾完成、逻辑未对齐只改皮

**logic-pass（逻辑通过）**:
Parity Inventory 状态：本路由已过 Logic Acceptance Packet；不表示像素门禁已再跑。可与历史 `packet-pass` 并存。
_Avoid_: 用 logic-pass 冒充像素完成、把历史 packet-pass 清成未完成仅因暂缓 diff

**Native-First Cutover（原生优先切流）**:
非 Mine 页从 Legacy SecondaryRouteIsland 迁出时：先接通各端 Native Shell UI 为主路径并过验收包，再删除岛内对应路由；禁止先把岛内 CMP 像素修满再整体搬原生（避免双份 UI 劳动）。
_Avoid_: 先修岛再迁、长期双开且无删除点

**Parity Domain Ticket（对齐域票）**:
全量对齐期的实现单位：同一小域 2–4 个相关路由为一票。Logic-first 阶段票面验收 = 三端打开 + Logic Acceptance Packet + gap 表；**不**要求 Screenshot Diff Gate。Mine Compose Island 与 Native-First Cutover **两轨并行**，共用 Parity Inventory。
_Avoid_: 一路由一票过碎、整轨一张大票、岛与壳强行串行无阻塞边却互相等待、逻辑阶段仍以像素 diff 挡票

**Parity Inventory Record（对齐清单记录）**:
仓内 markdown 为清单草稿 SoT；开票时同步到 GitHub（epic/子票）。二者冲突时以已确认的 GitHub 子票范围为准并回写仓内表。
_Avoid_: 只活在聊天里、只开 GitHub 无仓内表、双表长期分叉不回写

**Shared Presentation Logic（共享表现逻辑）**:
跨端共用的 UseCase 与可观察 UiState/ViewModel（页面状态机）；各端（及 Compose 岛）只负责把同一份状态渲染为像素。不含像素布局本身，也不默认共享完整导航图。
_Avoid_: 仅 Repository 层共享、把路由表/导航图默认算进共享层

**Android UI Split（Android 双 Compose 分工）**:
壳与四 Tab 根页（含「我的」主页）用 Android Jetpack Compose（本端）；「我的」二级及子页用共享 Compose Multiplatform，与 iOS/Harmony 同一份岛。
_Avoid_: Android 全走共享 CMP、把 Jetpack 与 CMP 混称为同一种而不划分模块

**Auth UI（认证 UI）**:
登录、注册，以及聊天/社区等处的未登录门闸，均属 Native Shell UI，不进入「我的」Compose 岛。
_Avoid_: 把登录注册默认放进 Mine Compose Island

**Kotlin-Owned BFF JSON（Kotlin 独占 BFF JSON）**:
对 BFF 的请求体与响应（含标准 `{code,message,data}` 信封）一律在共享 Kotlin 中用 kotlinx-serialization 编解码；Native Shell UI 只负责像素与平台 SDK，不解析 BFF JSON。
_Avoid_: ArkTS/Swift 手解 BFF、壳内平行 AuthApiClient、把「Harmony 另搞一套 JSON」当成平台必然

**Envelope Fallback（信封兜底）**:
当响应不是标准业务信封（例如网关 HTTP 401 的 OAuth `error`/`error_description`）时，仅在 Kotlin 网络层做薄适配；不构成第二套业务字段解析。
_Avoid_: 强制一切响应都能 decode 成标准信封、在壳层再解析异常体

**Account Session SoT（会话真相源）**:
登录会话以共享 Kotlin 会话层为唯一权威；壳层只读或经 Bridge 写入，不维护平行权威 token 存储。
_Avoid_: ArkTS SessionStore 与 Kotlin 双权威、登录成功只写壳不写 Kotlin

**Mine Island Hosting（我的岛宿主导航）**:
从「我的」主页进入二级时，由原生壳 push 一个 Compose 容器页承载岛；岛内自管二级/三级跳转；系统返回先弹出岛内页，栈尽再关闭容器回到「我的」主页。
_Avoid_: 岛内自管与原生壳脱节的双栈并行、无容器边界的裸嵌

**Legacy Shared Compose（遗留共享 Compose）**:
含历史 commonMain 产品 Compose，以及 iOS/Harmony 上暂用 **SecondaryRouteIsland** 承载的非「我的」二级页。全量对齐期主路径仍是 Native Shell UI + Mine Compose Island；Legacy 仅作过渡，对齐并验收后删除，不得长期双主路径。
_Avoid_: 一期立刻清空所有非岛 Compose、把 SecondaryRouteIsland 当作非 Mine 正式主路径、长期双主路径并存且不设删除点

**Phase-1 Tab Roots（一期 Tab 根页深度）**:
首页 / 聊天 / 社区根页按 Flutter 做满并对齐还原门槛；从根页进入的非「我的岛」深链业务页可后置。
_Avoid_: 仅骨架占位作为一期验收、三 Tab 完成度故意不齐

**Shared Design Tokens（共享设计 Token）**:
色、字号、间距等数值来自一份共享源，供 Android Jetpack、SwiftUI、ArkTS 与 Compose 岛共同消费；不以各端私有色板为真相。
_Avoid_: 各端对照 DESIGN 各自抄写、仅岛内共享 token

**Deferred Destination Stub（后置目标占位）**:
根页上存在、但目标 feature 不在一期的入口：视觉按 Flutter 保留；点击走原生占位（或安全 no-op），不进入「我的」Compose 岛，也不在一期实现完整目标页。
_Avoid_: 一期隐藏入口导致根页对不齐、一期强行做完全部深页
