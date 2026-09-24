# Flutter → KMP 细粒度任务拆分（按模块）

> **更细一版（按 `RoutePath` / UI 区块 / `.a.i.h` 三端）→ [`tasks-ultra-fine.md`](./tasks-ultra-fine.md)**（约 570 一级项，三端展开约 1320；H/I/J 已按 widget/API 展开）。

> SoT：`my_ai_project` · 目标：`my_kmp_project` · ADR 0002  
> **完成定义**：可点击路径 + 数据契约对齐；MSE 仅作回归信号，不算模块完成。  
> 平台默认顺序：**Android → iOS → Harmony**（未写平台时 = 三端都要，可先 Android）。  
> 状态：`[ ]` 未做 · `[~]` 部分 · `[x]` 可验收

**优先级**：P0 主 Tab 可点闭环 · P1 主 Tab 二级全通 · P2 周边模块 · P3 三端/支付 SDK

---

## 0. 壳与横切（Shell / Auth / Bridges）

### 0.1 Shell chrome
- [~] 0.1.1 Android：Splash → Privacy → 四 Tab 壳可进（已有）
- [ ] 0.1.2 iOS：Splash → Privacy → 四 Tab 壳可进 + 证据截图
- [ ] 0.1.3 Harmony：同上
- [ ] 0.1.4 三端底栏高度/选中态/图标与 Flutter `MainBottomBar` 对齐（非仅 Android）
- [ ] 0.1.5 隐私同意持久化：冷启动不再重复弹（验三端）

### 0.2 Auth / Session
- [~] 0.2.1 Android：密码登录 happy path（已有部分）
- [ ] 0.2.2 Android：注册 / OTP 与 Flutter 同错误文案
- [ ] 0.2.3 Android：401 → 清会话 → soft-auth 回登录
- [ ] 0.2.4 Android：登录后 resume pending Tab（Chat/Community/Mine）
- [ ] 0.2.5 iOS：同上 0.2.1–0.2.4
- [ ] 0.2.6 Harmony：同上
- [ ] 0.2.7 微信登录：Android/iOS/OHOS 各标 `done|missing`（缺则 registry）

### 0.3 Bridges
- [~] 0.3.1 WebView 打开/返回（Android 有离线 fixture；补 Flutter URL 契约）
- [ ] 0.3.2 扫码权限拒绝 UX（三端）
- [ ] 0.3.3 Deeplink 冷启动进路由（三端）
- [ ] 0.3.4 Push 注册 + payload→route（或 registry `missing`）

---

## 1. Home（Flutter 22 pages）— P0/P1

### 1.1 根页 `home_page`
- [~] 1.1.1 问候 / 搜索 / 扫一扫入口布局
- [~] 1.1.2 Banner 点击可导航（现多为 stub）
- [~] 1.1.3 Feature 九宫格：图标/标签与 Flutter catalog 一致
- [ ] 1.1.4 Feature 九宫格：每一格进真实二级（禁止一律 DeferredStub）
- [ ] 1.1.5 「公司数据」指标：接 Flutter 同 API；失败降级与 Flutter 一致（空/占位）
- [ ] 1.1.6 今日/昨日/近30天切换驱动真实请求
- [ ] 1.1.7 待办条：API 成功展示 / 失败隐藏（对齐 Flutter）
- [ ] 1.1.8 消息 pill → 消息/通知页（非 stub）

### 1.2 全部服务 `all_services_page`
- [x] 1.2.1 Android：从「更多」打开 `AllServicesScreen`
- [ ] 1.2.2 分区列表与 Flutter catalog 一一对应（线索/营销/教学/其他…）
- [ ] 1.2.3 编辑常用服务：增删 ≥3 下限、写回首页顺序
- [ ] 1.2.4 每一项 tap → 对应二级（不是 snackbar）
- [ ] 1.2.5 iOS / Harmony 同源页

### 1.3 搜索 `search_page`
- [ ] 1.3.1 入口从首页搜索框进入
- [ ] 1.3.2 历史/热词/结果列表（对齐 Flutter）
- [ ] 1.3.3 空态/错误态
- [ ] 1.3.4 iOS / Harmony

### 1.4 二手车 `used_car_*`
- [ ] 1.4.1 列表页 `used_car_list_page`
- [ ] 1.4.2 详情页 `used_car_detail_page`
- [ ] 1.4.3 创建页 `used_car_create_page`
- [ ] 1.4.4 首页「二手车」入口接线
- [ ] 1.4.5 iOS / Harmony

