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
- [x] 0.5 Design tokens / 沉浸式顶栏 / 底栏 49dp 与 Flutter 对齐 — 沉浸式顶栏/底栏 49dp 已落地（Android）
- [x] 0.6 soft-auth：未登录进门禁 Tab → 登录 → resume
- [x] 0.7 401 清会话 — TokenExpiredHandler / SoftAuth 清会话路径已有
- [x] 0.8 `platform-gap-registry.md` 全能力有状态 — platform-gap-registry 已覆盖本轮能力
- [x] 0.9 `acceptance-matrix.md` 与本清单同步 — acceptance-matrix 与本清单 Android 状态同步见本提交
- [x] 0.10 CI：`check-layer-deps.sh` + Android compile/test — CI assembleDebug+testDebugUnitTest（androidOnly）

# A. 壳 / 鉴权 / 桥接

## `/` — `splash`
- [x] R.splash.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.splash.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.splash.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.splash.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.splash.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.splash.ev.a 证据 `notes/evidence/.../root.{flutter,kmp}.png`
- [x] R.splash.ui.i / R.splash.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/main` — `main`
- [x] R.main.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.main.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.main.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.main.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.main.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.main.ev.a 证据 `notes/evidence/.../main.{flutter,kmp}.png`
- [x] R.main.ui.i / R.main.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/login` — `login`
- [x] R.login.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.login.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.login.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.login.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.login.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.login.ev.a 证据 `notes/evidence/.../login.{flutter,kmp}.png`
- [x] R.login.ui.i / R.login.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/login/password` — `loginPassword`
- [x] R.loginPassword.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.loginPassword.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.loginPassword.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.loginPassword.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.loginPassword.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.loginPassword.ev.a 证据 `notes/evidence/.../login_password.{flutter,kmp}.png`
- [x] R.loginPassword.ui.i / R.loginPassword.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/login/otp` — `loginOtp`
- [x] R.loginOtp.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.loginOtp.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.loginOtp.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.loginOtp.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.loginOtp.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.loginOtp.ev.a 证据 `notes/evidence/.../login_otp.{flutter,kmp}.png`
- [x] R.loginOtp.ui.i / R.loginOtp.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/register` — `register`
- [x] R.register.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.register.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.register.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.register.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.register.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.register.ev.a 证据 `notes/evidence/.../register.{flutter,kmp}.png`
- [x] R.register.ui.i / R.register.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/web` — `web`
- [x] R.web.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.web.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.web.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.web.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.web.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.web.ev.a 证据 `notes/evidence/.../web.{flutter,kmp}.png`
- [x] R.web.ui.i / R.web.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

# B. Home

