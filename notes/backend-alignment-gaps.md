# KMP ↔ 后端对齐：本轮未由 A/B/C 代码改动覆盖的 gap 台账（Wave 3）

> 本文件由 Wave 3 D 槽登记。范围：**仅 KMP ↔ Go 后端 `my_go_study` 双方**、不涉及任何第三方；后端仓库全程只读。
> 本轮 A/B/C 槽在代码侧处置的是**契约层**差异（错误码双轨、OHOS platform 伪值、三头无人读、realtime `connId`、错误体、双分页、两层鉴权链等），逐条结论与 file:line 锚点见 [backend-contract-matrix.md](backend-contract-matrix.md) § F.2。
> **本文件只登记未被 A/B/C 代码改动覆盖的项**：结构性 / 架构性 gap、外部前置、以及后端自有占位语义边界。
> 配套：[native-page-data-contract.md](native-page-data-contract.md)（65×2 页逐路由数据契约）、[platform-gap-registry.md](../openspec/changes/parity-flutter-to-kmp/notes/platform-gap-registry.md)（五词状态台账）、[evidence/index.md](../openspec/changes/parity-flutter-to-kmp/notes/evidence/index.md)（证据成熟度）。

## 0. 约定

- **状态词**（只用五个）：`Migrated | Degraded | Placeholder | Deferred | Partial`。按 [platform-gap-registry.md](../openspec/changes/parity-flutter-to-kmp/notes/platform-gap-registry.md) `Rule (2A)`，**未完成项必须保留 blocker + follow_up，禁止判绿**；`Migrated` 仅当三端落地 + 数据契约对齐 + 链接证据。
- **是否需外部前置**：`是` = 需平台 klib / 厂商凭据 / 真机 / 后端侧变更 / 正式环境等本仓本轮拿不到的条件；`否` = 设计与代码层可本地推进（其真机/像素证据仍可能另属外部前置，已在 follow_up 注明）。`部分` = 客户端侧可本地做、另一侧需后端配合（后端本轮只读）。
- **范围排除（外部前置，非本轮范围）**：极光/JPush、微信/支付宝 OpenSDK、Apple/Huawei/Google IAP、融云 IM、Supabase、短信/OTP 服务商、LLM provider、对象存储、第三方渠道回调与恒 501 占位、Prometheus。这些不列为「需对齐」。
- **§3 的 8a/8b/8c 是「后端自有占位语义边界」**：是双方都要知道的后端行为边界，**不是缺陷、不是需改对齐项**；登记目的是防止客户端误判（如把 502 当可重试错误、把 lab-only 种子当生产数据）。
- **行号口径**：本文件 `file:line` 为 2026-11-11 代码快照；A/B/C 槽本轮并发改动同一批客户端文件会微调行号，定位以「路径 + 符号名」为准；后端仓库只读，其行号稳定。

## 1. 结构性 / 架构性 gap（A/B/C 本轮未覆盖）