### 1.5 新车跟进 `new_car_follow_*`
- [ ] 1.5.1 列表
- [ ] 1.5.2 详情
- [ ] 1.5.3 创建
- [ ] 1.5.4 首页「新车跟进」入口接线
- [ ] 1.5.5 iOS / Harmony

### 1.6 分析 `analytics_*`
- [ ] 1.6.1 列表 `analytics_list_page`
- [ ] 1.6.2 详情 `analytics_detail_page`
- [ ] 1.6.3 首页/九宫格入口接线
- [ ] 1.6.4 iOS / Harmony

### 1.7 台账 `ledger_*`
- [ ] 1.7.1 列表
- [ ] 1.7.2 详情
- [ ] 1.7.3 入口接线
- [ ] 1.7.4 iOS / Harmony

### 1.8 售后 `after_sales_*`
- [ ] 1.8.1 列表
- [ ] 1.8.2 详情
- [ ] 1.8.3 创建
- [ ] 1.8.4 入口接线
- [ ] 1.8.5 iOS / Harmony

### 1.9 其它 Home 二级
- [ ] 1.9.1 `strategy_page` 投资策略
- [ ] 1.9.2 `home_learning_report_page` 学习报告
- [ ] 1.9.3 `home_feature_content_page` 通用内容容器
- [ ] 1.9.4 `hot_rank_detail_page` 热榜详情
- [ ] 1.9.5 `check_in_mall_page` 签到商城
- [ ] 1.9.6 `dubbing_home_page`（若仍挂 Home）入口或迁 video
- [ ] 1.9.7 以上 iOS / Harmony

### 1.10 Home 验收
- [ ] 1.10.1 Android：根页 + 全部服务 + 二手车 + 新车跟进 可点闭环证据
- [ ] 1.10.2 Android：指标/待办成功与失败各一套证据
- [ ] 1.10.3 iOS / Harmony 同 1.10.1

---

## 2. Chat（Flutter 3 pages）— P0

### 2.1 列表 `chat_page`
- [~] 2.1.1 Android：会话列表 UI（Mock 好友）
- [ ] 2.1.2 列表数据源对齐 Flutter MockIm / 真 IM 开关
- [ ] 2.1.3 搜索会话（现仅 UI 开关）
- [ ] 2.1.4 未读角标 / 在线点与 Flutter 一致
- [ ] 2.1.5 soft-auth：未登录进 Chat → 登录 → resume
- [ ] 2.1.6 iOS / Harmony

### 2.2 详情 `chat_detail_page`
- [~] 2.2.1 Android：从列表进入详情 + 本地发送气泡（已有）
- [ ] 2.2.2 历史消息分页/时间分隔
- [ ] 2.2.3 发送失败重试 / 发送中态
- [ ] 2.2.4 图片消息 + 预览（见 2.3）
- [ ] 2.2.5 语音入口（Flutter 有则对齐，否则 registry）
- [ ] 2.2.6 iOS / Harmony

### 2.3 图片预览 `image_preview_page`（Chat）
- [ ] 2.3.1 Chat 内图片点击预览
- [ ] 2.3.2 左右滑动多图
- [ ] 2.3.3 iOS / Harmony

### 2.4 IM 引擎
- [ ] 2.4.1 抽象 `ImGateway`（已有则核对）与 Flutter 融云开关一致
- [ ] 2.4.2 Mock 模式可验收 UI；真 SDK 按端标 `partial|missing`
- [ ] 2.4.3 Android 验收：登录→列表→详情→发送（证据）
- [ ] 2.4.4 iOS / Harmony 验收

---

## 3. Community（Flutter 7 pages）— P0

### 3.1 信息流 `community_page`
- [~] 3.1.1 Android：根页头/搜索/最新·热门·关注 Tab UI
- [~] 3.1.2 Android：帖卡片富文本（@/#/链接）
- [x] 3.1.3 Android：图片 **3×9 ImageGrid**（对齐 Flutter `ImageGridWidget`）
- [~] 3.1.4 Android：视频封面 16:9 + 播放钮（现 picsum）
- [ ] 3.1.5 数据：MockPost `Random(42)` 全量列表（不止 2 帖）或切 HttpPost
- [ ] 3.1.6 Tab 切换：最新 / 热门 / 关注 排序与 Flutter 一致
- [ ] 3.1.7 点赞/评论数本地乐观更新
- [ ] 3.1.8 空态 / 加载 / 错误
- [ ] 3.1.9 soft-auth gate
- [ ] 3.1.10 iOS / Harmony

