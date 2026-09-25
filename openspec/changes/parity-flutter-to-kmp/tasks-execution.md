# Flutter → KMP 全量执行清单（细粒度 · 含收口回检）

> **SoT**：`/Users/mac/Desktop/github/my_ai_project`  
> **路由权威**：`commons/wys_router/lib/src/route/route_path.dart`（共 103 条）  
> **目标**：`my_kmp_project` · ADR 0002 · 业务一致 + UI 像素对齐 Flutter（禁止臆造 stub 冒充完成）  
> **关联**：粗粒度 [`tasks.md`](./tasks.md) · 旧细粒度 [`tasks-fine.md`](./tasks-fine.md) · 旧超细 [`tasks-ultra-fine.md`](./tasks-ultra-fine.md)  
> **本文为执行权威**：实现与勾选以本文为准；发现与 Flutter 不符必须改回 `[ ]`。

## 状态约定

| 标记 | 含义 |
|------|------|
| `[ ]` | 未做 / 未验收 |
| `[~]` | 部分（有壳或 mock，**未**达 Flutter 同款） |
| `[x]` | 已对照 Flutter 验收通过（有证据路径） |
| `[n/a]` | 明确不做（out-of-scope / registry） |
| `.a` `.i` `.h` | Android / iOS / Harmony |

## 完成定义（每条路由必须同时满足）

对 **in-scope** 路由，下列子项全部 `[x]` 才算该路由完成：

1. **UI**：与 Flutter 同页并排截图；布局/文案/主色/空态对齐（允许平台系统控件差异）
2. **入口**：Flutter 所有可达入口在 KMP 均可进入（Tab / 九宫格 / 全部服务 / 深链）
3. **导航**：进/出/返回/参数与 `RoutePath` 一致
4. **业务**：校验、加载/失败/空态、主操作与 Flutter 同行为（真 API 优先；mock 仅当 Flutter 同页也是 mock，且字段一致）
5. **反 stub**：不得用 `SimpleDetail` / `DeferredStub` / 单行文案页冒充；缺口必须写 `platform-gap-registry`
6. **证据**：`notes/evidence/{module}/Android|{iOS}|{OHOS}/{route-slug}.{flutter|kmp}.png` + 一行结论
7. **平台**：至少 `.a` 完成；`.i`/`.h` 未做则登记 `missing`，**不得**标成整路由 `[x]`

## 诚实重置（2026-09-25）

此前若干二级页以 mock 壳交付并误标完成。**下列一律视为未完成**，须按 Flutter 重做：

- Mine：`/settings`（Flutter 为环境/主题/语言等，非「消息通知」双开关页）
- Mine：商务合作 / 提醒 / 邀请 / 粉丝群 / 意见反馈 — 须对照 Flutter 真页，不得自造
- Home：生活服务 / Club / 直播带货 — 须对照 Flutter `*_page.dart`，不得自造 hero 列表
- 任何仅 `SimpleDetail` / 单行 body 的路由
- 未与 Flutter 并排截图的「证据」一律作废重拍

## 建议波次（执行顺序）

| Wave | 范围 | 出口门禁 |
|------|------|----------|
| **W0** | 0.x 横切 + `/` `/main` 鉴权登录 | Flutter/KMP 同模拟器可登录；审计脚本可跑 |
| **W1** | Home 根 + 全部服务 + 搜索 | 九宫格/更多无挡板 |
| **W2** | Home 全部二级（B 段路由） | 列表→详情可点且对 Flutter |
| **W3** | Chat + Community | 发送/发布主路径 |
| **W4** | Mine + Settings + Mall | **真**设置页；地址/资料 |
| **W5** | Pay/Wallet + Media + Classroom + Live/Friend/AI | 主路径；支付不可伪造成功 |
| **W6** | iOS / Harmony 补齐或 registry `missing` | Z.3.4 |
| **W7** | **Z 全部门禁** | 全绿才允许写结案报告 |

---

# 0. 横切（全模块共用）

- [x] 0.1 拉起 Flutter SoT 于同一模拟器：`com.sample.module_sample`（可跑、可登录）
- [x] 0.2 固定测试账号流程文档化：`13400000000` / OTP `123456` 或密码通道；Go `8080` 健康检查
- [x] 0.3 KMP 与 Flutter 同账号同后端；禁止 SoftAuth 假登录冒充业务完成
- [x] 0.4 `RoutePath` 字符串表与 Flutter 103 条对齐（含别名）
- [ ] 0.5 Design tokens / 沉浸式顶栏 / 底栏 49dp 与 Flutter 对齐
- [x] 0.6 soft-auth：未登录进门禁 Tab → 登录 → resume
- [ ] 0.7 401 清会话
- [ ] 0.8 `platform-gap-registry.md` 全能力有状态
- [ ] 0.9 `acceptance-matrix.md` 与本清单同步
- [ ] 0.10 CI：`check-layer-deps.sh` + Android compile/test

# A. 壳 / 鉴权 / 桥接

## `/` — `splash`
- [x] R.splash.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.splash.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.splash.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.splash.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.splash.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.splash.ev.a 证据 `notes/evidence/.../root.{flutter,kmp}.png`
- [ ] R.splash.ui.i / R.splash.ui.h（或 registry `missing`）

## `/main` — `main`
- [x] R.main.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.main.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.main.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.main.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.main.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.main.ev.a 证据 `notes/evidence/.../main.{flutter,kmp}.png`
- [ ] R.main.ui.i / R.main.ui.h（或 registry `missing`）

## `/login` — `login`
- [x] R.login.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.login.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.login.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.login.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.login.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.login.ev.a 证据 `notes/evidence/.../login.{flutter,kmp}.png`
- [ ] R.login.ui.i / R.login.ui.h（或 registry `missing`）

