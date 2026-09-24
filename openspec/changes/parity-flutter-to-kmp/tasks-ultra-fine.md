# Flutter → KMP 超细粒度任务清单

> SoT：`my_ai_project` · 路由权威：`commons/wys_router/.../route_path.dart`  
> 完成定义：**可点路径 + 数据契约 + 三端**（默认可先 Android）  
> 状态：`[ ]` 未做 · `[~]` 部分 · `[x]` 可验收 · `[n/a]` 明确不做  
> ID 规则：`{模块}.{路由简写}.{序号}`  
> 每项建议工时粒度：**0.5–1 人日以内**（更大则再拆）

**平台后缀**（未写 = 先 Android，后补 iOS/OHOS）：  
- `.a` Android · `.i` iOS · `.h` Harmony

---

# A. 壳 / 鉴权 / 桥接

## A.0 Splash `/`
- [x] A.0.1.a 展示闪屏图/Logo，定时进入下一步
- [ ] A.0.1.i / A.0.1.h 同上（platform-gap: missing）
- [x] A.0.2 与 Flutter 资源一致（非随机图）
- [~] A.0.3 证据：冷启动截图三端（Android 见 notes/evidence/shell/Android/module-A.md）

## A.1 Main `/main`
- [x] A.1.1.a 四 Tab 容器（首页/聊天/社区/我的）
- [ ] A.1.1.i / A.1.1.h
- [x] A.1.2 底栏高度 49、选中 pill、图标字重对齐 Flutter（MainBottomBar）
- [x] A.1.3 Tab 切换不销毁根状态（keptTabs IndexedStack）
- [x] A.1.4 soft-auth：未登录进需登录 Tab → 登录 → resume

## A.2 Privacy（壳内，非 RoutePath）
- [x] A.2.1.a 首次同意弹层
- [x] A.2.2 同意持久化，二次启动跳过
- [x] A.2.3 不同意保持阻断（对齐 Flutter 重试文案）
- [ ] A.2.1.i / A.2.1.h

## A.3 Auth 路由

### `/login`
- [x] A.3.1.a 登录首页 UI（入口：密码/OTP/注册；合并页）
- [x] A.3.2 各入口导航正确（segmented + 注册）
- [x] A.3.3 错误/加载态
- [ ] A.3.1.i / A.3.1.h

### `/login/password`
- [x] A.3.4.a 账密表单 + 提交（LoginScreen Email 模式）
- [x] A.3.5 校验文案对齐 Flutter
- [x] A.3.6 成功写会话 + 跳转 redirectRoute / pendingTab
- [ ] A.3.4.i / A.3.4.h

### `/login/otp`
- [x] A.3.7.a 手机号 + 验证码 UI
- [x] A.3.8 发码倒计时 / 频控错误
- [x] A.3.9 校验登录成功
- [ ] A.3.7.i / A.3.7.h

### `/register`
- [x] A.3.10.a 注册表单 UI
- [x] A.3.11 提交 API + 限流文案
- [x] A.3.12 成功后进登录或自动登录
- [ ] A.3.10.i / A.3.10.h

### Auth 横切
- [x] A.3.13 401 清会话
- [ ] A.3.14 Token 刷新策略（若 Flutter 有）— missing，见 platform-gap
- [ ] A.3.15 微信登录：每端 `done|missing` 登记 — missing
- [x] A.3.16 验收：guest→login→resume Chat（Android；证据 module-A.md）

## A.4 Bridges

### `/web`
- [x] A.4.1.a WebView 打开/返回
- [x] A.4.2 JS Bridge 方法集对齐 Flutter `WebBridgeRegistry`（核心 8 方法）
- [x] A.4.3 Home WebHandlers 注册（`HomeWebHandlers`）
- [ ] A.4.1.i / A.4.1.h

### Scan / Deeplink / Push
- [x] A.4.4 扫码权限拒绝 UX
- [x] A.4.5 扫码成功回调路由
- [x] A.4.6 Deeplink 冷启动
- [~] A.4.7 Push 注册 + payload→route（StubPushBridge；厂商 SDK missing）
- [ ] A.4.4–7 三端

---

# B. Home 模块（`moduleId=home`，Tab order=0）

## B.1 根页 `/home` — `HomePage`

### UI 区块
- [x] B.1.1 问候 `HomeGreetingSection`（时段+用户名）
- [x] B.1.2 消息 pill / 通知入口
- [x] B.1.3 `HomeSearchBar` → `/home/search`
- [x] B.1.4 扫一扫入口 → Scan
- [x] B.1.5 `HomeBannerSection` + 点击跳转
- [x] B.1.6 `HomeFeatureGrid`（44dp、最多 9、含「更多」）
- [x] B.1.7 `HomeTodoCardStrip`（有数据才显示；失败隐藏）
- [x] B.1.8 `HomeStoreMetricsCard`（门店名/今日昨日30天/指标/明细）
- [x] B.1.9 投资策略入口 → `/home/strategy`
- [x] B.1.10 `HomeServiceGrid`
- [x] B.1.11 `HomeContactList`
- [x] B.1.12 `HomeNewsList`
- [x] B.1.13 学习报告入口 → `/home/learning_report`
- [x] B.1.14 下拉刷新 `refreshDashboard`
- [~] B.1.15 加载中 / 错误重试 placeholder（刷新有；全页 error 未做）
- [ ] B.1.16 签到弹窗 `DailyCheckInDialog.maybeShow`
- [ ] B.1.17 音乐 mini-player 底 inset（若 Music 启用）— 随 J.3