### 3.2 发布 `publish_page`
- [ ] 3.2.1 「+」进入发布页（现空）
- [ ] 3.2.2 文案 / 选图 / 选话题 / 校验
- [ ] 3.2.3 发布成功回列表置顶
- [ ] 3.2.4 iOS / Harmony

### 3.3 搜索 `community_search_page`
- [ ] 3.3.1 搜索框进入
- [ ] 3.3.2 动态/话题/用户结果分区
- [ ] 3.3.3 iOS / Harmony

### 3.4 预览 / 播放
- [ ] 3.4.1 `image_preview_page`（社区九宫格点击）
- [ ] 3.4.2 `video_play_page`
- [ ] 3.4.3 iOS / Harmony

### 3.5 其它
- [ ] 3.5.1 `topic_select_page`
- [ ] 3.5.2 `community_convention_page` / 公约弹窗
- [ ] 3.5.3 Android 验收：浏览→预览→发布→回列表
- [ ] 3.5.4 iOS / Harmony 验收

---

## 4. Mine / Settings（Flutter settings 11 pages）— P0/P1

### 4.1 根页 `mine_page`
- [~] 4.1.1 Android：头/资料卡/统计/常用服务/个人功能/菜单（CMP）
- [x] 4.1.2 「商城」→ Membership（临时映射，最终应进 Mall）
- [ ] 4.1.3 「我的钱包」→ Wallet 页
- [ ] 4.1.4 「我的课程」→ Classroom/课程入口
- [ ] 4.1.5 「我的订单」→ Mall orders
- [ ] 4.1.6 个人功能：短信模板 / 购车计算器 / 二手车 / 小视频 → 真页
- [ ] 4.1.7 签到日历入口
- [ ] 4.1.8 电子名片入口
- [ ] 4.1.9 iOS / Harmony 根页

### 4.2 设置岛
- [~] 4.2.1 Settings / Personalized / About（已有岛）
- [ ] 4.2.2 `mine_profile_page` 资料编辑
- [ ] 4.2.3 `settings_page` 与 Flutter 分组一致
- [ ] 4.2.4 `personalized_settings_page` 开关持久化
- [ ] 4.2.5 iOS / Harmony

### 4.3 地址 / 发票 / 计算器
- [ ] 4.3.1 `address_list_page` + `address_edit_page`
- [ ] 4.3.2 `purchase_calculator_page`
- [ ] 4.3.3 发票相关：产品要则做，否则 registry `n/a-out-of-scope`
- [ ] 4.3.4 iOS / Harmony

### 4.4 调试页（默认可 out-of-scope）
- [ ] 4.4.1 `mine_http_test_page` / `dialog_demo_page` → registry 标 n/a 或隔离 Debug 入口

### 4.5 Mine 验收
- [ ] 4.5.1 Android：根→设置→个性化→返回；根→钱包/订单/计算器各一证
- [ ] 4.5.2 iOS / Harmony

---

## 5. Mall（Flutter 4 pages）— P1

- [ ] 5.1.1 `mall_page` 列表/货架
- [ ] 5.1.2 `mall_detail_page`
- [ ] 5.1.3 `mall_orders_page`
- [ ] 5.1.4 `mall_order_detail_page`
- [ ] 5.1.5 Mine「商城」改挂 Mall（不再只进 Membership）
- [ ] 5.1.6 iOS / Harmony
- [ ] 5.1.7 Android 验收：进商城→详情→订单列表

---

## 6. Pay / Wallet / Membership — P1/P2

### 6.1 Pay
- [~] 6.1.1 Membership UI（Android 已有 `MembershipScreen`）
- [ ] 6.1.2 `pay_page` 收银台对齐 Flutter
- [ ] 6.1.3 `membership_renew_page`
- [ ] 6.1.4 微信 Pay adapter（或 registry missing）
- [ ] 6.1.5 支付宝 adapter（或 registry missing）
- [ ] 6.1.6 成功/取消/失败/不可用文案
- [ ] 6.1.7 iOS / Harmony