## `/login/password` — `loginPassword`
- [ ] R.loginPassword.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.loginPassword.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.loginPassword.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.loginPassword.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.loginPassword.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.loginPassword.ev.a 证据 `notes/evidence/.../login_password.{flutter,kmp}.png`
- [ ] R.loginPassword.ui.i / R.loginPassword.ui.h（或 registry `missing`）

## `/login/otp` — `loginOtp`
- [ ] R.loginOtp.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.loginOtp.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.loginOtp.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.loginOtp.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.loginOtp.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.loginOtp.ev.a 证据 `notes/evidence/.../login_otp.{flutter,kmp}.png`
- [ ] R.loginOtp.ui.i / R.loginOtp.ui.h（或 registry `missing`）

## `/register` — `register`
- [ ] R.register.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.register.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.register.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.register.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.register.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.register.ev.a 证据 `notes/evidence/.../register.{flutter,kmp}.png`
- [ ] R.register.ui.i / R.register.ui.h（或 registry `missing`）

## `/web` — `web`
- [ ] R.web.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.web.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.web.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.web.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.web.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.web.ev.a 证据 `notes/evidence/.../web.{flutter,kmp}.png`
- [ ] R.web.ui.i / R.web.ui.h（或 registry `missing`）

# B. Home

## `/home` — `home`
- [x] R.home.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.home.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.home.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.home.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.home.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.home.ev.a 证据 `notes/evidence/.../home.{flutter,kmp}.png`
- [ ] R.home.ui.i / R.home.ui.h（或 registry `missing`）

## `/home/learning_report` — `homeLearningReport`
- [x] R.homeLearningReport.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeLearningReport.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeLearningReport.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeLearningReport.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeLearningReport.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeLearningReport.ev.a 证据 `notes/evidence/.../home_learning_report.{flutter,kmp}.png`
- [ ] R.homeLearningReport.ui.i / R.homeLearningReport.ui.h（或 registry `missing`）

## `/home/check_in_mall` — `homeCheckInMall`
- [x] R.homeCheckInMall.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeCheckInMall.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeCheckInMall.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeCheckInMall.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeCheckInMall.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeCheckInMall.ev.a 证据 `notes/evidence/.../home_check_in_mall.{flutter,kmp}.png`
- [ ] R.homeCheckInMall.ui.i / R.homeCheckInMall.ui.h（或 registry `missing`）

## `/home/all_services` — `homeAllServices`
- [x] R.homeAllServices.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeAllServices.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeAllServices.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeAllServices.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeAllServices.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeAllServices.ev.a 证据 `notes/evidence/.../home_all_services.{flutter,kmp}.png`
- [ ] R.homeAllServices.ui.i / R.homeAllServices.ui.h（或 registry `missing`）

## `/home/search` — `homeSearch`
- [x] R.homeSearch.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeSearch.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeSearch.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeSearch.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeSearch.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeSearch.ev.a 证据 `notes/evidence/.../home_search.{flutter,kmp}.png`
- [ ] R.homeSearch.ui.i / R.homeSearch.ui.h（或 registry `missing`）

## `/home/dubbing_feed` — `homeDubbingFeed`
- [x] R.homeDubbingFeed.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeDubbingFeed.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeDubbingFeed.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeDubbingFeed.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeDubbingFeed.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeDubbingFeed.ev.a 证据 `notes/evidence/.../home_dubbing_feed.{flutter,kmp}.png`
- [ ] R.homeDubbingFeed.ui.i / R.homeDubbingFeed.ui.h（或 registry `missing`）

## `/home/strategy` — `homeStrategy`
- [x] R.homeStrategy.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeStrategy.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeStrategy.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeStrategy.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeStrategy.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeStrategy.ev.a 证据 `notes/evidence/.../home_strategy.{flutter,kmp}.png`
- [ ] R.homeStrategy.ui.i / R.homeStrategy.ui.h（或 registry `missing`）

## `/home/hot_rank_detail` — `homeHotRankDetail`
- [x] R.homeHotRankDetail.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeHotRankDetail.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeHotRankDetail.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeHotRankDetail.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeHotRankDetail.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeHotRankDetail.ev.a 证据 `notes/evidence/.../home_hot_rank_detail.{flutter,kmp}.png`
- [ ] R.homeHotRankDetail.ui.i / R.homeHotRankDetail.ui.h（或 registry `missing`）

## `/home/used_car` — `homeUsedCarList`
- [x] R.homeUsedCarList.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeUsedCarList.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeUsedCarList.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeUsedCarList.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeUsedCarList.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeUsedCarList.ev.a 证据 `notes/evidence/.../home_used_car.{flutter,kmp}.png`
- [ ] R.homeUsedCarList.ui.i / R.homeUsedCarList.ui.h（或 registry `missing`）

## `/home/used_car/detail` — `homeUsedCarDetail`
- [x] R.homeUsedCarDetail.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeUsedCarDetail.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeUsedCarDetail.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeUsedCarDetail.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeUsedCarDetail.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeUsedCarDetail.ev.a 证据 `notes/evidence/.../home_used_car_detail.{flutter,kmp}.png`
- [ ] R.homeUsedCarDetail.ui.i / R.homeUsedCarDetail.ui.h（或 registry `missing`）

## `/home/used_car/create` — `homeUsedCarCreate`
- [x] R.homeUsedCarCreate.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeUsedCarCreate.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeUsedCarCreate.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeUsedCarCreate.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeUsedCarCreate.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeUsedCarCreate.ev.a 证据 `notes/evidence/.../home_used_car_create.{flutter,kmp}.png`
- [ ] R.homeUsedCarCreate.ui.i / R.homeUsedCarCreate.ui.h（或 registry `missing`）