### 数据
- [~] B.1.18 `HomeController` / dashboard 模型对齐（mock）
- [x] B.1.19 指标 Tab 切换请求（本地切 mock）
- [~] B.1.20 门店切换 `onStoreTap`（入口可点→台账）
- [~] B.1.21 Todo API：mock 对齐；真 API 待接

### Feature 格子导航（每格单独项）
- [x] B.1.22 「更多」→ `/home/all_services`
- [x] B.1.23 「生活服务」→ `/home/life_service`
- [x] B.1.24 「直播带货」→ `/home/live_commerce`
- [x] B.1.25 「Club」→ `/home/club`
- [x] B.1.26 「二手车」→ `/home/used_car`
- [x] B.1.27 「新车跟进」→ `/home/new_car_follow`
- [x] B.1.28 「H5 调试」→ InAppWeb
- [x] B.1.29 其余格子按 Flutter catalog 一一接线（AI小石头/新车成交→对应路由或 stub）

### 平台 / 验收
- [ ] B.1.30.i / B.1.30.h 根页同能力
- [x] B.1.31 验收：刷新、Todo、九宫格真跳转（Android；evidence module-B.md）

---

## B.2 `/home/all_services` — `AllServicesPage`
- [x] B.2.1.a 从「更多」可打开（KMP 已有屏）
- [x] B.2.2 分区数据 `all_services_data` 对齐
- [~] B.2.3 常用服务编辑：增删、下限 3、写回（编辑可用；跨页持久化未做）
- [~] B.2.4 每项 tap → 导航表（部分走 HomeRoutes / stub）
- [x] B.2.5 顶栏返回
- [ ] B.2.1.i / B.2.1.h
- [~] B.2.6 验收：编辑常用（本页内）

## B.3 `/home/search` — `SearchPage`
- [x] B.3.1.a 入口：首页搜索框
- [x] B.3.2 搜索框 / 历史 / 热词 / 结果列表
- [x] B.3.3 空态、清除历史
- [~] B.3.4 结果项跳转（对齐 Flutter）
- [ ] B.3.1.i / B.3.1.h

## B.4 `/home/strategy` — `StrategyPage`
- [x] B.4.1.a 页 UI（九宫格/策略卡片）
- [x] B.4.2 数据源 / 交互
- [x] B.4.3 首页入口接线
- [ ] B.4.1.i / B.4.1.h

## B.5 `/home/learning_report` — `HomeLearningReportPage`
- [x] B.5.1.a 报告 UI 区块全做
- [x] B.5.2 数据加载
- [x] B.5.3 首页入口
- [ ] B.5.1.i / B.5.1.h

## B.6 `/home/check_in_mall` — `CheckInMallPage`
- [x] B.6.1.a 积分商城 UI
- [~] B.6.2 `points_api` 契约（mock）
- [x] B.6.3 签到动作
- [~] B.6.4 Mine 日历入口接线（HomeRoutes 已备）
- [ ] B.6.1.i / B.6.1.h

## B.7 `/home/dubbing_feed` — `DubbingHomePage`
- [x] B.7.1.a 配音 Feed UI
- [x] B.7.2 热榜卡 → `/home/hot_rank_detail`
- [~] B.7.3 与 Video 配音模块跳转（随 H）
- [ ] B.7.1.i / B.7.1.h

## B.8 `/home/hot_rank_detail` — `HotRankDetailPage`
- [x] B.8.1.a 详情列表 UI
- [x] B.8.2 mock/真数据对齐
- [ ] B.8.1.i / B.8.1.h

## B.9 容器页（同一 `HomeFeatureContentPage`）

### `/home/life_service`（child=`HomeVideoTabContent`）
- [x] B.9.1.a 标题「生活服务」+ Video Tab 内容
- [ ] B.9.1.i / B.9.1.h

### `/home/live_commerce`（child=`HomeClubTabContent`）
- [x] B.9.2.a 标题「直播带货」+ Club 内容
- [ ] B.9.2.i / B.9.2.h

### `/home/club`
- [x] B.9.3.a 标题「Club」+ Club 内容
- [ ] B.9.3.i / B.9.3.h

## B.10 二手车

### `/home/used_car` 列表
- [x] B.10.1.a 列表 UI + Binding
- [x] B.10.2 分页/筛选/空态（列表+空态；分页 mock）
- [x] B.10.3 进详情 / 创建
- [~] B.10.4 `used_car_order` + `transaction` repo（mock）
- [ ] B.10.1.i / B.10.1.h

### `/home/used_car/detail`
- [x] B.10.5.a 详情字段全量
- [x] B.10.6 操作按钮（对齐 Flutter）
- [ ] B.10.5.i / B.10.5.h

### `/home/used_car/create`
- [x] B.10.7.a 表单字段 + 校验
- [x] B.10.8 提交成功回列表
- [ ] B.10.7.i / B.10.7.h

## B.11 台账

### `/home/ledger`
- [x] B.11.1.a 列表 + Binding
- [~] B.11.2 `transaction_repository`（mock）
- [ ] B.11.1.i / B.11.1.h

### `/home/ledger/detail`
- [x] B.11.3.a 详情 + `TransactionListItem`
- [ ] B.11.3.i / B.11.3.h

## B.12 数据分析

### `/home/data_analytics`
- [x] B.12.1.a 列表 UI + Binding
- [~] B.12.2 `analytics_repository`（mock）
- [ ] B.12.1.i / B.12.1.h