### 6.2 Wallet
- [ ] 6.2.1 `wallet_page`
- [ ] 6.2.2 Mine「我的钱包」接线
- [ ] 6.2.3 iOS / Harmony

---

## 7. Video（Flutter 9 pages）— P2

- [ ] 7.1.1 `video_page` / hub
- [ ] 7.1.2 `short_video_page` + `short_video_play_page`
- [ ] 7.1.3 `short_video_publish_page` + `short_video_help_page`
- [ ] 7.1.4 配音：`dubbing_*` 列表/详情/作品（4 页）
- [ ] 7.1.5 Home/Mine「小视频」入口接线
- [ ] 7.1.6 播放器：Android/iOS/OHOS 真 adapter（禁 Stub 验收）
- [ ] 7.1.7 iOS / Harmony 验收

---

## 8. Classroom（Flutter 8 pages）— P2

- [ ] 8.1.1 `my_class_list_page`
- [ ] 8.1.2 作业：学生/教师详情、批改、统计、配音作业
- [ ] 8.1.3 `claim_gift_card_page`
- [ ] 8.1.4 `video_detail_page`（课堂）
- [ ] 8.1.5 Mine「我的课程」接线
- [ ] 8.1.6 iOS / Harmony

---

## 9. Live / Friend / Music / AI — P2/P3

### 9.1 Live（2 pages）
- [ ] 9.1.1 直播列表/间页对齐 Flutter
- [ ] 9.1.2 入口接线（Home 直播带货等）
- [ ] 9.1.3 iOS / Harmony

### 9.2 Friend（1 page）
- [ ] 9.2.1 好友页
- [ ] 9.2.2 入口接线
- [ ] 9.2.3 iOS / Harmony

### 9.3 Music（2 pages）
- [ ] 9.3.1 音乐页 + Now Playing / mini-player
- [ ] 9.3.2 iOS / Harmony

### 9.4 AI（1 page）
- [ ] 9.4.1 AI 页 / Home「AI小石头」接线
- [ ] 9.4.2 iOS / Harmony

### 9.5 bfui
- [ ] 9.5.1 registry 标 `n/a-out-of-scope`（除非产品要）

---

## 10. 资源与工程债 — 持续

- [ ] 10.1 去掉社区/首页 **SoT 截图烘焙** 资源，改 catalog/网络图
- [ ] 10.2 `sync-flutter-assets.sh` 与业务 drawable 映射表维护
- [ ] 10.3 `scripts/check-layer-deps.sh` 合入前必过
- [ ] 10.4 每个模块证据目录：`notes/evidence/<module>/<Platform>/`
- [ ] 10.5 更新 `platform-gap-registry.md` / `acceptance-matrix.md`（与本清单同步）

---

## 建议执行波次（仍细，可排期）

| Wave | 模块切片 | 出口标准 |
|------|----------|----------|
| **W1** | Home 1.1–1.2 + Chat 2.x Android + Community 3.1–3.2 Android | 四 Tab 主路径可点，无「开发中」挡主流程 |
| **W2** | Home 1.3–1.8 Android + Mine 4.1–4.3 Android | Home/Mine 二级清单页齐 |
| **W3** | Mall+Pay+Wallet Android | 商城/会员/钱包可逛可下单（SDK 可 missing） |
| **W4** | Video+Classroom+Live Android | 内容模块可进可播 |
| **W5** | W1–W4 的 iOS | 同 Android 出口 |
| **W6** | W1–W4 的 Harmony | 同 Android 出口 |
| **W7** | Friend/Music/AI + 支付真 SDK | 收尾与 registry 清零 |

---

## 统计（约）

| 模块 | 细项约数 | 当前粗进度 |
|------|----------|------------|
| 0 壳/鉴权/桥 | ~20 | ~30% |
| 1 Home | ~45 | ~15% |
| 2 Chat | ~18 | ~25% |
| 3 Community | ~25 | ~25% |
| 4 Mine/Settings | ~25 | ~25% |
| 5 Mall | ~7 | ~5% |
| 6 Pay/Wallet | ~10 | ~20% |
| 7 Video | ~7 | ~5% |
| 8 Classroom | ~6 | ~5% |
| 9 其它 | ~12 | ~5% |
| 10 工程债 | ~5 | — |
| **合计** | **~180** | **整体 ~20%** |