## `/home/ledger` — `homeLedgerList`
- [ ] R.homeLedgerList.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.homeLedgerList.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.homeLedgerList.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.homeLedgerList.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.homeLedgerList.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.homeLedgerList.ev.a 证据 `notes/evidence/.../home_ledger.{flutter,kmp}.png`
- [ ] R.homeLedgerList.ui.i / R.homeLedgerList.ui.h（或 registry `missing`）

## `/home/ledger/detail` — `homeLedgerDetail`
- [ ] R.homeLedgerDetail.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.homeLedgerDetail.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.homeLedgerDetail.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.homeLedgerDetail.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.homeLedgerDetail.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.homeLedgerDetail.ev.a 证据 `notes/evidence/.../home_ledger_detail.{flutter,kmp}.png`
- [ ] R.homeLedgerDetail.ui.i / R.homeLedgerDetail.ui.h（或 registry `missing`）

## `/home/data_analytics` — `homeDataAnalyticsList`
- [x] R.homeDataAnalyticsList.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeDataAnalyticsList.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeDataAnalyticsList.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeDataAnalyticsList.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeDataAnalyticsList.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeDataAnalyticsList.ev.a 证据 `notes/evidence/.../home_data_analytics.{flutter,kmp}.png`
- [ ] R.homeDataAnalyticsList.ui.i / R.homeDataAnalyticsList.ui.h（或 registry `missing`）

## `/home/data_analytics/detail` — `homeDataAnalyticsDetail`
- [ ] R.homeDataAnalyticsDetail.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.homeDataAnalyticsDetail.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.homeDataAnalyticsDetail.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.homeDataAnalyticsDetail.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.homeDataAnalyticsDetail.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.homeDataAnalyticsDetail.ev.a 证据 `notes/evidence/.../home_data_analytics_detail.{flutter,kmp}.png`
- [ ] R.homeDataAnalyticsDetail.ui.i / R.homeDataAnalyticsDetail.ui.h（或 registry `missing`）

## `/home/life_service` — `homeLifeService`
- [x] R.homeLifeService.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeLifeService.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeLifeService.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeLifeService.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeLifeService.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeLifeService.ev.a 证据 `notes/evidence/.../home_life_service.{flutter,kmp}.png`
- [ ] R.homeLifeService.ui.i / R.homeLifeService.ui.h（或 registry `missing`）

## `/home/live_commerce` — `homeLiveCommerce`
- [x] R.homeLiveCommerce.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeLiveCommerce.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeLiveCommerce.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeLiveCommerce.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeLiveCommerce.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeLiveCommerce.ev.a 证据 `notes/evidence/.../home_live_commerce.{flutter,kmp}.png`
- [ ] R.homeLiveCommerce.ui.i / R.homeLiveCommerce.ui.h（或 registry `missing`）

## `/home/club` — `homeClub`
- [x] R.homeClub.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeClub.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeClub.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeClub.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeClub.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeClub.ev.a 证据 `notes/evidence/.../home_club.{flutter,kmp}.png`
- [ ] R.homeClub.ui.i / R.homeClub.ui.h（或 registry `missing`）

## `/home/todo/partner-pending` — `homeTodoPartnerPending`
- [x] R.homeTodoPartnerPending.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeTodoPartnerPending.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeTodoPartnerPending.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeTodoPartnerPending.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeTodoPartnerPending.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeTodoPartnerPending.ev.a 证据 `notes/evidence/.../home_todo_partner-pending.{flutter,kmp}.png`
- [ ] R.homeTodoPartnerPending.ui.i / R.homeTodoPartnerPending.ui.h（或 registry `missing`）

## `/home/todo/follow-up-customers` — `homeTodoFollowUp`
- [x] R.homeTodoFollowUp.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeTodoFollowUp.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeTodoFollowUp.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeTodoFollowUp.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeTodoFollowUp.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeTodoFollowUp.ev.a 证据 `notes/evidence/.../home_todo_follow-up-customers.{flutter,kmp}.png`
- [ ] R.homeTodoFollowUp.ui.i / R.homeTodoFollowUp.ui.h（或 registry `missing`）

## `/home/todo/after-sales-appointments` — `homeTodoAfterSales`
- [x] R.homeTodoAfterSales.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeTodoAfterSales.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeTodoAfterSales.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeTodoAfterSales.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeTodoAfterSales.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeTodoAfterSales.ev.a 证据 `notes/evidence/.../home_todo_after-sales-appointments.{flutter,kmp}.png`
- [ ] R.homeTodoAfterSales.ui.i / R.homeTodoAfterSales.ui.h（或 registry `missing`）

## `/home/todo/order-pending-review` — `homeTodoOrderReview`
- [x] R.homeTodoOrderReview.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeTodoOrderReview.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeTodoOrderReview.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeTodoOrderReview.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeTodoOrderReview.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeTodoOrderReview.ev.a 证据 `notes/evidence/.../home_todo_order-pending-review.{flutter,kmp}.png`
- [ ] R.homeTodoOrderReview.ui.i / R.homeTodoOrderReview.ui.h（或 registry `missing`）

## `/home/after_sales` — `homeAfterSalesList`
- [x] R.homeAfterSalesList.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeAfterSalesList.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeAfterSalesList.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeAfterSalesList.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeAfterSalesList.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeAfterSalesList.ev.a 证据 `notes/evidence/.../home_after_sales.{flutter,kmp}.png`
- [ ] R.homeAfterSalesList.ui.i / R.homeAfterSalesList.ui.h（或 registry `missing`）

