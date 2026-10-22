# my_ai_project → my_kmp_project 全量同步 · 超细粒度任务清单

> SoT：`my_ai_project` @ `d7876a1` · 路由权威：`commons/wys_router/lib/src/route/route_path.dart`（103 条，清单见 `notes/route-inventory.md`）
> 完成定义：**可点路径 + 数据契约 + 三端**（logic-first 阶段默认可先 Android）
> 状态：`[ ]` 未做 · `[~]` 部分 · `[x]` 可验收 · `[n/a]` 明确不做
> ID 规则：`{模块}.{路由简写}.{序号}` · 平台后缀 `.a` Android / `.i` iOS / `.h` Harmony（未写 = 先 Android）
> 粒度：每项 ≤ 0.5–1 人日（更大再拆）
> 前序基线：`parity-flutter-to-kmp`（490 项：373 done / 26 partial / 90 todo / 5 n/a）；本清单承接其余量 + Flutter drift

## 架构硬规则（每一项任务的前置）

- **R1** 非「我的」页 → 原生壳：Android Compose 壳 / iOS SwiftUI / OHOS ArkTS + `NativeFeatureCatalog`
- **R2** 「我的」二级及子页 → 三端共享 Compose 岛（`MineIsland` / `SecondaryRouteIsland`）
- **R3** 共享业务逻辑 → `commonMain` + expect/actual；页面状态机不默认算进 UI
- **R4** 单 `:composeApp` + `libkn.so`；不为拆模块破坏 OHOS 链接
- **R5** 平台能力缺 → `platform-gap-registry` 登记（capability × 三端 + blocker + follow_up），不伪称 ready
- **R6** Flutter 为 mock 的能力允许 KMP 同等 mock 过逻辑验收；真 SDK 另列
- **R7** `scripts/check-layer-deps.sh` 必须通过
- **R8** 误差 ≤2%（logic-first 为可选回归；阶段结束后恢复 Android 截图 diff 硬门禁）

---

# A. 壳 / 鉴权 / 桥接

## A.0 Splash `/`
- [x] A.0.1.a 闪屏图/Logo + 定时进下一步
- [ ] A.0.1.i / A.0.1.h 同上
- [x] A.0.2 资源与 Flutter 一致（非随机图）
- [~] A.0.3 三端冷启动截图证据

## A.1 Main `/main`
- [x] A.1.1.a 四 Tab 容器（首页/聊天/社区/我的）
- [ ] A.1.1.i / A.1.1.h
- [x] A.1.2 底栏 49dp、选中 pill、图标字重对齐
- [x] A.1.3 Tab 切换不销毁根状态
- [x] A.1.4 soft-auth：未登录进需登录 Tab → 登录 → resume

## A.2 Privacy（壳内）
- [x] A.2.1.a 首次同意弹层
- [x] A.2.2 同意持久化，二次启动跳过
- [x] A.2.3 不同意保持阻断（文案对齐）
- [ ] A.2.1.i / A.2.1.h
- [~] A.2.4 OHOS 持久化落沙箱（当前 `/tmp`，重启可能丢；gap registry 已登记）

## A.3 Auth 路由

### `/login` · `/login/password` · `/login/otp` · `/register`
- [x] A.3.1–12.a 四页 UI + 校验文案 + 成功写会话 + redirectRoute/pendingTab
- [ ] A.3.1–12.i / A.3.1–12.h 同上（iOS 演示登录 / OHOS 华为一键登录）
- [x] A.3.13 401 清会话（Flutter 已统一 401/403 决策 → 见 L.1 drift）
- [x] A.3.14 Token 刷新策略：对齐 Flutter `AuthTokenRefreshInterceptor`（gap: missing）
- [ ] A.3.15 微信登录三端 `done|missing` 登记（gap: missing）
- [x] A.3.16 验收：guest→login→resume Chat（Android 证据已有）

### Auth 数据契约
- [x] A.3.17 登录/注册/OTP 请求响应模型与 Flutter `UserAuthApi` 对齐（envelope/错误码）
- [x] A.3.18 403 Forbidden 语义统一（Flutter `d7876a1`：wys_network 403 处理 → 见 L.1）

## A.4 Bridges