### `/home/data_analytics/detail`
- [x] B.12.3.a 详情图表/字段
- [ ] B.12.3.i / B.12.3.h

## B.13 Todo 子页（`home_todo_pages.dart`）

### `/home/todo/partner-pending`
- [x] B.13.1.a 页面 + API `join-applications`（mock）
- [ ] B.13.1.i / B.13.1.h

### `/home/todo/follow-up-customers`
- [x] B.13.2.a + API `follow-up-customers`（mock）
- [ ] B.13.2.i / B.13.2.h

### `/home/todo/after-sales-appointments`
- [x] B.13.3.a + API `after-sales-appointments`（mock）
- [ ] B.13.3.i / B.13.3.h

### `/home/todo/order-pending-review`
- [x] B.13.4.a + API `store-review-orders`（mock）
- [ ] B.13.4.i / B.13.4.h

### Todo 接线
- [x] B.13.5 根页 Todo 卡点击 → 对应子页
- [~] B.13.6 登录门（若 Flutter 要求）— soft-auth 沿用壳

## B.14 售后

### `/home/after_sales`
- [x] B.14.1.a 列表 + Binding
- [ ] B.14.1.i / B.14.1.h

### `/home/after_sales/create`
- [x] B.14.2.a 创建表单
- [ ] B.14.2.i / B.14.2.h

### `/home/after_sales/detail`
- [x] B.14.3.a 详情
- [ ] B.14.3.i / B.14.3.h

## B.15 新车跟进

### `/home/new_car_follow`
- [x] B.15.1.a 列表 + Binding
- [ ] B.15.1.i / B.15.1.h

### `/home/new_car_follow/create`
- [x] B.15.2.a 创建
- [ ] B.15.2.i / B.15.2.h

### `/home/new_car_follow/detail`
- [x] B.15.3.a 详情
- [ ] B.15.3.i / B.15.3.h

## B.16 Home 模块总验收
- [x] B.16.1 Android：根→全部服务→二手车列表→详情→返回（compile + route host）
- [x] B.16.2 Android：Todo 卡→子页
- [x] B.16.3 Android：指标 Tab 切换有请求证据（本地 mock 切换）
- [ ] B.16.4 iOS / Harmony 同 B.16.1

---

# C. Chat（`moduleId` talk/chat）

## C.1 `/chat` — `ChatPage`
- [x] C.1.1.a 标题「消息」+ 搜索/发起按钮
- [x] C.1.2.a 会话列表行（头像/名/摘要/时间/未读/在线）
- [x] C.1.3 搜索会话真正过滤
- [x] C.1.4 数据：MockIm ↔ 真 IM 开关（MockImEngine；真 SDK missing）
- [x] C.1.5 soft-auth gate + resume（Module A）
- [x] C.1.6 点击行 → `/chat/detail`
- [ ] C.1.1.i / C.1.1.h
- [x] C.1.7 验收：未登录进 Chat → 登录 → 回列表（壳 soft-auth）

## C.2 `/chat/detail` — `ChatDetailPage`
- [x] C.2.1.a 顶栏对方昵称 + 返回
- [x] C.2.2.a 气泡列表（左右）
- [x] C.2.3.a 输入框 + 发送（MockImEngine）
- [~] C.2.4 历史分页 / 时间分隔（时间标签有；分页未做）
- [x] C.2.5 发送中/失败重试
- [x] C.2.6 图片消息发送（`[image]` mock）
- [x] C.2.7 图片点击 → Chat `image_preview`
- [ ] C.2.8 语音入口（Flutter 有则做）— n/a mock
- [ ] C.2.1.i / C.2.1.h
- [x] C.2.9 验收：发送一条可见于列表

## C.3 Chat 图片预览
- [x] C.3.1.a 全屏预览 + 关闭（左右滑可后续）
- [ ] C.3.1.i / C.3.1.h

## C.4 IM 引擎
- [x] C.4.1 `ImGateway` 与 Flutter 融云开关文档化（MockImEngine 注释）
- [x] C.4.2 Mock 可验收；真 SDK 每端 `missing`（platform-gap）
- [~] C.4.3 收消息实时刷新列表（发送后刷新；无推送仿真）

---

# D. Community

## D.1 `/community` — `CommunityPage`
- [x] D.1.1.a 标题 + 「+」+ 搜索框
- [x] D.1.2.a Tab：最新/热门/关注 + 指示条
- [x] D.1.3.a 帖头（头像/名/时间来源/更多）
- [x] D.1.4.a 富文本 @/#/链接
- [x] D.1.5.a 图片九宫格 3×9（`ImageGridWidget` 合同）
- [x] D.1.6.a 视频 16:9 + 播放覆盖
- [x] D.1.7.a 赞/评/分享行
- [x] D.1.8.a 评论预览区
- [x] D.1.9 全量 Feed（Mock + 发布置顶）
- [x] D.1.10 Tab 排序逻辑对齐 Flutter（热门/关注过滤）
- [x] D.1.11 点赞乐观更新
- [~] D.1.12 下拉刷新 / 上拉更多（未做；Tab 切换可验收）
- [x] D.1.13 空/错/加载态（关注空态）
- [x] D.1.14 「+」→ `/community/publish`
- [x] D.1.15 搜索框 → `/community/search`
- [x] D.1.16 九宫格点击 → image preview
- [x] D.1.17 视频点击 → video play
- [x] D.1.18 soft-auth（壳）
- [ ] D.1.1.i / D.1.1.h
- [x] D.1.19 验收：最新/热门切换结果不同