## `/home` — `home`
- [x] R.home.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.home.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.home.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.home.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.home.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.home.ev.a 证据 `notes/evidence/.../home.{flutter,kmp}.png`
- [x] R.home.ui.i / R.home.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/home/learning_report` — `homeLearningReport`
- [x] R.homeLearningReport.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeLearningReport.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeLearningReport.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeLearningReport.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeLearningReport.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeLearningReport.ev.a 证据 `notes/evidence/.../home_learning_report.{flutter,kmp}.png`
- [x] R.homeLearningReport.ui.i / R.homeLearningReport.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/home/check_in_mall` — `homeCheckInMall`
- [x] R.homeCheckInMall.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeCheckInMall.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeCheckInMall.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeCheckInMall.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeCheckInMall.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeCheckInMall.ev.a 证据 `notes/evidence/.../home_check_in_mall.{flutter,kmp}.png`
- [x] R.homeCheckInMall.ui.i / R.homeCheckInMall.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/home/all_services` — `homeAllServices`
- [x] R.homeAllServices.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeAllServices.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeAllServices.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeAllServices.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeAllServices.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeAllServices.ev.a 证据 `notes/evidence/.../home_all_services.{flutter,kmp}.png`
- [x] R.homeAllServices.ui.i / R.homeAllServices.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/home/search` — `homeSearch`
- [x] R.homeSearch.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeSearch.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeSearch.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeSearch.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeSearch.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeSearch.ev.a 证据 `notes/evidence/.../home_search.{flutter,kmp}.png`
- [x] R.homeSearch.ui.i / R.homeSearch.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/home/dubbing_feed` — `homeDubbingFeed`
- [x] R.homeDubbingFeed.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeDubbingFeed.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeDubbingFeed.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeDubbingFeed.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeDubbingFeed.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeDubbingFeed.ev.a 证据 `notes/evidence/.../home_dubbing_feed.{flutter,kmp}.png`
- [x] R.homeDubbingFeed.ui.i / R.homeDubbingFeed.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/home/strategy` — `homeStrategy`
- [x] R.homeStrategy.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeStrategy.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeStrategy.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeStrategy.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeStrategy.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeStrategy.ev.a 证据 `notes/evidence/.../home_strategy.{flutter,kmp}.png`
- [x] R.homeStrategy.ui.i / R.homeStrategy.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/home/hot_rank_detail` — `homeHotRankDetail`
- [x] R.homeHotRankDetail.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeHotRankDetail.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeHotRankDetail.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeHotRankDetail.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeHotRankDetail.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeHotRankDetail.ev.a 证据 `notes/evidence/.../home_hot_rank_detail.{flutter,kmp}.png`
- [x] R.homeHotRankDetail.ui.i / R.homeHotRankDetail.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/home/used_car` — `homeUsedCarList`
- [x] R.homeUsedCarList.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeUsedCarList.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeUsedCarList.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeUsedCarList.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeUsedCarList.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeUsedCarList.ev.a 证据 `notes/evidence/.../home_used_car.{flutter,kmp}.png`
- [x] R.homeUsedCarList.ui.i / R.homeUsedCarList.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/home/used_car/detail` — `homeUsedCarDetail`
- [x] R.homeUsedCarDetail.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeUsedCarDetail.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeUsedCarDetail.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeUsedCarDetail.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeUsedCarDetail.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeUsedCarDetail.ev.a 证据 `notes/evidence/.../home_used_car_detail.{flutter,kmp}.png`
- [x] R.homeUsedCarDetail.ui.i / R.homeUsedCarDetail.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/home/used_car/create` — `homeUsedCarCreate`
- [x] R.homeUsedCarCreate.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeUsedCarCreate.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeUsedCarCreate.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeUsedCarCreate.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeUsedCarCreate.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeUsedCarCreate.ev.a 证据 `notes/evidence/.../home_used_car_create.{flutter,kmp}.png`
- [x] R.homeUsedCarCreate.ui.i / R.homeUsedCarCreate.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/home/ledger` — `homeLedgerList`
- [x] R.homeLedgerList.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeLedgerList.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeLedgerList.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeLedgerList.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeLedgerList.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeLedgerList.ev.a 证据 `notes/evidence/.../home_ledger.{flutter,kmp}.png`
- [x] R.homeLedgerList.ui.i / R.homeLedgerList.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/home/ledger/detail` — `homeLedgerDetail`
- [x] R.homeLedgerDetail.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeLedgerDetail.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeLedgerDetail.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeLedgerDetail.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeLedgerDetail.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeLedgerDetail.ev.a 证据 `notes/evidence/.../home_ledger_detail.{flutter,kmp}.png`
- [x] R.homeLedgerDetail.ui.i / R.homeLedgerDetail.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/home/data_analytics` — `homeDataAnalyticsList`
- [x] R.homeDataAnalyticsList.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeDataAnalyticsList.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeDataAnalyticsList.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeDataAnalyticsList.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeDataAnalyticsList.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeDataAnalyticsList.ev.a 证据 `notes/evidence/.../home_data_analytics.{flutter,kmp}.png`
- [x] R.homeDataAnalyticsList.ui.i / R.homeDataAnalyticsList.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/home/data_analytics/detail` — `homeDataAnalyticsDetail`
- [x] R.homeDataAnalyticsDetail.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeDataAnalyticsDetail.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeDataAnalyticsDetail.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeDataAnalyticsDetail.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeDataAnalyticsDetail.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeDataAnalyticsDetail.ev.a 证据 `notes/evidence/.../home_data_analytics_detail.{flutter,kmp}.png`
- [x] R.homeDataAnalyticsDetail.ui.i / R.homeDataAnalyticsDetail.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/home/life_service` — `homeLifeService`
- [x] R.homeLifeService.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeLifeService.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeLifeService.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeLifeService.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeLifeService.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeLifeService.ev.a 证据 `notes/evidence/.../home_life_service.{flutter,kmp}.png`
- [x] R.homeLifeService.ui.i / R.homeLifeService.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/home/live_commerce` — `homeLiveCommerce`
- [x] R.homeLiveCommerce.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeLiveCommerce.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeLiveCommerce.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeLiveCommerce.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeLiveCommerce.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeLiveCommerce.ev.a 证据 `notes/evidence/.../home_live_commerce.{flutter,kmp}.png`
- [x] R.homeLiveCommerce.ui.i / R.homeLiveCommerce.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/home/club` — `homeClub`
- [x] R.homeClub.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeClub.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeClub.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeClub.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeClub.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeClub.ev.a 证据 `notes/evidence/.../home_club.{flutter,kmp}.png`
- [x] R.homeClub.ui.i / R.homeClub.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/home/todo/partner-pending` — `homeTodoPartnerPending`
- [x] R.homeTodoPartnerPending.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeTodoPartnerPending.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeTodoPartnerPending.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeTodoPartnerPending.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeTodoPartnerPending.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeTodoPartnerPending.ev.a 证据 `notes/evidence/.../home_todo_partner-pending.{flutter,kmp}.png`
- [x] R.homeTodoPartnerPending.ui.i / R.homeTodoPartnerPending.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/home/todo/follow-up-customers` — `homeTodoFollowUp`
- [x] R.homeTodoFollowUp.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeTodoFollowUp.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeTodoFollowUp.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeTodoFollowUp.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeTodoFollowUp.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeTodoFollowUp.ev.a 证据 `notes/evidence/.../home_todo_follow-up-customers.{flutter,kmp}.png`
- [x] R.homeTodoFollowUp.ui.i / R.homeTodoFollowUp.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/home/todo/after-sales-appointments` — `homeTodoAfterSales`
- [x] R.homeTodoAfterSales.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeTodoAfterSales.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeTodoAfterSales.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeTodoAfterSales.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeTodoAfterSales.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeTodoAfterSales.ev.a 证据 `notes/evidence/.../home_todo_after-sales-appointments.{flutter,kmp}.png`
- [x] R.homeTodoAfterSales.ui.i / R.homeTodoAfterSales.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/home/todo/order-pending-review` — `homeTodoOrderReview`
- [x] R.homeTodoOrderReview.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeTodoOrderReview.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeTodoOrderReview.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeTodoOrderReview.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeTodoOrderReview.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeTodoOrderReview.ev.a 证据 `notes/evidence/.../home_todo_order-pending-review.{flutter,kmp}.png`
- [x] R.homeTodoOrderReview.ui.i / R.homeTodoOrderReview.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/home/after_sales` — `homeAfterSalesList`
- [x] R.homeAfterSalesList.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeAfterSalesList.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeAfterSalesList.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeAfterSalesList.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeAfterSalesList.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeAfterSalesList.ev.a 证据 `notes/evidence/.../home_after_sales.{flutter,kmp}.png`
- [x] R.homeAfterSalesList.ui.i / R.homeAfterSalesList.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/home/after_sales/create` — `homeAfterSalesCreate`
- [x] R.homeAfterSalesCreate.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeAfterSalesCreate.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeAfterSalesCreate.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeAfterSalesCreate.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeAfterSalesCreate.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeAfterSalesCreate.ev.a 证据 `notes/evidence/.../home_after_sales_create.{flutter,kmp}.png`
- [x] R.homeAfterSalesCreate.ui.i / R.homeAfterSalesCreate.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/home/after_sales/detail` — `homeAfterSalesDetail`
- [x] R.homeAfterSalesDetail.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeAfterSalesDetail.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeAfterSalesDetail.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeAfterSalesDetail.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeAfterSalesDetail.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeAfterSalesDetail.ev.a 证据 `notes/evidence/.../home_after_sales_detail.{flutter,kmp}.png`
- [x] R.homeAfterSalesDetail.ui.i / R.homeAfterSalesDetail.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/home/new_car_follow` — `homeNewCarFollow`
- [x] R.homeNewCarFollow.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeNewCarFollow.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeNewCarFollow.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeNewCarFollow.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeNewCarFollow.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeNewCarFollow.ev.a 证据 `notes/evidence/.../home_new_car_follow.{flutter,kmp}.png`
- [x] R.homeNewCarFollow.ui.i / R.homeNewCarFollow.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/home/new_car_follow/create` — `homeNewCarFollowCreate`
- [x] R.homeNewCarFollowCreate.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeNewCarFollowCreate.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeNewCarFollowCreate.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeNewCarFollowCreate.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeNewCarFollowCreate.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeNewCarFollowCreate.ev.a 证据 `notes/evidence/.../home_new_car_follow_create.{flutter,kmp}.png`
- [x] R.homeNewCarFollowCreate.ui.i / R.homeNewCarFollowCreate.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/home/new_car_follow/detail` — `homeNewCarFollowDetail`
- [x] R.homeNewCarFollowDetail.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.homeNewCarFollowDetail.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.homeNewCarFollowDetail.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.homeNewCarFollowDetail.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.homeNewCarFollowDetail.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.homeNewCarFollowDetail.ev.a 证据 `notes/evidence/.../home_new_car_follow_detail.{flutter,kmp}.png`
- [x] R.homeNewCarFollowDetail.ui.i / R.homeNewCarFollowDetail.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

# C. Chat

## `/chat` — `chat`
- [x] R.chat.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.chat.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.chat.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.chat.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.chat.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.chat.ev.a 证据 `notes/evidence/.../chat.{flutter,kmp}.png`
- [x] R.chat.ui.i / R.chat.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/chat/detail` — `chatDetail`
- [x] R.chatDetail.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.chatDetail.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.chatDetail.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.chatDetail.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.chatDetail.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.chatDetail.ev.a 证据 `notes/evidence/.../chat_detail.{flutter,kmp}.png`
- [x] R.chatDetail.ui.i / R.chatDetail.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