## `/home/after_sales/create` — `homeAfterSalesCreate`
- [x] R.homeAfterSalesCreate.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeAfterSalesCreate.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeAfterSalesCreate.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeAfterSalesCreate.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeAfterSalesCreate.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeAfterSalesCreate.ev.a 证据 `notes/evidence/.../home_after_sales_create.{flutter,kmp}.png`
- [ ] R.homeAfterSalesCreate.ui.i / R.homeAfterSalesCreate.ui.h（或 registry `missing`）

## `/home/after_sales/detail` — `homeAfterSalesDetail`
- [ ] R.homeAfterSalesDetail.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.homeAfterSalesDetail.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.homeAfterSalesDetail.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.homeAfterSalesDetail.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.homeAfterSalesDetail.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.homeAfterSalesDetail.ev.a 证据 `notes/evidence/.../home_after_sales_detail.{flutter,kmp}.png`
- [ ] R.homeAfterSalesDetail.ui.i / R.homeAfterSalesDetail.ui.h（或 registry `missing`）

## `/home/new_car_follow` — `homeNewCarFollow`
- [x] R.homeNewCarFollow.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeNewCarFollow.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeNewCarFollow.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeNewCarFollow.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeNewCarFollow.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeNewCarFollow.ev.a 证据 `notes/evidence/.../home_new_car_follow.{flutter,kmp}.png`
- [ ] R.homeNewCarFollow.ui.i / R.homeNewCarFollow.ui.h（或 registry `missing`）

## `/home/new_car_follow/create` — `homeNewCarFollowCreate`
- [x] R.homeNewCarFollowCreate.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeNewCarFollowCreate.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeNewCarFollowCreate.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeNewCarFollowCreate.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeNewCarFollowCreate.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeNewCarFollowCreate.ev.a 证据 `notes/evidence/.../home_new_car_follow_create.{flutter,kmp}.png`
- [ ] R.homeNewCarFollowCreate.ui.i / R.homeNewCarFollowCreate.ui.h（或 registry `missing`）

## `/home/new_car_follow/detail` — `homeNewCarFollowDetail`
- [ ] R.homeNewCarFollowDetail.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.homeNewCarFollowDetail.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.homeNewCarFollowDetail.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.homeNewCarFollowDetail.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.homeNewCarFollowDetail.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.homeNewCarFollowDetail.ev.a 证据 `notes/evidence/.../home_new_car_follow_detail.{flutter,kmp}.png`
- [ ] R.homeNewCarFollowDetail.ui.i / R.homeNewCarFollowDetail.ui.h（或 registry `missing`）

# C. Chat

## `/chat` — `chat`
- [ ] R.chat.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.chat.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.chat.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.chat.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.chat.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.chat.ev.a 证据 `notes/evidence/.../chat.{flutter,kmp}.png`
- [ ] R.chat.ui.i / R.chat.ui.h（或 registry `missing`）

## `/chat/detail` — `chatDetail`
- [ ] R.chatDetail.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.chatDetail.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.chatDetail.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.chatDetail.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.chatDetail.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.chatDetail.ev.a 证据 `notes/evidence/.../chat_detail.{flutter,kmp}.png`
- [ ] R.chatDetail.ui.i / R.chatDetail.ui.h（或 registry `missing`）

# D. Community

## `/community` — `community`
- [ ] R.community.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.community.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.community.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.community.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.community.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.community.ev.a 证据 `notes/evidence/.../community.{flutter,kmp}.png`
- [ ] R.community.ui.i / R.community.ui.h（或 registry `missing`）

## `/community/publish` — `communityPublish`
- [ ] R.communityPublish.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.communityPublish.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.communityPublish.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.communityPublish.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.communityPublish.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.communityPublish.ev.a 证据 `notes/evidence/.../community_publish.{flutter,kmp}.png`
- [ ] R.communityPublish.ui.i / R.communityPublish.ui.h（或 registry `missing`）

## `/community/convention` — `communityConvention`
- [ ] R.communityConvention.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.communityConvention.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.communityConvention.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.communityConvention.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.communityConvention.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.communityConvention.ev.a 证据 `notes/evidence/.../community_convention.{flutter,kmp}.png`
- [ ] R.communityConvention.ui.i / R.communityConvention.ui.h（或 registry `missing`）

## `/community/search` — `communitySearch`
- [ ] R.communitySearch.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.communitySearch.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.communitySearch.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.communitySearch.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.communitySearch.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.communitySearch.ev.a 证据 `notes/evidence/.../community_search.{flutter,kmp}.png`
- [ ] R.communitySearch.ui.i / R.communitySearch.ui.h（或 registry `missing`）

# E. Mine / Settings

## `/mine` — `mine`
- [ ] R.mine.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.mine.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.mine.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.mine.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.mine.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.mine.ev.a 证据 `notes/evidence/.../mine.{flutter,kmp}.png`
- [ ] R.mine.ui.i / R.mine.ui.h（或 registry `missing`）

## `/mine/http_test` — `mineHttpTest`
- [n/a] R.mineHttpTest.oos out-of-scope（debug/demo/bfui）；registry 标注

## `/mine/personalized_settings` — `personalizedSettings`
- [ ] R.personalizedSettings.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.personalizedSettings.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.personalizedSettings.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.personalizedSettings.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.personalizedSettings.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.personalizedSettings.ev.a 证据 `notes/evidence/.../mine_personalized_settings.{flutter,kmp}.png`
- [ ] R.personalizedSettings.ui.i / R.personalizedSettings.ui.h（或 registry `missing`）