## D.2 `/community/publish` — `PublishPage`
- [x] D.2.1.a 文案输入
- [x] D.2.2 选图（最多 9）
- [x] D.2.3 选话题 → topic select
- [x] D.2.4 校验（空文案等）
- [x] D.2.5 提交成功回 Feed 置顶
- [ ] D.2.1.i / D.2.1.h

## D.3 `/community/search` — `CommunitySearchPage`
- [x] D.3.1.a 搜索 UI
- [x] D.3.2 动态/话题/用户分区结果
- [x] D.3.3 空态
- [ ] D.3.1.i / D.3.1.h

## D.4 `/community/convention` — `CommunityConventionPage`
- [x] D.4.1.a 公约内容页/弹窗
- [x] D.4.2 触发时机对齐 Flutter（⋯）
- [ ] D.4.1.i / D.4.1.h

## D.5 Community 预览/播放
- [x] D.5.1.a `image_preview_page` 多图
- [x] D.5.2.a `video_play_page`（mock 播放器）
- [ ] D.5.1.i / D.5.1.h / D.5.2.i / D.5.2.h

## D.6 Topic
- [x] D.6.1.a `topic_select_page`
- [ ] D.6.1.i / D.6.1.h

## D.7 Community 验收
- [x] D.7.1 Android：浏览→预览→发布→回列表
- [ ] D.7.2 iOS / Harmony 同

---

# E. Mine / Settings（Tab「我的」）

## E.1 `/mine` — `MinePage`

### UI
- [x] E.1.1.a Header（头像/名/角色/公司/名片/电话）
- [x] E.1.2.a 统计条
- [x] E.1.3.a 常用服务四宫格
- [x] E.1.4.a 个人功能区
- [x] E.1.5.a 菜单列表
- [ ] E.1.6 滚动顶栏渐显（Flutter `_navFadeExtent=72`）

### 常用服务导航（`onQuickServiceTap`）
- [x] E.1.7 `mall` → `/mall`
- [x] E.1.8 `order` → `/mall/orders`
- [x] E.1.9 `wallet` → `/wallet`
- [x] E.1.10 `course` → Classroom 入口
- [x] E.1.11 「商城」走 Mall（Membership 独立）

### 个人功能
- [~] E.1.12 短信模板（stub toast）
- [x] E.1.13 购车计算器 → `/mine/purchase_calculator`
- [x] E.1.14 二手车 → `/home/used_car`
- [x] E.1.15 小视频 → `/video/short`
- [x] E.1.16 售后等（售后→Home after_sales）

### 菜单
- [x] E.1.17 地址 → `/mine/addresses`
- [x] E.1.18 设置 → `/settings`（MineIsland）
- [~] E.1.19 feedback/fan_group/invite/reminder/cooperation（stub 标签）
- [x] E.1.20 顶栏：个性化 / 设置 / 日历签到 / 登出

### 平台
- [ ] E.1.1.i / E.1.1.h
- [x] E.1.21 验收：商城/钱包/订单/计算器各一跳

## E.2 `/mine/profile`
- [x] E.2.1.a 资料表单
- [~] E.2.2 保存 API（本地 toast）
- [x] E.2.3 需登录（壳 soft-auth 覆盖敏感能力）
- [ ] E.2.1.i / E.2.1.h

## E.3 `/mine/personalized_settings`
- [x] E.3.1.a UI（岛内）
- [~] E.3.2 开关持久化（岛内已有部分）
- [ ] E.3.1.i / E.3.1.h

## E.4 `/mine/addresses` + `/mine/addresses/edit`
- [x] E.4.1.a 列表
- [x] E.4.2.a 编辑/新建
- [x] E.4.3 默认地址
- [ ] E.4.1.i / E.4.1.h

## E.5 `/mine/purchase_calculator`
- [x] E.5.1.a 计算器 UI/公式对齐
- [ ] E.5.1.i / E.5.1.h

## E.6 `/settings`
- [x] E.6.1.a 设置分组列表对齐 Flutter
- [ ] E.6.2 各项跳转
- [ ] E.6.1.i / E.6.1.h

## E.7 调试页（默认 n/a，除非产品要）
- [n/a] E.7.1 `/mine/http_test`
- [n/a] E.7.2 `/settings/dialog_demo`
- [n/a] E.7.3 linking/realtime/im/bluetooth debug
- [ ] E.7.4 若保留：仅 Debug 菜单可达

## E.8 发票
- [ ] E.8.1 `/settings/deal_invoice_demo` — 产品确认 in/out scope
- [ ] E.8.2 `/settings/deal_invoice/upload`

---

# F. Mall

## F.1 `/mall`
- [x] F.1.1.a 列表 UI
- [x] F.1.2 进详情
- [ ] F.1.1.i / F.1.1.h

## F.2 `/mall/detail`
- [x] F.2.1.a 详情
- [ ] F.2.1.i / F.2.1.h

## F.3 `/mall/orders`
- [x] F.3.1.a 订单列表
- [ ] F.3.1.i / F.3.1.h

## F.4 `/mall/orders/detail`
- [x] F.4.1.a 订单详情
- [ ] F.4.1.i / F.4.1.h

## F.5 Mall 验收
- [x] F.5.1 Android：Mine 商城→详情→订单
- [ ] F.5.2 iOS / Harmony

---

# G. Pay / Wallet

## G.1 `/pay` + `/pay/membership`
- [x] G.1.1.a 收银台 stub + MembershipScreen
- [~] G.1.2 真支付 SDK missing（platform-gap）
- [ ] G.1.1.i / G.1.1.h