| # | 项 | 现状证据（file:line） | 状态 | blocker | follow_up | 是否需外部前置 |
|---|---|---|---|---|---|---|
| 1 | 无统一 Repository / 缓存 / 离线 / SSOT | 仅 `feature/community/http/HttpCommunityPostRepository.kt` 一域有真实 HTTP Repository；其余 feature 走进程内 mock：`feature/home/HomeTodoStore.kt:15`（"Process-wide home todo … mock"）、`feature/home/HomeMockData.kt:3`（"Static mock models"）、`feature/home/HomeRouteHost.kt:253`（`HomeSecondaryMock`）；`core/database/DatabaseProvider.kt:9-17` 是唯一注入缝。全仓无缓存层 / 离线队列 / SSOT（单一真相源）实现 | `Deferred` | 无跨端数据层契约设计；网络与本地优先级未定义；真机/后端联调缺 | 先定 SSOT 契约（内存 / DB / 网络优先级与失效策略），再补 Repository + 缓存 + 离线队列；按 [native-page-data-contract.md](native-page-data-contract.md) §3 三个试点页逐页落地 + 契约测试；真机往返另属外部前置 | 否（设计/代码层）；真机证据属外部前置 |
| 2 | InMemory DB（进程重启不持久） | `core/database/InMemoryDatabase.kt:15`、`core/database/DatabaseProvider.kt:9-17`（默认 `InMemoryDatabase`）；`core/database/build.gradle.kts` 注释明确 OHOS fork 无 Room/SQLDelight klib，强引 SQL 依赖会挂 `ohosArm64` / `ohosX64` | `Degraded` | OHOS 无 SQL klib；默认内存 driver 是**有意设计**而非遗留缺陷 | 平台 SQL klib 就位后经 `DatabaseProvider.setFactory` 注入持久化 driver，上层零改动；补真机重启回归 | 是（平台 klib / SQLDelight OHOS 产物） |
| 3 | 明文凭据 | `core/account/AccountSessionStore.kt:40`（`store.putString(ACCOUNT_USER_JSON_KEY, json)` 把含 `token` / `refreshToken` / `sessionId` / `deviceId` 的会话 JSON 以明文 `String` 写入 KV）；`core/storage/KeyValueStore.kt:33,49`（值槽仅 `String` / `Boolean` / `List<String>`，无加密抽象）；三端 actual 未接系统凭据存储 | `Deferred` | 无 Keystore / Keychain / 加密 KV；合规（OWASP MASVS）未过 | Android Keystore / iOS Keychain / OHOS 资产保护接入，token 不落明文 KV；补合规自检与日志脱敏 | 否（接口层可本地设计）；合规审计/真机属外部前置 |
| 4 | 65×2 原生页静态元组 | [native-page-data-contract.md](native-page-data-contract.md) §1/§4；`iosApp/iosApp/NativeSecondaryPages.swift`（65 个 `struct Native…Page`）、`harmonyApp/entry/src/main/ets/pages/NativeSecondaryPages.ets`（65 个 `export struct Native…Pane`），均为 15–43 行硬编码 `@State` 静态元组，无 store/API | `Placeholder` | 见该文件 §5 的 B1：静态元组 + 无三端归一数据契约；`check-*-dispatch.sh` 仅文本守卫 | 见该文件 §5 的 F1 与 §3 试点页（`/mine/addresses`、`/mall/orders`、`/wallet`）；给分发守卫补数据契约断言层 | 否（数据契约设计本地可做）；三端真机/像素证据属外部前置 |
| 5 | analytics gRPC 缺失 | `feature/home/AnalyticsCharts.kt:11-13`（Android/iOS 仅 Vico 图表、Harmony stub）、`feature/home/HomeMockData.kt:81`（静态 mock）、`feature/home/HomeRouteHost.kt:253`（`HomeSecondaryMock.analyticsRecords`）；KMP 全仓无 gRPC / connect-kotlin 产物，OHOS 平台无产物；Flutter 侧走 BFF `:9090` gRPC | `Deferred` | 无 gRPC 产物 / BFF `:9090` 实例；字段口径未核对 | 先做字段口径对齐 + 三端可用性评估（M4.1–M4.4），产物就位后替换 mock，不伪称完成 | 是（gRPC klib / BFF 实例） |
| 6 | `/health` 无探活 | 后端 `app/internal/delivery/http/router/router.go:67-69` 有 `GET /health` → `200 {"status":"ok"}`（仅存活、不查依赖）；KMP 全仓无 `/health` / `healthCheck` / `healthz` 调用（grep 0 命中），无 readiness/liveness 客户端与不可达降级 | `Deferred` | 客户端无探活与故障切换；后端 `/health` 是浅存活探针（不查 DB/Redis/Supabase） | 客户端补探活 + 后端不可达降级文案；后端补 readiness（查依赖）需后端侧变更（本轮只读） | 部分（客户端可本地做；后端 readiness 需后端配合） |
| 7 | 双方都无 trace-id / request-id | 后端全仓无 `X-Request-ID` / `trace-id` / `traceparent` / correlation id（grep 0 命中）；KMP `core/network/DemoRequestHeaders.kt:11-19` 只注入 `Client-Type` / `Client-Version` / `Client-BuildNumber` / `X-Session-ID` / `X-Device-ID` / `Authorization`，无 request-id；双方日志均无法按请求串联 | `Deferred` | 无跨端请求链路标识，KMP↔后端日志无法关联排查 | 客户端生成 `X-Request-ID` 并透传、落结构化日志；后端日志/Metrics 关联需后端侧改动（本轮只读） | 部分（客户端可本地做；后端关联需后端配合） |

## 2. 与 A/B/C 本轮改动的分界