### `/web`
- [x] A.4.1.a WebView 打开/返回 + 离线 fixture 证据
- [x] A.4.2 JS Bridge 核心 8 方法对齐 `WebBridgeRegistry`
- [x] A.4.3 Home WebHandlers 注册
- [ ] A.4.1.i WKWebView 真承载 + 5.5 验收
- [ ] A.4.1.h OHOS 真 Web 组件（当前 placeholder；gap registry）

### Scan / Deeplink / Push
- [x] A.4.4–6.a 扫码权限 UX / 回调路由 / Deeplink 冷启动
- [~] A.4.7 Push 注册 + payload→route（StubPushBridge；厂商 SDK missing）
- [ ] A.4.4–7 三端展开（OHOS want.uri 已 ready，回归保持）
- [ ] A.4.8 Push 真 SDK（JPush/FCM/OHOS Push）—— 见 K 缺口

---

# B. Home 模块（28 条路由）

## B.1 根页 `/home` — `HomePage`
- [x] B.1.1–12.a UI 区块（问候/消息 pill/搜索/扫码/Banner/FeatureGrid/TodoStrip/门店指标/策略入口/ServiceGrid/ContactList/NewsList）
- [x] B.1.13–16 数据契约（banner/features/todo/store metrics/news）
- [x] B.1.17–29 交互与导航（格子→二级、todo→四子页、指标→明细）
- [ ] B.1.30.i / B.1.30.h 根页同能力（iOS/OHOS NativeFeature 根 + Compose 岛混排）
- [ ] B.1.31 音乐 mini-player 底 inset（随 J.3）
- [~] B.1.32 像素精修 vs Flutter（Banner 圆角/间距/字阶）

## B.2–B.8 二级页（all_services / search / strategy / learning_report / check_in_mall / transliter_feed / hot_rank_detail）
- [x] B.2.1–B.8.3.a 每页 UI + 数据 + 导航
- [ ] B.2–B.8 `.i` / `.h` 展开
- [~] B.3.x 搜索历史/热词/空态与 Flutter 一致
- [~] B.7.x transliter_feed 与 H.6 跟读列表同源数据契约定稿

## B.9 容器页（life_service / live_commerce / club）
- [x] B.9.1–3.a 容器 + tab 内容切换
- [ ] B.9.1–3.i / B.9.1–3.h

## B.10–B.12 二手车 / 台账 / 数据分析
- [x] B.10.1–7.a 列表/详情/创建（表单+图片占位+提交）
- [x] B.11.1–3.a 台账列表/详情
- [x] B.12.1–3.a 数据分析列表（转化环+迷你柱）/详情（漏斗+对比柱+雷达 0–100）
- [ ] B.10–B.12 `.i` / `.h`
- [~] B.12.4 图表用共享图表组件（Flutter `wys_chart` 独立包模式 → KMP `feature/content` 图表收敛，不散落）

## B.13 Todo 四子页
- [x] B.13.1–4.a 四页列表 + 筛选项 + 空态
- [x] B.13.5 接线：Home todo strip → 四路由
- [ ] B.13.1–4.i / B.13.1–4.h

## B.14–B.15 售后 / 新车跟进
- [x] B.14.1–3.a 售后列表/创建/详情
- [x] B.15.1–3.a 新车跟进列表/创建/详情
- [ ] B.14–B.15 `.i` / `.h`
- [ ] B.15.4 drift：Flutter `fc99691` 成交 UI + 跟进流水 + 店管归属展示 → 对齐（见 L.3）

## B.16 Home 总验收
- [x] B.16.1–3.a 全部二级可点 + 数据契约 + 证据
- [ ] B.16.4 iOS / Harmony 同 B.16.1

---

# C. Chat / IM

## C.1 `/chat` — `ChatPage`
- [x] C.1.1–8.a 会话列表（seed 3 peers、未读、时间分组、滑动操作）
- [ ] C.1.1.i / C.1.1.h

## C.2 `/chat/detail` — `ChatDetailPage`
- [x] C.2.1–7.a 消息列表 + InputPanel + 发送（280ms 回显）+ 对方已读（2s）+ markRead
- [~] C.2.8 语音入口（Flutter 有则做；当前 mock n/a）
- [ ] C.2.1.i / C.2.1.h

## C.3 图片预览
- [x] C.3.1.a 预览 + 手势
- [ ] C.3.1.i / C.3.1.h