## G.2 `/wallet`
- [x] G.2.1.a 钱包余额 + 充值入口
- [ ] G.2.1.i / G.2.1.h

---

# H. Video（SoT：`features/video` · `VideoModule`）

> Hub `/video` 在 Flutter 仅为占位「Video 模块」；真实能力在 short + dubbing。

## H.1 `/video` — `VideoPage`（占位 Hub）
- [x] H.1.1.a 对齐 Flutter：标题「视频」+ 中心占位文案（或产品决定改为入口跳板）
- [x] H.1.2 若改为跳板：链到 short / dubbing 列表
- [ ] H.1.1.i / H.1.1.h

## H.2 `/video/short` — `ShortVideoPage`

### UI 区块
- [x] H.2.1.a `AppNavBar` 标题 + 帮助入口（→ `/video/short/help`）
- [x] H.2.2 `ShortVideoProfileCard`：头像 / 昵称 / 职务·门店（与 Mine 同源 `profiles/me`）
- [x] H.2.3 `_StatsRow`：作品数 / 赞 / 等（`GET /short-videos/profile`）
- [x] H.2.4 `ShortVideoPublishTile` 虚线发布入口（→ publish）
- [x] H.2.5 `_SectionHeader`「我的作品」
- [x] H.2.6 瀑布流 / 交错网格：`ShortVideoItemTile`（封面、状态角标、时长）
- [x] H.2.7 `ShortVideoEmptyState` 空态插画 + CTA
- [x] H.2.8 `_ListFooter` 加载更多 / 没有更多
- [x] H.2.9 下拉刷新 + 触底 `loadMore`（距底 240）

### 交互
- [x] H.2.10 点 tile → `/video/short/play` + `ShortVideoPlayArgs(initialIndex, items)`
- [x] H.2.11 uploading 状态 toast「视频上传中…」
- [x] H.2.12 无 `videoUrl` toast「暂无可播放地址」
- [x] H.2.13 长按/菜单删除确认对话框 + API

### 数据 / 契约
- [x] H.2.14 Repository：`/api/v1/short-videos` 列表分页模型对齐 `ShortVideoItemModel`
- [x] H.2.15 Profile 接口对齐 `ShortVideoProfileModel` / `ShortVideoStatsModel`
- [x] H.2.16 登录门：未登录进 short → login → resume

### 平台 / 入口
- [x] H.2.17 Mine/Home「小视频」入口接线
- [ ] H.2.1–16 `.i` / `.h`
- [x] H.2.18 验收：列表→播放→返回列表位置保持

## H.3 `/video/short/play` — `ShortVideoPlayPage`
- [x] H.3.1.a 全屏竖滑 Feed（对齐 toolkit `ShortVideoFeedView` 行为）
- [x] H.3.2 初始 index + items 来自路由 args
- [x] H.3.3 单击暂停/播放；双击点赞（若 Flutter 有）
- [x] H.3.4 侧栏：头像关注 / 赞 / 评 / 分享（对齐 Flutter 实际控件）
- [x] H.3.5 底栏标题 + 话题 tag
- [x] H.3.6 横屏页（若 Flutter `ShortVideoLandscapePage` 启用）
- [x] H.3.7 **真播放器 adapter**（禁 Stub 验收）；缓冲/错误态
- [x] H.3.8 网络切换提示（对齐 `VideoNetworkWatcher` 语义）
- [ ] H.3.1–8 `.i` / `.h`
- [x] H.3.9 验收：连续滑 ≥3 条可播 + 返回

## H.4 `/video/short/publish` — `ShortVideoPublishPage`
- [x] H.4.1.a 本地视频预览 `_LocalVideoPreview`（封面图或首帧）
- [x] H.4.2 点预览全屏
- [x] H.4.3 标题输入 + 字数限制（对齐 Flutter）
- [x] H.4.4 话题选择/输入
- [x] H.4.5 相机/麦克风权限门（`CameraPermissionGate` 语义）
- [x] H.4.6 选视频 / 拍视频入口
- [x] H.4.7 提交：Flutter 注明片源可由服务端默认填充 — KMP 对齐同一契约
- [x] H.4.8 成功回 short 列表并刷新
- [ ] H.4.1–8 `.i` / `.h`

## H.5 `/video/short/help` — `ShortVideoHelpPage`
- [x] H.5.1.a 说明页 + `_StepCard` 步骤列表
- [x] H.5.2 文案/步骤数对齐 Flutter
- [ ] H.5.1.i / H.5.1.h

## H.6 `/video/dubbing/videos` — `DubbingVideoListPage`
- [x] H.6.1.a 列表 + `_VideoCard`（封面、标题、标签）
- [x] H.6.2 点卡 → `/video/dubbing/videos/detail`
- [x] H.6.3 Mock / API 切换策略与 Flutter `dubbing_media_mock_data` 对齐
- [x] H.6.4 与 `/home/dubbing_feed` 互通（同列表或跳转）
- [ ] H.6.1–4 `.i` / `.h`

## H.7 `/video/dubbing/videos/detail` — `DubbingVideoDetailPage`
- [x] H.7.1.a `PlayableVideoHeader` 沉浸头（字幕/水印叠加）
- [x] H.7.2 `_TitleSection` / `_TagsSection` / `_DescriptionSection`
- [x] H.7.3 `_UploaderSection`
- [x] H.7.4 `_AlbumSection` 分集
- [x] H.7.5 `_LatestWorksSection` → works
- [x] H.7.6 `_LeaderboardSection` 排行
- [x] H.7.7 `_DubbingBottomBar`：配音/收藏/分享（对齐 Flutter 按钮）
- [x] H.7.8 底栏延伸安全区
- [ ] H.7.1–8 `.i` / `.h`