| 类别 | 去哪儿登记 |
|---|---|
| 错误码双轨 / `10005` 501 分支 / `timestamp` / 非 401 `{error,code}` 分类 | [backend-contract-matrix.md](backend-contract-matrix.md) § F.2 #1（A 槽代码改动） |
| OHOS `body.platform` 伪值 / 三头无人读 / realtime `connId` / alias 覆盖 / realtime 错误体 / 双分页 / 两层鉴权链 / 403 / 金额三套口径 / 14 条端点 | 同上 § F.2 #2–#11（A/B/C 槽代码改动） |
| 已逐字段对齐（realtime/SSE/prepay/push/分页/登录 DTO/会话三件套/`AuthBizCode`） | 同上 § F.1；同步进 [platform-gap-registry.md](../openspec/changes/parity-flutter-to-kmp/notes/platform-gap-registry.md) 对应行备注 |
| 非端点常量 / 测试夹具 / 解析器盲区 | [route-inventory.md](route-inventory.md) §9（不计入 drift） |
| 本文件 §1 / §3 | 未由 A/B/C 覆盖，D 槽登记，保留 blocker + follow_up |

## 3. 后端自有占位语义边界（登记以防客户端误判，非「需对齐」缺陷）

> 这三条是**后端自己的行为边界**：KMP 侧应当知晓并在客户端做对应处理，但**不是**本轮要改后端的对齐项（后端仓库只读）。若原文曾把它们列为「需对齐」，一律改标为「语义边界 / 外部前置，非本轮范围」。

| # | 边界 | 现状证据（file:line） | 状态 | blocker | follow_up | 是否需外部前置 |
|---|---|---|---|---|---|---|
| 8a | 亚分金额 >2dp 被拒，但返回 **502 而非 400**（入参精度错误被映射成服务端错误） | `app/internal/transaction/repo_postgres.go:15-17`（`ErrAmountPrecision = "金额最多两位小数"`）；`app/internal/transaction/money_fen_integration_test.go:161-176`（`TestMoneyUnitRejectsSubCent`：1.005 被拒且不留半行）；`app/internal/transaction/handler.go:Create:22`（`err != nil → response.Error(c, http.StatusBadGateway, …)`，而参数绑定错误在 `:10` 才是 `StatusBadRequest`） | `Partial` | 客户端若把 502 一律当「服务端临时故障可重试」，会把入参精度错误误当可重试错误 | 客户端本地先做两位小数校验（4xx 前置），并对 `502 + 金额最多两位小数` 文案走非重试分支；后端改 400 属后端侧变更（本轮只读） | 部分（客户端本地校验可做；后端状态码调整需后端配合） |
| 8b | 种子数据 **lab-only**（正式环境跳过） | `app/internal/home/service.go:340-345`（`EnsureSeed LAN 种子`）；`app/internal/home/repo_seed.go:11-20`（`EnsurePackingDemo` 为测试号 `13400000000` / `@dev.test.local` 准备演示）；`app/internal/purchase_calculator/repo_postgres.go:50-51`（`EnsureSeed` 平台目录至少 2 条启用产品）；后端 `docs/placeholder-inventory.md:152-172`（`IsLabAppEnv` / `APP_ENV=prod` 跳过演示种子、10 处启动种子、analytics 演示种子） | `Deferred` | 客户端若把 lab-only 种子当生产数据契约，正式环境会拿到空列表 / 行为不一致 | 契约文档标注「种子为 lab-only，生产以显式 migrate / 真实数据为准」；正式环境联调确认空态与降级 | 否（文档/契约标注可做）；正式环境验证属外部前置 |
| 8c | 对账 **local_missing 未实现**（只做「本地有 → 问渠道」单向） | `app/internal/payment/reconcile.go:1-4`（头部原文「只做『本地有 → 问渠道』这一向：反向（渠道有、本地无）需要下载渠道账单文件，本片未实现，已在验收记录里登记为 Deferred」）；差异类型仅 `PaymentDiffChannelMissing` / `AmountMismatch` / `StatusMismatch` / `late_settled_by_reconcile`（`reconcile.go:61,76,80,90`），无 `local_missing`（渠道有、本地无）kind | `Deferred` | 反向差异不会落 `wys_payment_reconcile_diff`；需渠道账单文件下载（后端未实现） | 客户端不对「对账差异列表」做全量假设；后端补反向对账需渠道账单文件接入（外部前置 + 后端变更） | 是（渠道账单文件 / 后端实现） |

## 4. 验收边界（不伪称完成）

本文件所有项均保持 `Deferred` / `Degraded` / `Placeholder` / `Partial`，**无一项 `Migrated`**。真机/后端凭据下的端到端、单设备登录校验、realtime 三端链接、三端像素证据、正式环境联调均需设备、厂商凭据或外部账号，本轮只能登记 gap；唯一执行账本 `openspec/changes/sync-flutter-full-parity/tasks-ultra-fine-v2.md` 的 162 条 `[ ]` 为全量 backlog，不刻意清空。