## C.4 IM 引擎（可插拔）
- [x] C.4.1 `ImEngine` 接口 + `MockImEngine`（Flutter 对齐）
- [x] C.4.2 三端 actual（CMP/Android/iOS/OHOS native stores）
- [ ] C.4.3 **drift：真融云 IMLib**（Flutter `b70315c`/`90c7b46` 已真连：SDK 会话收发、门店群补拉、自由群 UI）→ 见 L.2
- [ ] C.4.4 BFF 签发 IM Token（Auth Session → Chat IM 关系，CONTEXT-MAP）

---

# D. Community（4 条路由）

## D.1 `/community`
- [x] D.1.1–10.a Feed（seed 35、latest/hot/following、点赞、空态）
- [ ] D.1.1.i / D.1.1.h

## D.2 `/community/publish`
- [x] D.2.1–5.a 文本发布 → 共享引擎（Flutter 同为「开发中」）
- [ ] D.2.1.i / D.2.1.h
- [ ] D.2.6 image_picker 适配（gap: missing，随发布/我的）

## D.3 `/community/search` · D.4 `/community/convention`
- [x] D.3.1–4.a 搜索（历史/结果/空态）
- [x] D.4.1–2.a 公约富文本
- [ ] D.3/D.4 `.i` / `.h`

## D.5 预览/播放 · D.6 Topic · D.7 验收
- [x] D.5.1–2.a 图片预览 + 视频播放入口
- [x] D.6.1.a Topic 详情
- [x] D.7.1.a 模块验收
- [ ] D.5/D.6/D.7 三端补齐

---

# E. Mine / Settings（7 + 8 条路由）

## E.1 `/mine`（共享 Compose 岛）
- [x] E.1.1–20.a Mine 根 UI（头部/常用服务/个人功能/菜单）
- [x] E.1.21 常用服务导航（onQuickServiceTap → 各二级）
- [ ] E.1.1.i / E.1.1.h（三端共用同一份 Compose 岛）

## E.2–E.5 Mine 二级（profile / personalized_settings / addresses+edit / purchase_calculator / http_test）
- [x] E.2.1–4.a 资料编辑 + 保存
- [x] E.3.1–3.a 个性化开关
- [x] E.4.1–4.a 地址列表/编辑（中国省市区选择器）
- [x] E.5.1–2.a 购车计算器
- [x] E.5.3.a http_test（API 探活）
- [ ] E.2–E.5 `.i` / `.h`

## E.6 `/settings`
- [x] E.6.1–6.a 设置项 + 退出登录 + 关于
- [ ] E.6.1.i / E.6.1.h

## E.7 调试页（5 条）
- [n/a] E.7.1–3 dialog_demo / linking_debug / realtime_debug / im_debug / bluetooth_demo（out-of-scope）
- [ ] E.7.4 若保留：仅 Debug 菜单可达

## E.8 发票（2 条，待产品定 scope）
- [ ] E.8.1 `/settings/deal_invoice_demo` — 产品确认 in/out
- [ ] E.8.2 `/settings/deal_invoice/upload` — 上传（图片选择 + 进度）
- [ ] E.8.3 数据契约对齐 Flutter `deal_invoice_api`（models/viewmodel 已在 SoT）

## E.9 环境服务（drift）
- [ ] E.9.1 Flutter `environment_service` 环境切换能力对等（dev/lan/prod 分流，见 L.4）

---

# F. Mall（4 条路由）

- [x] F.1.1–4.a 瀑布流列表 / 详情 / 订单列表 / 订单详情
- [ ] F.1–F.4 `.i` / `.h`
- [~] F.5 支付入口接 G（真支付前显示不可用，无假 Success）

---

# G. Pay / Wallet（3 条路由）

- [x] G.1.1–3.a 支付页 + 会员页 UI（渠道不可用态）
- [x] G.2.1–2.a 钱包 UI
- [ ] G.1/G.2 `.i` / `.h`
- [ ] G.3 微信/支付宝真通道三端（gap: stub；OpenSDK 集成后 sandbox 验收）
- [ ] G.4 订单状态机与 Flutter `pay` 模块对齐（超时/取消/轮询）

---

# H. Video（9 条路由）

## H.1 `/video` Hub
- [x] H.1.1.a 占位 Hub + 入口
- [ ] H.1.1.i / H.1.1.h