## H.8 `/video/dubbing/works` — `DubbingWorkListPage`
- [x] H.8.1.a 作品列表 `_WorkCard`
- [x] H.8.2 点卡 → `/video/dubbing/works/detail`
- [ ] H.8.1.i / H.8.1.h

## H.9 `/video/dubbing/works/detail` — `DubbingWorkDetailPage`
- [x] H.9.1.a Tab：介绍 `_IntroTab` / 评论 `_CommentsTab`
- [x] H.9.2 `_MoreWorkRow` 更多作品
- [x] H.9.3 `_BottomBar` 操作
- [x] H.9.4 播放头复用 `PlayableVideoHeader`
- [ ] H.9.1–4 `.i` / `.h`

## H.10 Video 总验收
- [x] H.10.1 短视频：入口→列表→播→发→帮助 全通（Android 证据）
- [x] H.10.2 配音：feed/list→detail→works→detail 全通
- [x] H.10.3 播放器非 Stub 声明写入 `platform-gap-registry`
- [x] H.10.4 iOS / Harmony 同路径抽检

---

# I. Classroom（SoT：`features/classroom` · 当前多为 `ClassroomMockData`）

> 数据层先 mock 对齐 UI；若产品要求真 API，另开契约项，不阻塞可点路径。

## I.0 横切
- [x] I.0.1 `ClassroomColors` / `ClassroomDimens` token 迁 KMP（绿主色、卡片圆角）
- [x] I.0.2 模型：`ClassInfo` / `HomeworkType` / `TimeFilter` / `HomeworkTab` / …
- [x] I.0.3 路由 args：`ClassroomRouteArgs(classId=…)` / studentId
- [x] I.0.4 登录门：Mine「课程」→ classroom
- [x] I.0.5 Mine `course` 快捷入口 → `/classroom/my_class`

## I.1 `/classroom/my_class` — `MyClassListPage`
- [x] I.1.1.a NavBar「我的班级」+ 返回
- [x] I.1.2 顶行「班级」+「禁用班级」toast（开发中，对齐 Flutter）
- [x] I.1.3 `_ClassCard`：名称 / 邀请码 / 成员数
- [x] I.1.4 卡底三按钮：邀请同学(toast) / 作业统计→I.2 / 排行榜(toast)
- [x] I.1.5 「作业点评 >」→ `/classroom/homework/review`
- [x] I.1.6 底栏「创建班级」toast（开发中）
- [x] I.1.7 列表数据：`ClassroomMockData.classes` 或真 API
- [ ] I.1.1–7 `.i` / `.h`

## I.2 `/classroom/homework_stats` — `ClassHomeworkStatsPage`
- [x] I.2.1.a NavBar + 时间 pill（本周/本月/上月/自定义）
- [x] I.2.2 自定义 → `CustomTimeRangeSheet`
- [x] I.2.3 `_TabChip`：全部 / 配音 / 同步（`HomeworkTab`）
- [x] I.2.4 `_StatsCard` + `_StatItem` + `_StatDivider` 汇总
- [x] I.2.5 `_TypeTag` 类型角标
- [x] I.2.6 `_StudentListCard` / `_StudentRow` 学生完成/待做
- [x] I.2.7 点学生 → `/classroom/homework/detail_teacher`（带 studentId）
- [x] I.2.8 Controller：`HomeworkStatsController` 过滤逻辑对齐
- [ ] I.2.1–8 `.i` / `.h`

## I.3 `/classroom/homework/detail_teacher` — `HomeworkDetailTeacherPage`
- [x] I.3.1.a NavBar「作业详情」+「导出成绩」按钮（可先空实现对齐可见性）
- [x] I.3.2 `_ProfileCard` 学生头像 emoji / 作业数 / 完成率
- [x] I.3.3 `_StatusTabs`：全部/已完成/未完成（`HomeworkStatusTab`）
- [x] I.3.4 `_TimelineItem` 列表（类型、日期、班级名）
- [x] I.3.5 点时间线条 → student detail / dubbing / review（对齐 Flutter 实际跳转）
- [x] I.3.6 Binding + Controller 注册
- [ ] I.3.1–6 `.i` / `.h`

## I.4 `/classroom/homework/detail_student` — `HomeworkDetailStudentPage`
- [x] I.4.1.a `_UserCard`
- [x] I.4.2 `_ProgressCard` 进度
- [x] I.4.3 `_SocialProofRow`
- [x] I.4.4 `_ContentCard` + `_TaskRow`（星奖励、题量）
- [x] I.4.5 点任务 → dubbing homework 或对应类型页
- [ ] I.4.1–5 `.i` / `.h`

## I.5 `/classroom/homework/dubbing` — `DubbingHomeworkPage`
- [x] I.5.1.a `_GradientHeader`
- [x] I.5.2 `_DubbingItemRow` 列表（分数、可重交）
- [x] I.5.3 设置 → `DubbingSettingsSheet`（`DubbingMode` / `ScoringMode` / Switch 行）
- [x] I.5.4 提交/重交交互对齐 Flutter
- [x] I.5.5 与 Video 配音详情互通（若有）
- [ ] I.5.1–5 `.i` / `.h`