## `/mine/profile` — `mineProfile`
- [ ] R.mineProfile.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.mineProfile.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.mineProfile.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.mineProfile.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.mineProfile.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.mineProfile.ev.a 证据 `notes/evidence/.../mine_profile.{flutter,kmp}.png`
- [ ] R.mineProfile.ui.i / R.mineProfile.ui.h（或 registry `missing`）

## `/mine/addresses` — `mineAddresses`
- [ ] R.mineAddresses.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.mineAddresses.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.mineAddresses.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.mineAddresses.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.mineAddresses.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.mineAddresses.ev.a 证据 `notes/evidence/.../mine_addresses.{flutter,kmp}.png`
- [ ] R.mineAddresses.ui.i / R.mineAddresses.ui.h（或 registry `missing`）

## `/mine/addresses/edit` — `mineAddressEdit`
- [ ] R.mineAddressEdit.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.mineAddressEdit.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.mineAddressEdit.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.mineAddressEdit.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.mineAddressEdit.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.mineAddressEdit.ev.a 证据 `notes/evidence/.../mine_addresses_edit.{flutter,kmp}.png`
- [ ] R.mineAddressEdit.ui.i / R.mineAddressEdit.ui.h（或 registry `missing`）

## `/settings` — `settings`
- [x] R.settings.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.settings.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.settings.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.settings.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.settings.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.settings.ev.a 证据 `notes/evidence/.../settings.{flutter,kmp}.png`
- [ ] R.settings.ui.i / R.settings.ui.h（或 registry `missing`）

## `/settings/dialog_demo` — `dialogDemo`
- [n/a] R.dialogDemo.oos out-of-scope（debug/demo/bfui）；registry 标注

## `/settings/linking_debug` — `linkingDebug`
- [n/a] R.linkingDebug.oos out-of-scope（debug/demo/bfui）；registry 标注

## `/settings/realtime_debug` — `realtimeDebug`
- [n/a] R.realtimeDebug.oos out-of-scope（debug/demo/bfui）；registry 标注

## `/settings/im_debug` — `imDebug`
- [n/a] R.imDebug.oos out-of-scope（debug/demo/bfui）；registry 标注

## `/settings/bluetooth_demo` — `bluetoothDemo`
- [n/a] R.bluetoothDemo.oos out-of-scope（debug/demo/bfui）；registry 标注

## `/settings/deal_invoice_demo` — `dealInvoiceDemo`
- [n/a] R.dealInvoiceDemo.oos out-of-scope（debug/demo/bfui）；registry 标注

## `/settings/deal_invoice/upload` — `dealInvoiceUpload`
- [n/a] R.dealInvoiceUpload.oos out-of-scope（debug/demo/bfui）；registry 标注

## `/mine/purchase_calculator` — `purchaseCalculator`
- [ ] R.purchaseCalculator.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.purchaseCalculator.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.purchaseCalculator.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.purchaseCalculator.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.purchaseCalculator.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.purchaseCalculator.ev.a 证据 `notes/evidence/.../mine_purchase_calculator.{flutter,kmp}.png`
- [ ] R.purchaseCalculator.ui.i / R.purchaseCalculator.ui.h（或 registry `missing`）

# F. Mall

## `/mall` — `mall`
- [ ] R.mall.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.mall.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.mall.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.mall.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.mall.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.mall.ev.a 证据 `notes/evidence/.../mall.{flutter,kmp}.png`
- [ ] R.mall.ui.i / R.mall.ui.h（或 registry `missing`）

## `/mall/detail` — `mallDetail`
- [ ] R.mallDetail.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.mallDetail.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.mallDetail.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.mallDetail.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.mallDetail.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.mallDetail.ev.a 证据 `notes/evidence/.../mall_detail.{flutter,kmp}.png`
- [ ] R.mallDetail.ui.i / R.mallDetail.ui.h（或 registry `missing`）

## `/mall/orders` — `mallOrders`
- [ ] R.mallOrders.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.mallOrders.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.mallOrders.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.mallOrders.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.mallOrders.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.mallOrders.ev.a 证据 `notes/evidence/.../mall_orders.{flutter,kmp}.png`
- [ ] R.mallOrders.ui.i / R.mallOrders.ui.h（或 registry `missing`）

## `/mall/orders/detail` — `mallOrderDetail`
- [ ] R.mallOrderDetail.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.mallOrderDetail.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.mallOrderDetail.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.mallOrderDetail.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.mallOrderDetail.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.mallOrderDetail.ev.a 证据 `notes/evidence/.../mall_orders_detail.{flutter,kmp}.png`
- [ ] R.mallOrderDetail.ui.i / R.mallOrderDetail.ui.h（或 registry `missing`）

# G. Pay / Wallet

## `/pay` — `pay`
- [ ] R.pay.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.pay.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.pay.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.pay.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.pay.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.pay.ev.a 证据 `notes/evidence/.../pay.{flutter,kmp}.png`
- [ ] R.pay.ui.i / R.pay.ui.h（或 registry `missing`）

## `/pay/membership` — `payMembership`
- [ ] R.payMembership.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.payMembership.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.payMembership.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.payMembership.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.payMembership.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.payMembership.ev.a 证据 `notes/evidence/.../pay_membership.{flutter,kmp}.png`
- [ ] R.payMembership.ui.i / R.payMembership.ui.h（或 registry `missing`）

## `/wallet` — `wallet`
- [ ] R.wallet.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.wallet.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.wallet.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.wallet.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.wallet.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.wallet.ev.a 证据 `notes/evidence/.../wallet.{flutter,kmp}.png`
- [ ] R.wallet.ui.i / R.wallet.ui.h（或 registry `missing`）

# H. Media（Video / Music / Dubbing）

## `/video` — `video`
- [ ] R.video.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.video.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.video.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.video.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.video.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.video.ev.a 证据 `notes/evidence/.../video.{flutter,kmp}.png`
- [ ] R.video.ui.i / R.video.ui.h（或 registry `missing`）