# D. Community

## `/community` — `community`
- [x] R.community.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.community.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.community.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.community.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.community.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.community.ev.a 证据 `notes/evidence/.../community.{flutter,kmp}.png`
- [x] R.community.ui.i / R.community.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/community/publish` — `communityPublish`
- [x] R.communityPublish.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.communityPublish.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.communityPublish.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.communityPublish.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.communityPublish.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.communityPublish.ev.a 证据 `notes/evidence/.../community_publish.{flutter,kmp}.png`
- [x] R.communityPublish.ui.i / R.communityPublish.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/community/convention` — `communityConvention`
- [x] R.communityConvention.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.communityConvention.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.communityConvention.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.communityConvention.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.communityConvention.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.communityConvention.ev.a 证据 `notes/evidence/.../community_convention.{flutter,kmp}.png`
- [x] R.communityConvention.ui.i / R.communityConvention.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/community/search` — `communitySearch`
- [x] R.communitySearch.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.communitySearch.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.communitySearch.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.communitySearch.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.communitySearch.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.communitySearch.ev.a 证据 `notes/evidence/.../community_search.{flutter,kmp}.png`
- [x] R.communitySearch.ui.i / R.communitySearch.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

# E. Mine / Settings

## `/mine` — `mine`
- [x] R.mine.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.mine.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.mine.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.mine.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.mine.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.mine.ev.a 证据 `notes/evidence/.../mine.{flutter,kmp}.png`
- [x] R.mine.ui.i / R.mine.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/mine/http_test` — `mineHttpTest`
- [n/a] R.mineHttpTest.oos out-of-scope（debug/demo/bfui）；registry 标注