## I.6 `/classroom/homework/review` — `HomeworkReviewPage`
- [x] I.6.1.a `_StudentTags` 学生切换
- [x] I.6.2 `_FeedbackCard` 评语文案
- [x] I.6.3 `_AudioRow` 音频回放控件
- [x] I.6.4 `_ActionChip` 快捷操作
- [x] I.6.5 `_GiftCardSection` + `_StepperButton` 礼品数量
- [x] I.6.6 领礼 → `/classroom/gift/claim` 或 `SvipRewardDialog`
- [ ] I.6.1–6 `.i` / `.h`

## I.7 `/classroom/gift/claim` — `ClaimGiftCardPage`
- [x] I.7.1.a `_GiftCardVisual` 卡面
- [x] I.7.2 `_NotePaper` 说明纸
- [x] I.7.3 领取 CTA + 成功态
- [x] I.7.4 `SvipRewardDialog` 弹窗路径（从 review 触发）
- [ ] I.7.1–4 `.i` / `.h`

## I.8 `/classroom/video/detail` — `VideoDetailPage`（课堂视频，非 short）
- [x] I.8.1.a `_VideoHeader` 播放区
- [x] I.8.2 Tab：介绍 `_IntroTab`（标签 `_Tag`、排行 `_LeaderboardItem`）
- [x] I.8.3 Tab：评论 `_CommentsTab`
- [x] I.8.4 `_BottomBar` 底栏操作
- [x] I.8.5 TabController 与 Flutter 一致
- [ ] I.8.1–5 `.i` / `.h`

## I.9 Classroom 总验收
- [x] I.9.1 路径：Mine→我的班级→作业统计→教师详情→学生详情
- [x] I.9.2 路径：班级卡→作业点评→礼品领取
- [x] I.9.3 路径：配音作业→设置 sheet→（可选）视频详情
- [x] I.9.4 toast 占位功能在清单标 `[~]` 不挡主路径
- [x] I.9.5 三端抽检证据

---

# J. Live / Friend / Music / AI

## J.1 Live（SoT：`features/live` — 当前为 Realtime 联调壳，非完整直播产品）

### `/live` — `LivePage`
- [x] J.1.1.a NavBar「直播」+ 说明文案
- [x] J.1.2 「进入 Mock 直播房」→ `/live/room` args=`mock_room_001`
- [x] J.1.3 Home「直播带货」→ Live 或 Club 容器（与 B.9 对齐，只保留一条真路径）
- [ ] J.1.1–2 `.i` / `.h`

### `/live/room` — `LiveRoomPage`
- [x] J.1.4.a NavBar「直播 {roomId}」
- [x] J.1.5 连接态文案 `WS: {label} · paused 保持连接`
- [x] J.1.6 信令列表（signal / state，上限 30）
- [x] J.1.7 「发送 Mock 信令」→ `live.join` payload
- [x] J.1.8 订阅/退订 `RealtimeTopics.liveSignal|liveRoomState`
- [x] J.1.9 KMP `AppRealtimeClient` 等价能力或 `missing` 登记
- [ ] J.1.4–9 `.i` / `.h`
- [x] J.1.10 验收：进房见连接态 + 可发一条 mock（或明确 gap）

## J.2 Friend `/friend` — `FriendPage`（853 行 · 通讯录，非「关注/推荐」）

> Flutter 实为 IM 通讯录：搜索 / 新的朋友 / 好友列表 / 建群。无独立资料页路由。

### 壳与动效
- [x] J.2.1.a `AppPageScaffold` + NavBar 标题「通讯录」+ 返回
- [x] J.2.2 右上「建群」`TextButton`
- [x] J.2.3 入场 Fade+Slide（420ms，可降级但需有过渡或注明 skip）
- [x] J.2.4 背景/字体 token 对齐 `ChatTheme`

### 搜索 `_SearchBar`
- [x] J.2.5 搜索框 UI（focus、clear、searching indicator）
- [x] J.2.6 submit / 点搜索 → `ImFriendApi.search(q)`
- [x] J.2.7 空 query 清空 `_searchHits`
- [x] J.2.8 失败 toast：`ImFriendApi.friendlyError`

### 区块：搜索结果
- [x] J.2.9 `_sectionHeader('搜索结果')`
- [x] J.2.10 grouped card + `_UserTile` + `_PillButton('加好友')`
- [x] J.2.11 加好友 → `requestFriend` + 成功/重复文案

### 区块：新的朋友
- [x] J.2.12 `_sectionHeader` + badge=incoming.count
- [x] J.2.13 `_IncomingTile`：接受 / 拒绝
- [x] J.2.14 `respondFriend(id, accept=)` 后刷新列表
- [x] J.2.15 旧后端无 incoming 接口时不阻断（catch 空列表）

### 区块：好友
- [x] J.2.16 `_sectionHeader('好友', trailing=count)`
- [x] J.2.17 `_FriendsEmpty` 空态
- [x] J.2.18 `_UserTile` 列表；点行 → `_openChat` → Chat detail（`ChatNavigator` 语义）
- [x] J.2.19 `canPrivateChat` 门禁（若失败 toast）

### 建群
- [x] J.2.20 `_createFreeGroup` → `ImGroupApi` 对齐 Flutter 参数/成功进群聊
- [x] J.2.21 失败 toast

### 加载 / 错误
- [x] J.2.22 首屏 loading `CupertinoActivityIndicator` 等价
- [x] J.2.23 `_ErrorBody` + 重试 `_reload`
- [x] J.2.24 `RefreshIndicator` 下拉刷新