## `/video/short` — `shortVideo`
- [ ] R.shortVideo.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.shortVideo.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.shortVideo.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.shortVideo.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.shortVideo.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.shortVideo.ev.a 证据 `notes/evidence/.../video_short.{flutter,kmp}.png`
- [ ] R.shortVideo.ui.i / R.shortVideo.ui.h（或 registry `missing`）

## `/video/short/play` — `shortVideoPlay`
- [ ] R.shortVideoPlay.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.shortVideoPlay.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.shortVideoPlay.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.shortVideoPlay.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.shortVideoPlay.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.shortVideoPlay.ev.a 证据 `notes/evidence/.../video_short_play.{flutter,kmp}.png`
- [ ] R.shortVideoPlay.ui.i / R.shortVideoPlay.ui.h（或 registry `missing`）

## `/video/short/publish` — `shortVideoPublish`
- [ ] R.shortVideoPublish.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.shortVideoPublish.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.shortVideoPublish.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.shortVideoPublish.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.shortVideoPublish.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.shortVideoPublish.ev.a 证据 `notes/evidence/.../video_short_publish.{flutter,kmp}.png`
- [ ] R.shortVideoPublish.ui.i / R.shortVideoPublish.ui.h（或 registry `missing`）

## `/video/short/help` — `shortVideoHelp`
- [ ] R.shortVideoHelp.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.shortVideoHelp.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.shortVideoHelp.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.shortVideoHelp.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.shortVideoHelp.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.shortVideoHelp.ev.a 证据 `notes/evidence/.../video_short_help.{flutter,kmp}.png`
- [ ] R.shortVideoHelp.ui.i / R.shortVideoHelp.ui.h（或 registry `missing`）

## `/music/list` — `musicList`
- [ ] R.musicList.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.musicList.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.musicList.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.musicList.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.musicList.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.musicList.ev.a 证据 `notes/evidence/.../music_list.{flutter,kmp}.png`
- [ ] R.musicList.ui.i / R.musicList.ui.h（或 registry `missing`）

## `/music/now_playing` — `musicNowPlaying`
- [ ] R.musicNowPlaying.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.musicNowPlaying.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.musicNowPlaying.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.musicNowPlaying.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.musicNowPlaying.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.musicNowPlaying.ev.a 证据 `notes/evidence/.../music_now_playing.{flutter,kmp}.png`
- [ ] R.musicNowPlaying.ui.i / R.musicNowPlaying.ui.h（或 registry `missing`）

## `/video/dubbing/videos` — `dubbingVideoList`
- [ ] R.dubbingVideoList.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.dubbingVideoList.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.dubbingVideoList.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.dubbingVideoList.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.dubbingVideoList.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.dubbingVideoList.ev.a 证据 `notes/evidence/.../video_dubbing_videos.{flutter,kmp}.png`
- [ ] R.dubbingVideoList.ui.i / R.dubbingVideoList.ui.h（或 registry `missing`）

## `/video/dubbing/videos/detail` — `dubbingVideoDetail`
- [ ] R.dubbingVideoDetail.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.dubbingVideoDetail.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.dubbingVideoDetail.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.dubbingVideoDetail.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.dubbingVideoDetail.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.dubbingVideoDetail.ev.a 证据 `notes/evidence/.../video_dubbing_videos_detail.{flutter,kmp}.png`
- [ ] R.dubbingVideoDetail.ui.i / R.dubbingVideoDetail.ui.h（或 registry `missing`）

## `/video/dubbing/works` — `dubbingWorkList`
- [ ] R.dubbingWorkList.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.dubbingWorkList.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.dubbingWorkList.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.dubbingWorkList.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.dubbingWorkList.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.dubbingWorkList.ev.a 证据 `notes/evidence/.../video_dubbing_works.{flutter,kmp}.png`
- [ ] R.dubbingWorkList.ui.i / R.dubbingWorkList.ui.h（或 registry `missing`）

## `/video/dubbing/works/detail` — `dubbingWorkDetail`
- [ ] R.dubbingWorkDetail.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.dubbingWorkDetail.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.dubbingWorkDetail.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.dubbingWorkDetail.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.dubbingWorkDetail.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.dubbingWorkDetail.ev.a 证据 `notes/evidence/.../video_dubbing_works_detail.{flutter,kmp}.png`
- [ ] R.dubbingWorkDetail.ui.i / R.dubbingWorkDetail.ui.h（或 registry `missing`）

# I. Classroom

## `/classroom/my_class` — `classroomMyClass`
- [ ] R.classroomMyClass.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.classroomMyClass.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.classroomMyClass.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.classroomMyClass.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.classroomMyClass.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.classroomMyClass.ev.a 证据 `notes/evidence/.../classroom_my_class.{flutter,kmp}.png`
- [ ] R.classroomMyClass.ui.i / R.classroomMyClass.ui.h（或 registry `missing`）

## `/classroom/homework_stats` — `classroomHomeworkStats`
- [ ] R.classroomHomeworkStats.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.classroomHomeworkStats.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.classroomHomeworkStats.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.classroomHomeworkStats.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.classroomHomeworkStats.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.classroomHomeworkStats.ev.a 证据 `notes/evidence/.../classroom_homework_stats.{flutter,kmp}.png`
- [ ] R.classroomHomeworkStats.ui.i / R.classroomHomeworkStats.ui.h（或 registry `missing`）