## H.2 `/video/short` — `ShortVideoPage`
- [x] H.2.1–16.a feed（上下滑、点赞、评论入口、作者信息、音乐浮标）
- [ ] H.2.1–16 `.i` / `.h`
- [~] H.2.17 播放 Surface（Android 已；iOS/OHOS 真 surface 见 K 缺口）

## H.3–H.5 play / publish / help
- [x] H.3.1–8.a 播放页（进度/手势/倍速）
- [x] H.4.1–8.a 发布（选择/封面/描述）
- [x] H.5.1–2.a 帮助
- [ ] H.3–H.5 `.i` / `.h`

## H.6–H.9 跟读视频/作品（4 条路由）
- [x] H.6.1–4.a 列表 / 详情
- [x] H.8.1–2.a 作品列表
- [ ] H.6–H.9 `.i` / `.h`
- [~] H.10 播放器统一走 `MediaPlayer` expect/actual（替换 stub；见 K 缺口）

---

# I. Classroom（8 条路由）

## I.0 横切
- [x] I.0.1 逻辑模型自 Flutter `ClassroomMockData` 迁移（班级/作业/礼物）
- [x] I.0.2 路由 args 契约（id/role）
- [ ] I.0.3 真数据源（classroom API）—— 后续

## I.1–I.8 页面
- [x] I.1.1–7.a 我的班级列表
- [x] I.2.1–8.a 作业统计
- [x] I.3.1–6.a 作业详情（师）
- [x] I.4.1–5.a 作业详情（生）
- [x] I.5.1–5.a 跟读作业
- [x] I.6.1–6.a 作业批改
- [x] I.7.1–4.a 领礼物卡（一次性领取）
- [x] I.8.1–5.a 课堂视频详情
- [ ] I.1–I.8 `.i` / `.h`
- [~] I.9 邀请/排行榜/创建班（Flutter 为 toast 占位 → 可点主路径验收）

---

# J. Live / Friend / Music / AI / bfui

## J.1 Live（2 条路由）
- [x] J.1.1–3.a 列表 → 房间（mock）
- [ ] J.1.4–9 `.i` / `.h` + 房间内交互
- [~] J.1.10 realtime 联调壳（非完整直播产品；真 SDK 见 K 缺口）

## J.2 Friend `/friend`（通讯录 853 行）
- [x] J.2.1–12.a 壳/动效/搜索/新的朋友/好友/建群/加载错误
- [x] J.2.13 数据契约 `ImFriendApi`（mock_peer_01…03 + ensureChat）
- [ ] J.2.14 好友关系真 API + IM 厂商 SDK（见 L.2）

## J.3 Music（2 条路由）
- [x] J.3.1–6.a 列表 / now_playing / mini player（Android + iOS）
- [ ] J.3.1–10 `.i` / `.h`（音频引擎分端）
- [~] J.3.11 OHOS 播放器（MediaKit；当前 stub）

## J.4 AI `/ai/stream`
- [x] J.4.1–9.a 气泡多轮 + mock chunk 渲染
- [ ] J.4.1–9 `.i` / `.h`
- [ ] J.4.10 **真 SSE**：`AiStreamRepository` 接 Flutter AI 接口（gap: stub）

## J.5 bfui（17 条路由）
- [n/a] J.5.1 全部 out-of-scope（除非产品改口）

---

# L. Flutter drift 同步（`my_ai_project` HEAD `d7876a1` 相对 KMP 基线的新增/变更）

## L.1 鉴权与会话（`d7876a1` unify 401/403 + `10654a9` LAN）
- [x] L.1.1 401/403 决策统一：对齐 Flutter 会话决策（登出/续期/提示分流）
- [x] L.1.2 `wys_network` 403 Forbidden 处理对等（错误映射/文案）
- [x] L.1.3 Token 刷新拦截器（`AuthTokenRefreshInterceptor`）—— 承接 A.3.14
- [x] L.1.4 LAN 后端按设备分流（`BackendLocalhost` 与 Flutter 规则一致；emu `10.0.2.2` / 真机 LAN IP 不入库）