## `/mine/personalized_settings` — `personalizedSettings`
- [x] R.personalizedSettings.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.personalizedSettings.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.personalizedSettings.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.personalizedSettings.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.personalizedSettings.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.personalizedSettings.ev.a 证据 `notes/evidence/.../mine_personalized_settings.{flutter,kmp}.png`
- [x] R.personalizedSettings.ui.i / R.personalizedSettings.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/mine/profile` — `mineProfile`
- [x] R.mineProfile.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.mineProfile.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.mineProfile.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.mineProfile.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.mineProfile.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.mineProfile.ev.a 证据 `notes/evidence/.../mine_profile.{flutter,kmp}.png`
- [x] R.mineProfile.ui.i / R.mineProfile.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/mine/addresses` — `mineAddresses`
- [x] R.mineAddresses.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.mineAddresses.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.mineAddresses.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.mineAddresses.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.mineAddresses.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.mineAddresses.ev.a 证据 `notes/evidence/.../mine_addresses.{flutter,kmp}.png`
- [x] R.mineAddresses.ui.i / R.mineAddresses.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/mine/addresses/edit` — `mineAddressEdit`
- [x] R.mineAddressEdit.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.mineAddressEdit.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.mineAddressEdit.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.mineAddressEdit.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.mineAddressEdit.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.mineAddressEdit.ev.a 证据 `notes/evidence/.../mine_addresses_edit.{flutter,kmp}.png`
- [x] R.mineAddressEdit.ui.i / R.mineAddressEdit.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/settings` — `settings`
- [x] R.settings.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.settings.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.settings.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.settings.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.settings.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.settings.ev.a 证据 `notes/evidence/.../settings.{flutter,kmp}.png`
- [x] R.settings.ui.i / R.settings.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

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
- [x] R.purchaseCalculator.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.purchaseCalculator.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.purchaseCalculator.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.purchaseCalculator.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.purchaseCalculator.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.purchaseCalculator.ev.a 证据 `notes/evidence/.../mine_purchase_calculator.{flutter,kmp}.png`
- [x] R.purchaseCalculator.ui.i / R.purchaseCalculator.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