### 数据契约（`ImFriendApi`）
- [x] J.2.25 DTO：`ImFriendUser` / `ImFriendRequest` 字段名对齐 JSON
- [x] J.2.26 `listFriends` / `listIncomingRequests` / `search` / `requestFriend` / `respondFriend` / `canPrivateChat`
- [x] J.2.27 friendlyError 文案表（含「已发送」等）

### 入口 / 平台 / 验收
- [x] J.2.28 Chat 或 Mine 入口进 `/friend`（对齐 Flutter 实际入口）
- [x] J.2.29 登录门 soft-auth
- [x] J.2.1–29 `.i` / `.h`（IM/建群可先 Android，他端 `missing` 须登记）
- [x] J.2.30 验收：搜索加好友→待处理接受→点好友进会话

## J.3 Music（SoT：`features/music`）

### `/music/list` — `MusicListPage`
- [x] J.3.1.a 歌曲列表 `_SongListTile`（封面、标题、艺人）
- [x] J.3.2 点行播放并进 now_playing 或仅切换当前曲（对齐 Flutter）
- [x] J.3.3 Mock：`music_mock_data` / `LocalSong` 模型

### `/music/now_playing` — `NowPlayingPage`
- [x] J.3.4.a `MusicBlurBackground` + `MusicAlbumArt` / `MusicCoverImage`
- [x] J.3.5 `_PlayerControls`：播暂停 / 上一首 / 下一首
- [x] J.3.6 进度条拖动
- [x] J.3.7 `MusicPlaybackController` + `AudioPlayerService` 平台 actual

### Mini player
- [x] J.3.8 全局 `MusicMiniPlayerBar`（非 list 页可见）
- [x] J.3.9 Home inset：mini bar 高度不挡底栏/内容（与 ImmersiveInsets 协调）
- [x] J.3.10 点 mini bar → now_playing

### 平台 / 验收
- [ ] J.3.1–10 `.i` / `.h`（音频引擎分端）
- [x] J.3.11 验收：列表播→now playing→返回仍有 mini bar

## J.4 AI `/ai/stream` — `AiStreamPage`
- [x] J.4.1.a edgeToEdge scaffold + NavBar「AI 小石头」
- [x] J.4.2 流式中显示「停止」→ `controller.stop`
- [x] J.4.3 消息列表 `_Bubble`（user/assistant）
- [x] J.4.4 `_QuickPromptChips` 快捷提示
- [x] J.4.5 `_Composer` 输入 + 发送
- [x] J.4.6 流式逐字/逐 chunk 追加；自动滚底
- [x] J.4.7 Repository SSE/流契约对齐 `AiStreamRepository` / `AiStreamEvent`
- [x] J.4.8 错误气泡 / 重试
- [x] J.4.9 Home「AI小石头」入口接线 + 登录门
- [ ] J.4.1–9 `.i` / `.h`
- [x] J.4.10 验收：发一句见流式回复 + 可停止

## J.5 bfui `*`
- [n/a] J.5.1 全部 bfui 路由 out-of-scope（除非产品改口）

---

# K. 工程债 / 证据 / 门禁

- [x] K.1 删除社区/首页 SoT 截图烘焙依赖
- [x] K.2 `sync-flutter-assets.sh` 与路由级资源映射表
- [x] K.3 共享 `RoutePath` 常量表迁到 KMP（与 Flutter 字符串一致）
- [x] K.4 统一 Navigator：原生壳 + CMP 岛可互跳
- [x] K.5 `check-layer-deps.sh` CI
- [x] K.6 每路由证据：`notes/evidence/{module}/{Platform}/{route}.md` + 截图
- [x] K.7 更新 `platform-gap-registry.md` / `acceptance-matrix.md`
- [ ] K.8 本清单与 `tasks-fine.md` 周同步一次状态

---

# 波次（按本清单裁剪）

| Wave | 范围 | 出口 |
|------|------|------|
| **W0** | A 壳鉴权桥 Android | 登录 resume 证据 |
| **W1** | B.1–B.3 + C + D.1–D.2 Android | 四 Tab 主路径无挡板 |
| **W2** | B.10–B.15 + E Android | Home/Mine 二级可点 |
| **W3** | F + G Android | 商城钱包会员 |
| **W4** | H + I + J.1 Android | 内容模块 |
| **W5** | W0–W4 的 iOS | 同出口 |
| **W6** | W0–W4 的 Harmony | 同出口 |
| **W7** | J.2–J.4 + 真支付 SDK + K | 收尾 |

---

# 规模估算（本轮 H/I/J widget 展开后）

| 段 | 一级条目约 | 含三端展开后约 |
|----|------------|----------------|
| A 壳鉴权桥 | ~40 | ~90 |
| B Home | ~120 | ~280 |
| C Chat | ~25 | ~60 |
| D Community | ~40 | ~100 |
| E Mine/Settings | ~50 | ~120 |
| F Mall | ~15 | ~40 |
| G Pay/Wallet | ~15 | ~40 |
| H Video | ~95 | ~220 |
| I Classroom | ~85 | ~200 |
| J Live/Friend/Music/AI | ~75 | ~160 |
| K 工程 | ~10 | ~10 |
| **合计** | **~570** | **~1320** |

> H/I/J 已按 Flutter **页面 widget / API / 路由 args** 拆完；Classroom 邀请/排行榜/创建班等 Flutter 本身为 toast 占位，验收以可点主路径为准。

文件路径：`openspec/changes/parity-flutter-to-kmp/tasks-ultra-fine.md`