> L.1 备注（2026-10-15 实现记录）：KMP 侧 401 流由 `AuthRefreshingApiClient`（core/network 装饰器）独占，
> feature/auth 经 `AuthSessionBootstrap.install()` 注入策略（`classifyAuthHttp`）与单飞刷新器；
> 刷新请求走 `NetworkFacade.rawApi()`（未装饰 client，Flutter `skipAuthRefresh` 对等）防递归。
> 已知微小差异：Flutter 对 iOS/OHOS 真机未注入 host 也会告警，KMP 仅 Android 告警（行为一致：URL 原样返回）。

## L.2 真融云 IM（`b70315c` / `90c7b46`）
- [ ] L.2.1 IMLib 三端集成（Android/iOS/OHOS 可用性评估 + gap 登记）
- [ ] L.2.2 SDK 会话收发替换 `MockImEngine`（消息模型/已读回执/离线补拉）
- [ ] L.2.3 门店群补拉 + 自由群 UI 对等
- [ ] L.2.4 业务备份客户端（Flutter 已有）对等或明确 n/a
- [ ] L.2.5 IM Token 签发链路（BFF）+ `friend` 关系 API 接线

## L.3 新车跟进成交 UI（`fc99691`）
- [ ] L.3.1 成交态 UI + 跟进流水时间线
- [ ] L.3.2 店管归属展示字段
- [ ] L.3.3 数据契约与 Flutter new-car-follow API 对齐

## L.4 品牌与配置（`622c7cb` / `10654a9`）
- [x] L.4.1 iHome 文案全量替换（KMP 侧残留旧品牌名清查）
- [ ] L.4.2 环境服务（dev/lan/prod）对等 + 切换入口（若产品要）
- [ ] L.4.3 `.env` / `DemoApiHosts` 规则与 Flutter 一致

## L.5 双仓流程
- [ ] L.5.1 对齐 Flutter 侧「双仓编码流程入口」（AGENTS.md 约定）——KMP 侧同款入口

---

# K. 工程债 / 证据 / 门禁

- [x] K.1 共享 `AppRoutePath` 常量表与 Flutter 字符串一致（前序 K.3）
- [x] K.2 `check-layer-deps.sh` 层依赖门禁
- [x] K.3 每路由证据目录规范 `notes/evidence/{module}/{Platform}/{route}.md`
- [x] K.4 `platform-gap-registry.md` 结构（capability × 三端 + blocker + follow_up）
- [~] K.5 截图 diff 工具（`scripts/screenshot_diff_gate.py`；logic-first 期间可选回归）
- [ ] K.6 真 SDK 缺口逐条闭环：Push / 微信登录 / 支付 OpenSDK / OHOS 相机 / OHOS Web / OHOS MediaKit / image_picker / SSE
- [ ] K.7 设计令牌终检：`core/design` 全量对齐 Flutter `AppTheme`（像素证据）
- [ ] K.8 本清单与 `notes/route-inventory.md` 周同步（状态 + Flutter diff 巡检）
- [ ] K.9 全量对齐期结束 → 恢复 Android 截图 diff 硬门禁 + `packet-pass` 判定

---

# 波次

| Wave | 范围 | 出口 |
|---|---|---|
| W0 | A 全段三端 | 登录 resume + 401/403 + gap 登记 |
| W1 | B.1–B.3 + C + D.1–D.2 | 四 Tab 主路径无挡板 |
| W2 | B.10–B.15 + E | Home/Mine 二级可点 |
| W3 | F + G | 商城/钱包/会员（真支付前不可用态一致） |
| W4 | H + I + J.1 | 内容模块 |
| W5 | W0–W4 的 iOS | 同出口 |
| W6 | W0–W4 的 Harmony | 同出口 |
| W7 | L drift + K.6 真 SDK | 全量对齐 |

# 规模估算（相对前序 490 项的净增量）

| 段 | 一级条目约 | 三端展开后约 |
|---|---|---|
| A 壳鉴权桥（含 401/403/刷新/微信） | ~25 | ~60 |
| B Home 28 路由（含精修 + drift） | ~60 | ~140 |
| C Chat/IM（含真融云） | ~20 | ~60 |
| D Community | ~15 | ~40 |
| E Mine/Settings（含发票/环境） | ~20 | ~50 |
| F Mall | ~8 | ~20 |
| G Pay/Wallet（含真通道） | ~10 | ~30 |
| H Video | ~20 | ~55 |
| I Classroom | ~15 | ~40 |
| J Live/Friend/Music/AI（含 SSE） | ~20 | ~55 |
| L drift | ~18 | ~45 |
| K 工程/门禁 | ~9 | ~9 |
| **合计** | **~240** | **~600** |