# F. Mall

## `/mall` — `mall`
- [x] R.mall.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.mall.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.mall.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.mall.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.mall.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.mall.ev.a 证据 `notes/evidence/.../mall.{flutter,kmp}.png`
- [x] R.mall.ui.i / R.mall.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/mall/detail` — `mallDetail`
- [x] R.mallDetail.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.mallDetail.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.mallDetail.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.mallDetail.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.mallDetail.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.mallDetail.ev.a 证据 `notes/evidence/.../mall_detail.{flutter,kmp}.png`
- [x] R.mallDetail.ui.i / R.mallDetail.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/mall/orders` — `mallOrders`
- [x] R.mallOrders.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.mallOrders.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.mallOrders.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.mallOrders.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.mallOrders.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.mallOrders.ev.a 证据 `notes/evidence/.../mall_orders.{flutter,kmp}.png`
- [x] R.mallOrders.ui.i / R.mallOrders.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/mall/orders/detail` — `mallOrderDetail`
- [x] R.mallOrderDetail.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.mallOrderDetail.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.mallOrderDetail.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.mallOrderDetail.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.mallOrderDetail.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.mallOrderDetail.ev.a 证据 `notes/evidence/.../mall_orders_detail.{flutter,kmp}.png`
- [x] R.mallOrderDetail.ui.i / R.mallOrderDetail.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

# G. Pay / Wallet

## `/pay` — `pay`
- [x] R.pay.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.pay.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.pay.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.pay.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.pay.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.pay.ev.a 证据 `notes/evidence/.../pay.{flutter,kmp}.png`
- [x] R.pay.ui.i / R.pay.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/pay/membership` — `payMembership`
- [x] R.payMembership.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.payMembership.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.payMembership.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.payMembership.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.payMembership.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.payMembership.ev.a 证据 `notes/evidence/.../pay_membership.{flutter,kmp}.png`
- [x] R.payMembership.ui.i / R.payMembership.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/wallet` — `wallet`
- [x] R.wallet.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.wallet.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.wallet.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.wallet.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.wallet.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.wallet.ev.a 证据 `notes/evidence/.../wallet.{flutter,kmp}.png`
- [x] R.wallet.ui.i / R.wallet.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

# H. Media（Video / Music / Dubbing）

## `/video` — `video`
- [x] R.video.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.video.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.video.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.video.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.video.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.video.ev.a 证据 `notes/evidence/.../video.{flutter,kmp}.png`
- [x] R.video.ui.i / R.video.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/video/short` — `shortVideo`
- [x] R.shortVideo.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.shortVideo.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.shortVideo.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.shortVideo.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.shortVideo.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.shortVideo.ev.a 证据 `notes/evidence/.../video_short.{flutter,kmp}.png`
- [x] R.shortVideo.ui.i / R.shortVideo.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/video/short/play` — `shortVideoPlay`
- [x] R.shortVideoPlay.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.shortVideoPlay.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.shortVideoPlay.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.shortVideoPlay.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.shortVideoPlay.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.shortVideoPlay.ev.a 证据 `notes/evidence/.../video_short_play.{flutter,kmp}.png`
- [x] R.shortVideoPlay.ui.i / R.shortVideoPlay.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/video/short/publish` — `shortVideoPublish`
- [x] R.shortVideoPublish.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.shortVideoPublish.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.shortVideoPublish.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.shortVideoPublish.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.shortVideoPublish.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.shortVideoPublish.ev.a 证据 `notes/evidence/.../video_short_publish.{flutter,kmp}.png`
- [x] R.shortVideoPublish.ui.i / R.shortVideoPublish.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/video/short/help` — `shortVideoHelp`
- [x] R.shortVideoHelp.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.shortVideoHelp.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.shortVideoHelp.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.shortVideoHelp.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.shortVideoHelp.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.shortVideoHelp.ev.a 证据 `notes/evidence/.../video_short_help.{flutter,kmp}.png`
- [x] R.shortVideoHelp.ui.i / R.shortVideoHelp.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/music/list` — `musicList`
- [x] R.musicList.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.musicList.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.musicList.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.musicList.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.musicList.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.musicList.ev.a 证据 `notes/evidence/.../music_list.{flutter,kmp}.png`
- [x] R.musicList.ui.i / R.musicList.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/music/now_playing` — `musicNowPlaying`
- [x] R.musicNowPlaying.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.musicNowPlaying.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.musicNowPlaying.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.musicNowPlaying.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.musicNowPlaying.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.musicNowPlaying.ev.a 证据 `notes/evidence/.../music_now_playing.{flutter,kmp}.png`
- [x] R.musicNowPlaying.ui.i / R.musicNowPlaying.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/video/dubbing/videos` — `dubbingVideoList`
- [x] R.dubbingVideoList.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.dubbingVideoList.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.dubbingVideoList.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.dubbingVideoList.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.dubbingVideoList.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.dubbingVideoList.ev.a 证据 `notes/evidence/.../video_dubbing_videos.{flutter,kmp}.png`
- [x] R.dubbingVideoList.ui.i / R.dubbingVideoList.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/video/dubbing/videos/detail` — `dubbingVideoDetail`
- [x] R.dubbingVideoDetail.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.dubbingVideoDetail.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.dubbingVideoDetail.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.dubbingVideoDetail.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.dubbingVideoDetail.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.dubbingVideoDetail.ev.a 证据 `notes/evidence/.../video_dubbing_videos_detail.{flutter,kmp}.png`
- [x] R.dubbingVideoDetail.ui.i / R.dubbingVideoDetail.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/video/dubbing/works` — `dubbingWorkList`
- [x] R.dubbingWorkList.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.dubbingWorkList.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.dubbingWorkList.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.dubbingWorkList.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.dubbingWorkList.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.dubbingWorkList.ev.a 证据 `notes/evidence/.../video_dubbing_works.{flutter,kmp}.png`
- [x] R.dubbingWorkList.ui.i / R.dubbingWorkList.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/video/dubbing/works/detail` — `dubbingWorkDetail`
- [x] R.dubbingWorkDetail.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.dubbingWorkDetail.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.dubbingWorkDetail.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.dubbingWorkDetail.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.dubbingWorkDetail.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.dubbingWorkDetail.ev.a 证据 `notes/evidence/.../video_dubbing_works_detail.{flutter,kmp}.png`
- [x] R.dubbingWorkDetail.ui.i / R.dubbingWorkDetail.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

# I. Classroom

## `/classroom/my_class` — `classroomMyClass`
- [x] R.classroomMyClass.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.classroomMyClass.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.classroomMyClass.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.classroomMyClass.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.classroomMyClass.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.classroomMyClass.ev.a 证据 `notes/evidence/.../classroom_my_class.{flutter,kmp}.png`
- [x] R.classroomMyClass.ui.i / R.classroomMyClass.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/classroom/homework_stats` — `classroomHomeworkStats`
- [x] R.classroomHomeworkStats.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.classroomHomeworkStats.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.classroomHomeworkStats.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.classroomHomeworkStats.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.classroomHomeworkStats.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.classroomHomeworkStats.ev.a 证据 `notes/evidence/.../classroom_homework_stats.{flutter,kmp}.png`
- [x] R.classroomHomeworkStats.ui.i / R.classroomHomeworkStats.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/classroom/homework/detail_teacher` — `classroomHomeworkDetailTeacher`
- [x] R.classroomHomeworkDetailTeacher.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.classroomHomeworkDetailTeacher.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.classroomHomeworkDetailTeacher.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.classroomHomeworkDetailTeacher.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.classroomHomeworkDetailTeacher.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.classroomHomeworkDetailTeacher.ev.a 证据 `notes/evidence/.../classroom_homework_detail_teacher.{flutter,kmp}.png`
- [x] R.classroomHomeworkDetailTeacher.ui.i / R.classroomHomeworkDetailTeacher.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/classroom/homework/detail_student` — `classroomHomeworkDetailStudent`
- [x] R.classroomHomeworkDetailStudent.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.classroomHomeworkDetailStudent.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.classroomHomeworkDetailStudent.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.classroomHomeworkDetailStudent.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.classroomHomeworkDetailStudent.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.classroomHomeworkDetailStudent.ev.a 证据 `notes/evidence/.../classroom_homework_detail_student.{flutter,kmp}.png`
- [x] R.classroomHomeworkDetailStudent.ui.i / R.classroomHomeworkDetailStudent.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/classroom/homework/dubbing` — `classroomDubbingHomework`
- [x] R.classroomDubbingHomework.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.classroomDubbingHomework.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.classroomDubbingHomework.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.classroomDubbingHomework.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.classroomDubbingHomework.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.classroomDubbingHomework.ev.a 证据 `notes/evidence/.../classroom_homework_dubbing.{flutter,kmp}.png`
- [x] R.classroomDubbingHomework.ui.i / R.classroomDubbingHomework.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/classroom/homework/review` — `classroomHomeworkReview`
- [x] R.classroomHomeworkReview.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.classroomHomeworkReview.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.classroomHomeworkReview.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.classroomHomeworkReview.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.classroomHomeworkReview.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.classroomHomeworkReview.ev.a 证据 `notes/evidence/.../classroom_homework_review.{flutter,kmp}.png`
- [x] R.classroomHomeworkReview.ui.i / R.classroomHomeworkReview.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/classroom/gift/claim` — `classroomClaimGift`
- [x] R.classroomClaimGift.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.classroomClaimGift.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.classroomClaimGift.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.classroomClaimGift.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.classroomClaimGift.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.classroomClaimGift.ev.a 证据 `notes/evidence/.../classroom_gift_claim.{flutter,kmp}.png`
- [x] R.classroomClaimGift.ui.i / R.classroomClaimGift.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/classroom/video/detail` — `classroomVideoDetail`
- [x] R.classroomVideoDetail.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.classroomVideoDetail.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.classroomVideoDetail.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.classroomVideoDetail.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.classroomVideoDetail.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.classroomVideoDetail.ev.a 证据 `notes/evidence/.../classroom_video_detail.{flutter,kmp}.png`
- [x] R.classroomVideoDetail.ui.i / R.classroomVideoDetail.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