## `/classroom/homework/detail_teacher` — `classroomHomeworkDetailTeacher`
- [ ] R.classroomHomeworkDetailTeacher.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.classroomHomeworkDetailTeacher.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.classroomHomeworkDetailTeacher.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.classroomHomeworkDetailTeacher.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.classroomHomeworkDetailTeacher.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.classroomHomeworkDetailTeacher.ev.a 证据 `notes/evidence/.../classroom_homework_detail_teacher.{flutter,kmp}.png`
- [ ] R.classroomHomeworkDetailTeacher.ui.i / R.classroomHomeworkDetailTeacher.ui.h（或 registry `missing`）

## `/classroom/homework/detail_student` — `classroomHomeworkDetailStudent`
- [ ] R.classroomHomeworkDetailStudent.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.classroomHomeworkDetailStudent.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.classroomHomeworkDetailStudent.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.classroomHomeworkDetailStudent.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.classroomHomeworkDetailStudent.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.classroomHomeworkDetailStudent.ev.a 证据 `notes/evidence/.../classroom_homework_detail_student.{flutter,kmp}.png`
- [ ] R.classroomHomeworkDetailStudent.ui.i / R.classroomHomeworkDetailStudent.ui.h（或 registry `missing`）

## `/classroom/homework/dubbing` — `classroomDubbingHomework`
- [ ] R.classroomDubbingHomework.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.classroomDubbingHomework.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.classroomDubbingHomework.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.classroomDubbingHomework.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.classroomDubbingHomework.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.classroomDubbingHomework.ev.a 证据 `notes/evidence/.../classroom_homework_dubbing.{flutter,kmp}.png`
- [ ] R.classroomDubbingHomework.ui.i / R.classroomDubbingHomework.ui.h（或 registry `missing`）

## `/classroom/homework/review` — `classroomHomeworkReview`
- [ ] R.classroomHomeworkReview.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.classroomHomeworkReview.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.classroomHomeworkReview.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.classroomHomeworkReview.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.classroomHomeworkReview.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.classroomHomeworkReview.ev.a 证据 `notes/evidence/.../classroom_homework_review.{flutter,kmp}.png`
- [ ] R.classroomHomeworkReview.ui.i / R.classroomHomeworkReview.ui.h（或 registry `missing`）

## `/classroom/gift/claim` — `classroomClaimGift`
- [ ] R.classroomClaimGift.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.classroomClaimGift.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.classroomClaimGift.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.classroomClaimGift.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.classroomClaimGift.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.classroomClaimGift.ev.a 证据 `notes/evidence/.../classroom_gift_claim.{flutter,kmp}.png`
- [ ] R.classroomClaimGift.ui.i / R.classroomClaimGift.ui.h（或 registry `missing`）

## `/classroom/video/detail` — `classroomVideoDetail`
- [ ] R.classroomVideoDetail.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.classroomVideoDetail.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.classroomVideoDetail.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.classroomVideoDetail.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.classroomVideoDetail.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.classroomVideoDetail.ev.a 证据 `notes/evidence/.../classroom_video_detail.{flutter,kmp}.png`
- [ ] R.classroomVideoDetail.ui.i / R.classroomVideoDetail.ui.h（或 registry `missing`）

# J. Live / Friend / AI

## `/ai/stream` — `aiStream`
- [ ] R.aiStream.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.aiStream.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.aiStream.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.aiStream.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.aiStream.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.aiStream.ev.a 证据 `notes/evidence/.../ai_stream.{flutter,kmp}.png`
- [ ] R.aiStream.ui.i / R.aiStream.ui.h（或 registry `missing`）

## `/friend` — `friend`
- [ ] R.friend.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.friend.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.friend.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.friend.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.friend.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.friend.ev.a 证据 `notes/evidence/.../friend.{flutter,kmp}.png`
- [ ] R.friend.ui.i / R.friend.ui.h（或 registry `missing`）

## `/live` — `live`
- [ ] R.live.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.live.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.live.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.live.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.live.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.live.ev.a 证据 `notes/evidence/.../live.{flutter,kmp}.png`
- [ ] R.live.ui.i / R.live.ui.h（或 registry `missing`）

## `/live/room` — `liveRoom`
- [ ] R.liveRoom.ui.a UI 对照 Flutter 同页（并排截图）
- [ ] R.liveRoom.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [ ] R.liveRoom.nav.a 导航参数/返回栈对齐 RoutePath
- [ ] R.liveRoom.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [ ] R.liveRoom.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [ ] R.liveRoom.ev.a 证据 `notes/evidence/.../live_room.{flutter,kmp}.png`
- [ ] R.liveRoom.ui.i / R.liveRoom.ui.h（或 registry `missing`）

# K. Misc

## `/bfui/introduction_animation` — `bfuiIntroductionAnimation`
- [n/a] R.bfuiIntroductionAnimation.oos out-of-scope（debug/demo/bfui）；registry 标注

## `/bfui/hotel_booking` — `bfuiHotelBooking`
- [n/a] R.bfuiHotelBooking.oos out-of-scope（debug/demo/bfui）；registry 标注

## `/bfui/hotel_filters` — `bfuiHotelFilters`
- [n/a] R.bfuiHotelFilters.oos out-of-scope（debug/demo/bfui）；registry 标注

## `/bfui/fitness_app` — `bfuiFitnessApp`
- [n/a] R.bfuiFitnessApp.oos out-of-scope（debug/demo/bfui）；registry 标注

## `/bfui/my_diary` — `bfuiMyDiary`
- [n/a] R.bfuiMyDiary.oos out-of-scope（debug/demo/bfui）；registry 标注

## `/bfui/training` — `bfuiTraining`
- [n/a] R.bfuiTraining.oos out-of-scope（debug/demo/bfui）；registry 标注