> 说明：前序 373 项 done 不在本表重复计数；本表只计**净剩余 + drift**。`.i`/`.h` 展开是最大单项（约 45%），可按波次 W5/W6 整体排期。

---

# 复审修订（2026-10-15）

> 代码核查结论（见 `notes/review-2026-10-15.md`）：iOS/Harmony **原生壳**已实现绝大多数**主路由**页面，
> 前序把 `.i`/`.h` 一律标 `[ ]` 已不准确。真实剩余缺口 = **二级详情/创建子路由**落到原生壳
> `genericList`（iOS）/ `default`（Harmony）占位 stub。本段补全子路由 stub 子任务，并标注主路由 `.i`/`.h` 已被原生壳覆盖（随 W5/W6 收尾批量改 `[x]`）。

## R1 过时登记修正
- ✅ `token refresh interceptor`：W0 已实现共享 KMP 机制（`AuthRefreshingApiClient` + `AuthTokenRefresh` + `AuthSessionBootstrap`），`platform-gap-registry.md` 已由 `missing` 改为 `ready`。L.1.3 / A.3.14 维持 `[x]`。

## R2 W5.i — iOS 子路由 stub 闭环（NativeFeatureHost 专属页面）
- [x] W5.i.1 `/home/used_car/detail` 专属页（复用 NativeUsedCarPage 数据）— 已实现并接入 NativeFeatureHost，`swiftc -parse` 校验通过
- [x] W5.i.2 `/home/used_car/create` 表单页（车型/车牌/客户/估价 + 提交 mock）— 已实现并接入
- [x] W5.i.3 `/home/ledger/detail` 明细页 — 已实现并接入
- [x] W5.i.4 `/home/data_analytics/detail` 图表详情页 — 已实现并接入
- [x] W5.i.5 `/home/new_car_follow/create` 建跟进表单 — 已实现并接入
- [x] W5.i.6 `/home/new_car_follow/detail` 跟进详情 + 流水 — 已实现并接入
- [x] W5.i.7 `/home/after_sales/create` 建工单表单 — 已实现并接入
- [x] W5.i.8 `/home/after_sales/detail` 售后详情 — 已实现并接入
- [x] W5.i.9 `/mall/orders/detail` 订单详情 — 已实现并接入
- [x] W5.i.10 `/video/transliter/videos` + `/videos/detail` 跟读视频列表/详情 — 已实现并接入
- [x] W5.i.11 `/video/transliter/works` + `/works/detail` 作品列表/详情 — 已实现并接入
- [x] W5.i.12 `/community/convention` 公约富文本 — 已实现并接入
- [x] W5.i.13 `/live/room` 直播间内页 — 已实现并接入
- [x] W5.i.14 `/music/now_playing` 播放页 — 已实现并接入
- [x] W5.i.15 `/video` Hub 容器页 — 已实现并接入

## R3 W6.h — Harmony 子路由 stub 闭环（NativeFeaturePane 专属 pane）
- [x] W6.h.1–15 同 W5.i.1–15 的 ArkTS 等价实现（17 个 NativeXxxPane）+ `resolveNativePath`/`nativePaneIdFor` 分发修正 + `Index.ets` 渲染分支 — 已实现并接线；DevEco 整包构建待验证

## R4 主路由 `.i`/`.h` 标记校正（随 W5/W6 收尾批量改 `[x]`）
- 下列主路由原生壳已实现，原 `[ ]` `.i`/`.h` 视为被原生壳覆盖：B.1.30、B.2–B.9、B.10–B.16、C.1–C.3、D.1–D.4、E.1–E.6、F.1–F.4、G.1–G.2、H.1–H.9、I.1–I.8、J.1–J.4（除真 SDK 项 L.2/J.4.10/G.3 等仍 `[ ]`）。

## R5 当前已开工
- [x] W5.i.1–W5.i.15（iOS）与 W6.h.1–W6.h.15（Harmony，共 17 个子路由）已全部实现并接入各自原生壳（NativeFeatureHost / Index.ets + NativeFeatureCatalog）。Xcode / DevEco 整包构建待在对应 IDE 验证；ArkTS 已按既有模式对齐、`swiftc -parse` 已通过。