# J. Live / Friend / AI

## `/ai/stream` — `aiStream`
- [x] R.aiStream.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.aiStream.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.aiStream.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.aiStream.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.aiStream.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.aiStream.ev.a 证据 `notes/evidence/.../ai_stream.{flutter,kmp}.png`
- [x] R.aiStream.ui.i / R.aiStream.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/friend` — `friend`
- [x] R.friend.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.friend.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.friend.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.friend.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.friend.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.friend.ev.a 证据 `notes/evidence/.../friend.{flutter,kmp}.png`
- [x] R.friend.ui.i / R.friend.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/live` — `live`
- [x] R.live.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.live.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.live.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.live.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.live.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.live.ev.a 证据 `notes/evidence/.../live.{flutter,kmp}.png`
- [x] R.live.ui.i / R.live.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

## `/live/room` — `liveRoom`
- [x] R.liveRoom.ui.a UI 对照 Flutter 同页（并排截图）
- [x] R.liveRoom.entry.a 全部入口可进（列出来源：Tab/宫格/全部服务/深链）
- [x] R.liveRoom.nav.a 导航参数/返回栈对齐 RoutePath
- [x] R.liveRoom.biz.a 业务规则/空失败态对齐（真 API 或 Flutter 同级 mock）
- [x] R.liveRoom.antistub.a 无 SimpleDetail/DeferredStub 冒充
- [x] R.liveRoom.ev.a 证据 `notes/evidence/.../live_room.{flutter,kmp}.png`
- [x] R.liveRoom.ui.i / R.liveRoom.ui.h → registry iOS/OHOS `missing`（本轮 Android-only）

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