## `/bfui/design_course` — `bfuiDesignCourse`
- [n/a] R.bfuiDesignCourse.oos out-of-scope（debug/demo/bfui）；registry 标注

## `/bfui/course_info` — `bfuiCourseInfo`
- [n/a] R.bfuiCourseInfo.oos out-of-scope（debug/demo/bfui）；registry 标注

## `/bfui/help` — `bfuiHelp`
- [n/a] R.bfuiHelp.oos out-of-scope（debug/demo/bfui）；registry 标注

## `/bfui/feedback` — `bfuiFeedback`
- [n/a] R.bfuiFeedback.oos out-of-scope（debug/demo/bfui）；registry 标注

## `/bfui/invite_friend` — `bfuiInviteFriend`
- [n/a] R.bfuiInviteFriend.oos out-of-scope（debug/demo/bfui）；registry 标注

## `/bfui/navigation_drawer` — `bfuiNavigationDrawer`
- [n/a] R.bfuiNavigationDrawer.oos out-of-scope（debug/demo/bfui）；registry 标注

## `/bfui/glass_view` — `bfuiGlassView`
- [n/a] R.bfuiGlassView.oos out-of-scope（debug/demo/bfui）；registry 标注

## `/bfui/wave_view` — `bfuiWaveView`
- [n/a] R.bfuiWaveView.oos out-of-scope（debug/demo/bfui）；registry 标注

## `/bfui/running_view` — `bfuiRunningView`
- [n/a] R.bfuiRunningView.oos out-of-scope（debug/demo/bfui）；registry 标注

## `/bfui/workout_view` — `bfuiWorkoutView`
- [n/a] R.bfuiWorkoutView.oos out-of-scope（debug/demo/bfui）；registry 标注

## `/bfui/mediterranean_diet` — `bfuiMediterraneanDiet`
- [n/a] R.bfuiMediterraneanDiet.oos out-of-scope（debug/demo/bfui）；registry 标注

# L. 非独立路由但必须做

- [ ] L.1 Privacy 首次同意 + 持久化（三端）
- [ ] L.2 Scan 权限拒绝/成功（三端或 gap）
- [ ] L.3 Deeplink 冷启动（三端或 gap）
- [ ] L.4 Push 注册+payload→route（或 gap `missing`）
- [ ] L.5 微信登录各端 done|missing
- [ ] L.6 微信支付 / 支付宝 SDK 或 gap `stub`（不可标业务完成）
- [ ] L.7 All Services 分区 catalog 与 Flutter 一一对应 + 编辑常用
- [ ] L.8 Home 公司数据/待办 API 与 Flutter 同契约
- [ ] L.9 Community feed 真实数据契约（非截图烘焙）
- [ ] L.10 Chat IM 引擎：mock 对齐 Flutter 行为并 registry `partial`，或接真 SDK
- [ ] L.11 Music mini-player 全局 inset
- [ ] L.12 删除错误 SoT 证据；只保留并排合格图

---

# Z. 全部完成后的强制回检（不可跳过）

> 当本清单 **in-scope** 项全部勾选后，**不得结案**。必须执行本门禁；失败则把对应项改回 `[ ]` 并继续做，直到门禁全绿。

## Z.1 机器勾选审计

- [ ] Z.1.1 脚本统计：本文件 in-scope `[ ]`/`[~]` 计数为 0（允许仅剩已登记 `missing` 的 `.i`/`.h`）
- [ ] Z.1.2 `grep` 代码：`SimpleDetail` / `DeferredStub` / `一期后置` / 自造「消息通知」设置页 — 不得出现在宣称完成的路由
- [ ] Z.1.3 `RoutePath` 103 条：每条为 `[x]` 或 `[n/a]` 或「平台 missing 已登记」

## Z.2 Flutter 并排抽样（至少）

- [ ] Z.2.1 壳：Splash → Privacy → 四 Tab
- [ ] Z.2.2 登录：guest→OTP/密码→resume
- [ ] Z.2.3 Home 根 + 全部服务 + 搜索
- [ ] Z.2.4 Home 随机 5 条二级（含列表→详情）
- [ ] Z.2.5 Chat 列表→详情→发送
- [ ] Z.2.6 Community feed→发布
- [ ] Z.2.7 Mine 根 + **真** Settings + Profile + Addresses
- [ ] Z.2.8 Mall 列表→详情→订单
- [ ] Z.2.9 Pay/Membership 不可伪造成功
- [ ] Z.2.10 Classroom / Live / Friend / Music / AI 各 1 条主路径

## Z.3 失败处理闭环

- [ ] Z.3.1 任一 Z.2 失败：在本文件对应 `R.*` 改回 `[ ]`，写失败原因一行
- [ ] Z.3.2 修复后重跑该条 + 相关入口
- [ ] Z.3.3 全部 Z.1–Z.2 绿后，才允许更新 `acceptance-report.md` 与考虑 `/opsx-archive`
- [ ] Z.3.4 若只完成 Android：报告必须列出 iOS/OHOS `missing` 清单，禁止写「项目完成」

## Z.4 结案命令（门禁绿之后）

```bash
# 1) 未完成计数应为 0（除 n/a 与已登记 missing）
python3 scripts/parity_task_audit.py  # 或下文内嵌统计
# 2) 编译
./gradlew :composeApp:testDebugUnitTest -PandroidOnly=true
# 3) openspec validate
openspec validate --change parity-flutter-to-kmp
```

---

## 附录：路由规模

- RoutePath 总计：**103**
- In-scope：**78** ×（每路由约 7 子项）≈ **546** 细项
- Out-of-scope：**25**
- 加横切 L + 门禁 Z ≈ 总细项 **~576**

文件路径：`openspec/changes/parity-flutter-to-kmp/tasks-execution.md`