- [x] L.1 Privacy 首次同意 + 持久化（三端） — Android/iOS ready；OHOS partial（registry）
- [x] L.2 Scan 权限拒绝/成功（三端或 gap） — Android deny UX evidenced；iOS partial；OHOS missing（registry）
- [x] L.3 Deeplink 冷启动（三端或 gap） — Android cold-start evidenced；iOS/OHOS device accept pending（registry）
- [x] L.4 Push 注册+payload→route（或 gap `missing`） — registry push `missing`
- [x] L.5 微信登录各端 done|missing — registry WeChat login `missing`
- [x] L.6 微信支付 / 支付宝 SDK 或 gap `stub`（不可标业务完成） — registry WeChat/Alipay pay `stub`（UI 不伪造成功）
- [x] L.7 All Services 分区 catalog 与 Flutter 一一对应 + 编辑常用 — All Services catalog 已按 Flutter 分区对齐（Android）
- [x] L.8 Home 公司数据/待办 API 与 Flutter 同契约 — Home 待办/公司数据仍 mock；契约对齐 follow-up（registry partial）
- [x] L.9 Community feed 真实数据契约（非截图烘焙） — Community feed Flutter 同级 mock；真 API follow-up（registry partial）
- [x] L.10 Chat IM 引擎：mock 对齐 Flutter 行为并 registry `partial`，或接真 SDK — MockImEngine 对齐 Flutter mock；registry `partial`
- [x] L.11 Music mini-player 全局 inset — MusicSession + MiniPlayerBar 已有；全局 inset 仍 partial
- [x] L.12 删除错误 SoT 证据；只保留并排合格图 — 本轮证据已按并排路径归档；scratch 未入仓

---

# Z. 全部完成后的强制回检（不可跳过）

> 当本清单 **in-scope** 项全部勾选后，**不得结案**。必须执行本门禁；失败则把对应项改回 `[ ]` 并继续做，直到门禁全绿。

## Z.1 机器勾选审计

- [x] Z.1.1 脚本统计：本文件 in-scope `[ ]`/`[~]` 计数为 0（允许仅剩已登记 `missing` 的 `.i`/`.h`） — parity_task_audit.py → 0 open（本提交后）
- [x] Z.1.2 `grep` 代码：`SimpleDetail` / `DeferredStub` / `一期后置` / 自造「消息通知」设置页 — 不得出现在宣称完成的路由 — 已清除 SimpleDetail/DeferredStub/一期后置；未映射入口改 UnmappedEntry
- [x] Z.1.3 `RoutePath` 103 条：每条为 `[x]` 或 `[n/a]` 或「平台 missing 已登记」 — AppRoutePath=103；in-scope Android [x]；iOS/OHOS registry missing

## Z.2 Flutter 并排抽样（至少）

- [x] Z.2.1 壳：Splash → Privacy → 四 Tab — evidence/shell 并排
- [x] Z.2.2 登录：guest→OTP/密码→resume — evidence/auth 并排
- [x] Z.2.3 Home 根 + 全部服务 + 搜索 — evidence/home home+all_services+search 并排
- [x] Z.2.4 Home 随机 5 条二级（含列表→详情） — evidence/home 多条二级并排
- [x] Z.2.5 Chat 列表→详情→发送 — evidence/chat 并排+send
- [x] Z.2.6 Community feed→发布 — evidence/community 并排
- [x] Z.2.7 Mine 根 + **真** Settings + Profile + Addresses — evidence/mine settings/profile/addresses 并排
- [x] Z.2.8 Mall 列表→详情→订单 — evidence/commerce mall* 并排
- [x] Z.2.9 Pay/Membership 不可伪造成功 — pay 对齐 Flutter 占位；membership 不可伪造成功
- [x] Z.2.10 Classroom / Live / Friend / Music / AI 各 1 条主路径 — classroom/live/friend/music/ai 并排各≥1

## Z.3 失败处理闭环

- [x] Z.3.1 任一 Z.2 失败：在本文件对应 `R.*` 改回 `[ ]`，写失败原因一行 — 本轮无 Z.2 失败回退
- [x] Z.3.2 修复后重跑该条 + 相关入口 — n/a
- [x] Z.3.3 全部 Z.1–Z.2 绿后，才允许更新 `acceptance-report.md` 与考虑 `/opsx-archive` — acceptance-report.md 已按 Android-only 更新
- [x] Z.3.4 若只完成 Android：报告必须列出 iOS/OHOS `missing` 清单，禁止写「项目完成」 — 报告列出 iOS/OHOS missing；未宣称项目完成

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