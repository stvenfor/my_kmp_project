# OpenSpec 同步剩余任务实施计划

> 生成：2026-11-05 · 范围：`openspec/changes/sync-flutter-full-parity` + `openspec/changes/parity-flutter-to-kmp` 的未完成项
> 本文只做计划，不改代码。执行由其它智能体编排；本文给出可独立领取的任务、依赖、验收命令与证据要求。

## 0. 先看结论

1. **任务清单已落后于代码。** `tasks-ultra-fine-v2.md` 的 M0（回退单 `:composeApp`）已被后续真实的模块拆分取代；M1/M2/M3/M8/P1–P5 大部分已落地。继续按清单原样执行会重复劳动。
2. **当前静态门禁是绿的（本次实测）**：Kotlin 词法扫描 OK、layer gate 0 违规、iOS/OHOS 原生页分发 65/65、Flutter baseline 对齐 `d7876a1`。
3. **但验收门禁存在假绿。** `scripts/parity_task_audit.py` 只审计 `tasks-execution.md`（585 done / 0 open），不读 v2/v1。它的 `PASS` 不代表剩余任务清零。
4. **真正的剩余集中在四类**：账本纠偏、契约与路由 drift、已有核心能力的消费方接线、外部 SDK 与设备/像素验收。建议先做 `W0`（约 1 天），否则多智能体会按三份互相矛盾的清单并行改同一片代码。

---

## 1. 本次勘察基线（实测）

### 1.1 仓库与工作区

| 项 | 结果 |
|---|---|
| HEAD | `8e3027a fix` |
| Flutter SoT baseline | `d7876a1`，与 `notes/flutter-baseline.md` 对齐 |
| 未提交 | `scripts/with-gradle-lock.sh` 仅文件模式 100644 → 100755 |
| 未跟踪 | `.archify/`、`docs/deep-dive/` |
| 模块现状 | `:composeApp` + 10 个 `:core:*`（含 `:core:database`）+ 17 个 `:feature:*` + `:component` + `:ohosAggregate` |

### 1.2 门禁与检查（2026-11-05 实跑）

| 检查 | 命令 | 结果 | 含义 |
|---|---|---|---|
| 静态门禁 | `bash scripts/verify-parity.sh --static-only` | OK | Kotlin 词法 + layer gate 通过 |
| 分层门禁 | `bash scripts/check-layer-deps.sh` | OK | 无 feature→feature、无下层反向 import、无 feature 模块互依赖 |
| Harmony 分发 | `bash scripts/check-harmony-pane-dispatch.sh` | OK | 65 定义 = 65 映射 = 65 渲染 |
| iOS 分发 | `bash scripts/check-ios-page-dispatch.sh` | OK | 65 定义 = 65 渲染；30 条显式 out-of-scope |
| 任务审计 | `python3 scripts/parity_task_audit.py` | PASS | **只读 `tasks-execution.md`，假绿** |
| Flutter drift | `bash scripts/audit-flutter-drift.sh` | **exit 1** | 4 轴有 drift：endpoint 1 missing / 3 extra；route 7 missing / 40 extra |
| 单测 | 上次记录 2026-10-17 | **660 tests / 68 类 / 0 失败** | 本次未重跑；任何改动后必须重跑 |

### 1.3 已落地、清单仍标未做的能力（抽样核实）

| 能力 | 代码证据 | 清单常见误标 |
|---|---|---|
| core 模块下沉 | `core/{base,analytics,storage,router,network,account,design,platform,ui,database}` 均在 `settings.gradle.kts` | v2 M0 / P2 |
| feature 拆分 | `feature/` 17 个模块，`check-layer-deps.sh` 0 违规 | v2 M0 / P4 |
| OHOS 持久化 | `core/storage/KeyValueStore.ohos.kt` 走 `OH_Preferences`，无 `/tmp` actual | v2 M8.5、registry 旧行 |
| token refresh | `AuthRefreshingApiClient` + `AuthTokenRefresh` + `AuthSessionBootstrap` | registry 旧行 |
| realtime 核心 | `AppRealtimeClientImpl`、`RealtimeStartupGate`、`RealtimeSeqStore`、`RealtimeOutboundQueue` 及 9 个测试类 | v2 M2 仍全 `[ ]` |
| SSE 核心 | `SseParser`、`SseTransport`、`AiSseApi` 及 `AiSseApiTest` | v2 M3 仍全 `[ ]` |
| API 契约 | 93 条 Kotlin 路径，覆盖 home/mall/im/pay/wallet/classroom 等 | v2 M1 大量未勾 |
| 原生分发 | iOS/OHOS 65 个专属页面与分发 | v1 W5.i/W6.h 状态未回写 |

### 1.4 真实剩余（未落地或不完整）

| 领域 | 现状 | 证据 |
|---|---|---|
| 三端原生页数据契约 | 抽样页面仍为**本地静态元组**，未发现共享数据契约/真实 API | `iosApp/iosApp/NativeSecondaryPages.swift`、`harmonyApp/.../NativeSecondaryPages.ets` |
| realtime 消费方接线 | 核心与测试齐全，但生产侧未构造，`feature/live`、`feature/chat` 未接 | 全仓 grep 仅定义与测试，无 `AppRealtimeClient` 生产构造点 |
| AI SSE UI | `AiSseApi` 已实现，`AiStreamScreen` 仍 mock/toast | `feature/ai/AiStreamScreen.kt` |
| Push | `StubPushBridge` + 三端 `PlatformPushSdk` 均为占位 | `component/push/PushBridge.kt` |
| 支付/IAP | 通道 adapter 全为 `null`；sandbox 默认开启；IAP `finishPurchase` 缺失 | `component/pay/PayGateway.kt`、`feature/pay/PayApi.kt` 的 `GAP(R5)` |
| 微信登录 | 无 SDK；Flutter `appId` 也为空 | registry `WeChat login missing` |
| 融云 IM | 仅 mock；OHOS 产物未确认 | `feature/chat/RongImConfig.kt` |
| OHOS 相机/Web/MediaKit | 相机 missing；Web partial；播放器 stub | registry + `MediaPlayer.ohos.kt` |
| image_picker / 语音 / 防截屏 / connectivity | 全仓无实现 | 搜索无命中 |
| gRPC analytics | 无产物；图表仍 mock/Vico | registry |
| 关系型存储 | `core:database` 默认 `InMemoryDatabase`，无 SQL driver | `core/database` |
| X-Session-ID / X-Device-ID | `DemoRequestHeaders` 只注入 `Authorization` 等 | `UserAuthHttpApi.kt` 的 `GAP(R5)` |
| 设备/像素验收 | 截图门禁脚本存在但未恢复为硬门禁；本机缺 `numpy`/`Pillow`（实测 `ModuleNotFoundError`）；三端证据不全 | `scripts/screenshot_diff_gate.py`、`notes/evidence/` |

---

## 2. 口径与真相源优先级（重要）

并行编排前先固定以下优先级，避免三份清单互相覆盖：

1. **代码 + 可复跑门禁 + Flutter `d7876a1` SoT** —— 最高。
2. **`notes/platform-gap-registry.md`** —— 能力状态的唯一登记表（必须先纠偏）。
3. **`sync-flutter-full-parity/tasks-ultra-fine-v2.md`** —— 本轮执行账本（但需按本文 W0 重写状态）。
4. **`tasks-ultra-fine.md` / `parity-flutter-to-kmp/tasks-fine.md` / `tasks-execution.md`** —— 历史账本，只用于查漏，不再直接驱动执行。

明确作废/已完成、不要再排期的条目：

| 条目 | 处置 | 理由 |
|---|---|---|
| v2 M0.1–M0.12 | 标记 `superseded` | 模块拆分已按新架构完成，HEAD 已越过当时的 staged 积压 |
| v2 P1–P5 | 标记完成 | `settings.gradle.kts`、根 `subprojects {}`、`:core:design` 资源、KV、17 feature 均已在册 |
| v2 M13.2 | 已完成 | layer gate 0 违规 |
| v2 M13.11 | 已完成 | `AGENTS.md` 地图已反映 28 模块 |
| v2 M8.5 | 已完成 | OHOS 改为 `OH_Preferences` |
| v1 W5.i / W6.h | 部分完成 | 页面与分发已闭环，但数据仍静态 → 转入 `W3` |
| registry 的 OHOS `/tmp`、token refresh、in-scope RoutePath 旧 caveat | 需按代码纠偏 | 见 `W0.2` |

---

## 3. 工作分解

### W0. 账本与门禁收口（阻断，先做）

目标：让“剩余任务”只有一份真相，且门禁不再假绿。无外部依赖，建议单批完成。

| ID | 任务 | 验收 |
|---|---|---|
| W0.1 | 指定 `tasks-ultra-fine-v2.md` 为唯一执行账本；在 v1/v2/`tasks-fine.md` 顶部写明状态与作废关系 | 三份文件不再互相矛盾；`grep` 能看到唯一主账本 |
| W0.2 | 纠偏 `platform-gap-registry.md`：OHOS 持久化、token refresh、SSE（传输已落/UI 未接）、realtime（核心已落/未接线）、in-scope RoutePath（页面 ready ≠ 数据 ready）；合并重复的 `push entry` 两行 | 每行都有代码证据；不把“页面可开”写成“能力 ready” |
| W0.3 | 建立证据索引：`notes/evidence/index.md`，按模块/平台/路由登记已有证据与缺口 | 证据目录可被审计；无悬空链接 |
| W0.4 | 修复 `scripts/parity_task_audit.py`：支持主账本路径（参数或常量），并扫描 v2 的 `[ ] / [~]`；先作为本地/夜间/归档门槛，主账本清零后再接入阻塞式 CI | 审计在真实剩余 > 0 时失败；不再只审计 585 条旧账 |
| W0.5 | 门禁补齐：把 `check-harmony-pane-dispatch.sh`、`check-ios-page-dispatch.sh` 接入 `verify-parity.sh`；`audit-flutter-drift.sh` 在 W1 清零后接入为硬门禁 | `verify-parity.sh` 能拦住分发回退；drift 清零后 CI 可拦住路由回退 |
| W0.6 | 工作区收口：提交 `with-gradle-lock.sh` 模式变更；明确 `.archify/`、`docs/deep-dive/` 是提交还是 gitignore | `git status` 干净或只剩明确忽略项 |

**W0 出口门禁**

```bash
bash scripts/verify-parity.sh --static-only
bash scripts/check-harmony-pane-dispatch.sh
bash scripts/check-ios-page-dispatch.sh
python3 scripts/parity_task_audit.py   # 此时应反映真实剩余，不得直接 PASS
```

---

### W1. 契约与路由 drift 清零

目标：让 `audit-flutter-drift.sh` 归零（或只剩有书面 allowlist 的条目），并把契约补齐到 Flutter SoT。

| ID | 任务 | 现状/证据 | 验收 |
|---|---|---|---|
| W1.1 | 补 `POST /api/v1/payments/prepay` 契约与 `PayApi` 接线 | drift：Kotlin 缺此条；v2 M1.5.3 | 契约测试 + 调用方；drift 归零 |
| W1.2 | 处置 3 条 Kotlin extra endpoint | `/api/v1/mall`（基础常量）、`/api/v1/user/huawei/login`（v2 M1.0.5）、`/im/friends/requests/:v/respond`（审计拼接 artifact） | 每条二选一：删除 / 登记为产品确认的扩展 / 修审计归一化 |
| W1.3 | 路由 drift：6 条 `dubbing` vs KMP `transliter` + 1 条 `homework_stats` 审计归一化 | `/classroom/homework/dubbing`、`/home/dubbing_feed`、`/video/dubbing/*`；`/classroom/homework_stats` 同时出现在 missing/extra | 决定“别名映射/统一改名”，同步 Flutter 映射表、Kotlin 常量、route inventory、原生分发脚本；修审计的常量归一化 |
| W1.4 | 40 条 Kotlin extra route 分类 | 内部根路径/别名（`/app`、`/splash`、`/privacy/consent`、模块根）vs 真实新增产品页 vs Flutter toast-only | 分类表入 `notes/route-inventory.md`；不明确的产品页找产品确认 |
| W1.5 | 复核 v2 M1.0 的 6 条自造路径 | `mine/profile`、`mine/invoice`、`chat/list`、`home/feed`、`user/huawei/login` 等 | 每条有“已对齐/明确本地 mock/已删除”结论 |
| W1.6 | 复核 M1.1 网络能力 | GET/PUT/PATCH/DELETE、分页、路径参数、错误码映射 | 已有测试覆盖或补测；契约快照不漂移 |
| W1.7 | 双仓 drift 巡检脚本固化 | `scripts/audit-flutter-drift.sh` 已存在，当前 exit 1 | 接入 `W0.5`；baseline/allowlist 有注释 |

**W1 出口门禁**

```bash
bash scripts/audit-flutter-drift.sh          # exit 0
bash scripts/verify-parity.sh --static-only
```

> 注意：W1.3 的“统一改名”会同时触碰 Flutter 映射、Kotlin 常量、iOS/ArkTS 分发与 route inventory，必须单批完成，禁止只改一侧。

---

### W2. 已有核心能力的消费方接线

目标：核心层已经写好但没人用的能力，真正进入 App。每项都先写“接线点在哪”，再改。

| ID | 任务 | 现状 | 接线点 | 验收 |
|---|---|---|---|---|
| W2.1 | realtime 生产实例 | `AppRealtimeClientImpl` / `RealtimeStartupGate` 仅测试使用 | `composeApp/.../app/AppContainer` 或 `ShellBootstrap` 建唯一实例；经 `:component` 端口注入 `feature/live`、`feature/chat` | 登录后建连；登出断开；live 弹幕/在线人数、chat 收推使用真实入站路由 |
| W2.2 | realtime 持久化 | v2 P3 备注：`RealtimeKeyValueStore` 生产侧无构造点 | 用 `core.storage.KeyValueStore` 实现序号/离线游标存储 | 重启后序号不丢；契约测试 |
| W2.3 | connectivity | 无跨端实现 | `expect/actual` 或各端原生注册；realtime 闸门消费 | 网络恢复触发重连；断网退避可测 |
| W2.4 | AI SSE UI | `AiSseApi` 完成，`AiStreamScreen` 仍 mock | `feature/ai` 屏幕接 `streamCompletion`：增量、停止、错误气泡、多轮 | 单测 + Android 证据；OHOS arm64 实测；x64 登记 gap |
| W2.5 | X-Session-ID / X-Device-ID | `DemoRequestHeaders` 只注入 Authorization | 扩展逐请求头能力，认证/IM 建立后注入；移除 `UserAuthHttpApi` 的 GAP | 回归 `AuthHttpDecisionTest`；契约测试断言三头 |
| W2.6 | 关系型存储决策 | `core:database` 默认内存 | 先出评估：SQLDelight（三端产物/OHOS 验证）vs KV 降级 vs 登记 gap | 决策文档 + 选择后的最小实现/明确 gap |
| W2.7 | 环境系统复核 | `NetEnvironment` + 设置入口已有 | 对齐 Flutter 3-flavor、`.env`、`USE_MOCK_IM`、LAN/loopback | `BackendHostPolicyTest` 扩三 flavor 矩阵；设置切换语义明确 |
| W2.8 | Push 接线前置 | `PushRegistrationApi` 已有；`StubPushBridge` 未接真实 token | 先完成 token 上报链路与点击 deeplink（SDK 本体归 W4） | 点击 payload → 路由可测；token 上报 mock 可验收 |

**W2 出口门禁**

```bash
./gradlew testDebugUnitTest
./gradlew :composeApp:compileDebugKotlinAndroid
./gradlew compileKotlinIosX64 compileKotlinIosArm64 compileKotlinIosSimulatorArm64
./gradlew compileKotlinOhosArm64 compileKotlinOhosX64
```

---

### W3. 三端原生页数据契约（M10.B，最大产品面缺口）

目标：把 iOS/ArkTS 的 65 × 2 个“静态元组页”升级为**有真实数据、空态、错误态**的页面，同时不破坏 ADR 0002/0003 的所有权（非 Mine 页走原生壳，Mine 二级走 Compose 岛）。

**现状证据**：`NativeUsedCarPage` 等页面直接内联 `orders` 元组；`FeaturePage` 只提供标题/行/主按钮；Kotlin 侧没有跨语言数据出口。也就是说当前“页面可开”但“数据不可信”。

**先决条件**：本段必须先产出一页 ADR/设计说明，再批量改。禁止 65 页 × 2 端手工复制同一份 mock。

| ID | 任务 | 说明 | 验收 |
|---|---|---|---|
| W3.1 | 数据契约 ADR | 在 `docs/adr/` 或 `openspec/.../notes/` 定方案：Kotlin 领域数据 → JSON schema → Swift/ArkTS；声明字段、空态、错误、分页、缓存 | ADR 通过评审；字段表可被三端共用 |
| W3.2 | 出口 seam | 在 `:composeApp` 暴露统一数据桥（参考 `AuthBridge` 的导出模式）；不得让 feature 互相依赖，不得让 `:core` 反向 import | 一个入口按 route/path 返回 JSON；`libkn.so` / iOS framework 仍由 `:composeApp` 产出 |
| W3.3 | Home 族（28 路由） | 列表/详情/创建接 M1.2–M1.3 契约；替换原生静态元组 | 三端同路由同字段；空态/错误态；Android 截图 |
| W3.4 | Mall / Mine / Wallet 族 | 接 M1.4–M1.5；`$orderId` 参数化 | 同上 |
| W3.5 | Community / Chat / Friend / Live | 接 M1.4.1 / M1.6 / M2 / M6；ArkTS 移除本地 chat mock（v2 M10.B.10） | 同上；realtime/IM mock 边界清晰 |
| W3.6 | Classroom / Media / Music | 接 Classroom 数据源与 M2/M3；OHOS 播放器 gap 显式 | 同上 |
| W3.7 | 分发守卫扩展 | 在现有 65/65 基础上断言“每个 Native 页面有数据 seam 或显式 static-fixture 标记” | 新增页面不能静默落回静态 tuple |

**W3 出口门禁**

```bash
bash scripts/check-harmony-pane-dispatch.sh
bash scripts/check-ios-page-dispatch.sh
./gradlew testDebugUnitTest
```

> 允许分阶段：先 Home / Mall / Mine / Chat 四个高价值族，再补齐剩余。每族完成即更新 `route-inventory.md` 与 `platform-gap-registry.md`，不要等全量结束。

---

### W4. 外部 SDK 与平台能力闭环

目标：把 registry 里的 `missing/stub/partial` 逐条变成 `ready`，或在产品确认后保持 `n/a`。**没有凭据/设备时只允许登记 gap，禁止伪称 ready。**

| ID | 能力 | 当前 | 外部前置 | 完成判据 |
|---|---|---|---|---|
| W4.1 | 融云 IM（Android/iOS/OHOS） | mock 对等 | AppKey；OHOS 融云产物确认 | `ChatPort` 真实现 + 三端连接/收发/已读/补拉；保留 mock gating |
| W4.2 | 微信登录 | missing | WeChat OpenSDK、appId、Universal Link | `/user/wechat/login` 全链路；三端 |
| W4.3 | 微信/支付宝支付 | adapter null；sandbox 默认开 | 商户号、证书、OpenSDK/tobias | 真通道三端；release 关闭 sandbox；不得假 Success |
| W4.4 | Apple / Huawei / Android IAP | 验单 HTTP 已落，`finishPurchase` 缺 | App Store Connect / AGC / Play Console 配置 | 购买-验单-完成事务闭环；三端 |
| W4.5 | Push（FCM/APNs/JPush/OHOS Push） | 全 stub | 各厂商 key/证书 | 注册、token 上报、点击 deeplink；三端 |
| W4.6 | OHOS 相机/扫码 | missing | 真机 + 权限配置 | 授权/拒绝 UX；扫码返回；gap 清零 |
| W4.7 | OHOS WebView | placeholder | OHOS Web 组件 | 真实加载 + JS bridge + 离线 fixture |
| W4.8 | OHOS MediaKit 播放器 | stub | 真机 + MediaKit | 音频/视频播放；错误态 |
| W4.9 | image_picker | missing | 三端权限与选择器 | 发布/头像/发票上传可用 |
| W4.10 | 语音录制/播放 | missing | 录音权限；OHOS 新 cinterop def | 按住说话、时长/波形、气泡播放 |
| W4.11 | 屏幕防截取 | missing | 各端窗口 API | 生效范围与 Flutter 一致；可关闭 |
| W4.12 | analytics gRPC | missing | BFF `:9090`；OHOS 产物评估 | 字段口径对齐；不可用处登记 gap |
| W4.13 | 关系型存储驱动 | 内存 | SQLDelight/OHOS 验证 | W2.6 决策落地 |

**W4 规则**

- 每个能力单独 PR/提交：接口（`commonMain`）→ 三端 actual → registry 行 → 证据。
- 任一平台未 ready：registry 保持 `partial/missing`，验收不得写“完成”。
- 支付/推送/IM 的 key 只放本地配置或 CI secret，绝不入库。

---

### W5. 设备、像素与验收收口

目标：把“代码完成”变成“可审计的完成”。

| ID | 任务 | 验收 |
|---|---|---|
| W5.0 | 截图门禁运行环境 | 安装 `numpy` + `Pillow`；把依赖写入脚本说明或 `requirements-parity.txt`，避免 `ModuleNotFoundError` |
| W5.1 | 恢复 Android 截图 diff 硬门禁 | `scripts/screenshot_diff_gate.py --flutter ... --kmp ... --out-dir ... --route ...` 对 in-scope 路由运行，MSE ≤ 2.0；证据入 `notes/evidence/` |
| W5.2 | 三端 open-path 验收 | Android 模拟器/真机、iOS Simulator、Harmony 模拟器/真机；含 Soft Auth 路径 |
| W5.3 | 每路由证据 | 按 `notes/evidence/{module}/{Platform}/{route}.md` 落截图/差异/结论；与 `route-inventory.md` 状态同步 |
| W5.4 | 状态回写 | `route-inventory.md` 从 `partial/stub` 推进到 `ready` 或保留 gap；registry 同步 |
| W5.5 | 验收矩阵 | `acceptance-matrix.md` 全量勾选；未完成项必须指向 registry 行 |
| W5.6 | iOS/OHOS 采样策略 | 按 ADR 0003：open-path + sampling，不要求与 Android 同一硬门禁 |

**W5 出口门禁**

```bash
# 以 Android 为例；iOS/OHOS 按 scripts/run-*.sh
./scripts/run-android.sh

# 截图门禁（W5.0 安装 numpy/Pillow 后）
python3 scripts/screenshot_diff_gate.py \
  --flutter <flutter-sot.png> \
  --kmp <kmp-android.png> \
  --out-dir openspec/changes/parity-flutter-to-kmp/notes/evidence/<route>/Android \
  --route <route> --bar 2.0
```

---

### W6. 文档、流程与归档

| ID | 任务 | 验收 |
|---|---|---|
| W6.1 | 双仓编码流程入口对齐 Flutter（v2 M14.1） | KMP 侧文档可执行；含 drift 巡检步骤 |
| W6.2 | Flutter ADR 摘要入库（v2 M14.2） | 0003/0005/0006/0007/0008/0009/0012 摘要，标注“禁止移植的废弃件” |
| W6.3 | `AGENTS.md` 地图复核 | 模块数、source set、门禁命令与代码一致 |
| W6.4 | OpenSpec 归档判定 | 不满足“in-scope 全 ready 或 gap 有登记”不得 archive |

---

## 4. 依赖与建议波次

```text
Wave A（无外部依赖，先做）
  W0 账本/门禁 → W1 契约/路由 drift
  可并行：W0.6 工作区收口、W1.7 巡检脚本

Wave B（核心能力接线）
  W2.1 realtime → W2.2 持久化 → W2.3 connectivity
  W2.4 AI SSE
  W2.5 请求头 → W2.7 环境复核
  W2.6 存储决策
  W2.8 Push 前置
  可与 W3.1 ADR 并行

Wave C（产品面数据）
  W3.1 ADR → W3.2 seam → W3.3 Home → W3.4 Mall/Mine/Wallet
  → W3.5 Community/Chat/Friend/Live → W3.6 Classroom/Media/Music
  每族完成即进入 W5 证据

Wave D（外部 SDK，按凭据到位情况并行）
  W4.1 IM / W4.2 微信登录 / W4.3 支付 / W4.4 IAP
  W4.5 Push / W4.6–W4.8 OHOS 能力 / W4.9–W4.13 其余
  无凭据时只登记 gap，不阻塞 Wave A–C

Wave E（验收收口）
  W5 全量 → W6 文档/归档
```

**关键依赖**

- `W3` 依赖 `W1`（字段口径稳定）与 `W2.1–W2.4`（realtime/SSE 数据源）。
- `W4` 依赖 `W0.2`（registry 可写）与对应凭据/设备。
- `W5` 依赖 `W3` 与 `W4`；不允许在数据仍是静态 tuple 时宣称像素验收通过。
- `W0.4` 必须在 `W1` 之前或同批完成，否则主账本仍不可信。

---

## 5. 单项完成定义与证据规范

每个任务卡（供编排器拆分）至少包含：

```text
ID / 标题
目标（一句话，可验收）
改动面（模块 / 文件 / source set）
前置（依赖 ID、外部凭据、设备）
实施约束（R1–R8、feature 互不依赖、common-first、immersive insets）
验收命令
证据路径
失败/回滚方式
```

**单项 Done 必须同时满足**

1. 代码在正确 source set；未引入 feature→feature 依赖。
2. 相关目标可编译：Android + iOS + OHOS（不能只跑 Android）。
3. 单测/契约测试通过；新行为有测试或可复现脚本。
4. `route-inventory.md` 与 `platform-gap-registry.md` 在同批更新。
5. 证据落到 `notes/evidence/...`，不是聊天里的一句“已完成”。
6. 未把 mock/sandbox/占位写成 `ready`。

**验收命令速查**

```bash
# 静态 + 分层
bash scripts/verify-parity.sh --static-only
bash scripts/check-layer-deps.sh

# 原生分发
bash scripts/check-harmony-pane-dispatch.sh
bash scripts/check-ios-page-dispatch.sh

# 账本 / drift
python3 scripts/parity_task_audit.py
bash scripts/audit-flutter-drift.sh

# 全量本地验证（耗时，合并前跑）
bash scripts/verify-parity.sh

# 单测
./gradlew testDebugUnitTest

# 五目标编译
./gradlew :composeApp:compileDebugKotlinAndroid \
  compileKotlinIosX64 compileKotlinIosArm64 compileKotlinIosSimulatorArm64 \
  compileKotlinOhosArm64 compileKotlinOhosX64

# OHOS 产物
./gradlew :composeApp:publishDebugBinariesToHarmonyApp
```

---

## 6. 外部前置与阻塞清单

以下项无法仅靠代码完成，必须由用户/产品提供，否则只能登记 gap：

| 阻塞项 | 影响 | 需要 |
|---|---|---|
| 融云 AppKey（Flutter 默认空） | W4.1 | AppKey；OHOS 融云 SDK 产物确认 |
| 微信 appId / Universal Link | W4.2、W4.3 | 开放平台配置 |
| 微信/支付宝商户号、证书 | W4.3 | 商户配置；沙箱/生产区分 |
| Apple IAP 产品与 App Store Connect | W4.4 | 产品 ID、共享密钥、测试账号 |
| Huawei IAP / AGC | W4.4 | AGC 应用与商品配置 |
| Push 厂商 key（FCM/APNs/JPush/OHOS Push） | W4.5 | 各平台证书/token |
| Go BFF 可达地址与测试账号 | 全链路验收 | `:8080` HTTP、`:9090` gRPC；Supabase 注册配额问题 |
| macOS Xcode | iOS 整包/证据 | 本机或 CI runner |
| DevEco + Harmony 真机 | OHOS 整包/相机/Web/MediaKit | 设备与签名 |
| image_picker 权限配置 | W4.9 | 三端 Manifest/Info.plist/module.json5 |
| gRPC OHOS 产物 | W4.12 | 评估结论；无产物则保持 gap |

---

## 7. 建议立即起手（前 10 项）

按依赖顺序，适合先派给一个或两个智能体：

1. `W0.1` 固定唯一执行账本，标注 v1/v2/`tasks-fine` 关系。
2. `W0.2` 纠偏 registry（OHOS 持久化、realtime/SSE、RoutePath 数据 caveat、重复 push 行）。
3. `W0.4` 修 `parity_task_audit.py` 只读旧账本的问题。
4. `W0.6` 收口工作区（模式变更、`.archify`/`docs/deep-dive` 决策）。
5. `W1.1` 补 `/api/v1/payments/prepay`。
6. `W1.2` 处置 3 条 extra endpoint；同时修审计 `:v` 归一化。
7. `W1.3` 决定 `dubbing`/`transliter` 命名并单批改三端映射。
8. `W1.4` 40 条 extra route 分类表。
9. `W2.4` AI SSE 接 `AiStreamScreen`（独立、可快速出证据）。
10. `W2.1 + W2.5` realtime 生产接线与请求头（两项都触及启动/会话，建议同批）。

---

## 8. 风险与防返工

| 风险 | 后果 | 对策 |
|---|---|---|
| 三份任务清单并行 | 重复实现、冲突提交 | W0.1 先固定主账本；旧账本只读 |
| `parity_task_audit` 假绿 | 未完成就归档 | W0.4 修审计并接 CI |
| iOS/OHOS 静态页批量改 | 130 个页面手工漂移 | 先 W3.1 ADR + W3.2 seam，按族推进 |
| realtime/SSE 无生产构造点 | 核心代码“看起来完成”但不可用 | W2 以“接线点 + 证据”为完成判据 |
| 外部 SDK 无凭据 | 伪 ready / 假 Success | registry 保持 missing；sandbox 仅 debug 明示 |
| `verify-parity.sh` 默认含 5 目标编译 | 单次验证成本高 | 日常用 `--static-only`；合并前跑全量 |
| 路由改名只改一侧 | 原生页 404/落回占位 | W1.3 单批改 Flutter 映射 + Kotlin + Swift/ArkTS + 脚本 |
| 智能体违反模块边界 | layer gate 红 | 每项任务卡写明 R1–R8；提交前跑 `check-layer-deps.sh` |

---

## 附录 A：任务清单到工作流的映射

| 来源 | 条目 | 去向 |
|---|---|---|
| v2 | M0.* | 作废（W0 记录） |
| v2 | P1–P5 | 已完成（W0 记录） |
| v2 | M1.0–M1.1 | W1 |
| v2 | M1.2–M1.7 | W1 复核；缺项进 W3 |
| v2 | M1.6.7 `ChatPort` | W2/W4.1 |
| v2 | M2.* | W2.1–W2.3 + W4 |
| v2 | M3.* | W2.4 |
| v2 | M4.* | W4.12 |
| v2 | M5.* | W4.2–W4.4 |
| v2 | M6.* | W4.10 |
| v2 | M7.* | W4.11 |
| v2 | M8.* | W2.2 / W2.6（M8.5 已完成） |
| v2 | M9.* | W2.7 |
| v2 | M10.A.* | 已完成（分发脚本 65/65） |
| v2 | M10.B.* | W3 |
| v2 | M11.* | W4.1 |
| v2 | M12.* | W3.3（新车跟进行为/数据） |
| v2 | M13.1/13.3/13.4/13.5/13.6/13.7/13.9/13.10 | W0 / W5 |
| v2 | M13.2/13.8/13.11 | 已完成或 W0 记录 |
| v2 | M14.* | W0.5 / W6 |
| v1 | A–L 未勾项 | 先查 v2 是否覆盖；唯一项（K.6/K.7/K.9/L.5）进 W4/W5/W6 |
| parity | `tasks-fine.md` 0–10 | 历史；行为/UX 细项经 W3/W5 复核，不单独排期 |
| parity | `tasks-execution.md` | 已完成历史账本，只读 |

## 附录 B：需要回写状态的历史条目

完成 W0 时，至少修正以下行的状态/备注，避免下一轮又从头审计：

- `platform-gap-registry.md`：OHOS 持久化、token refresh、SSE、realtime、in-scope RoutePath、重复 push 行。
- `route-inventory.md`：AI/Chat/Community/Friend/Live/Video/Classroom/Music 的 `partial/stub` 与 W3/W4 进度同步。
- `tasks-ultra-fine-v2.md`：M0/P/M1/M2/M3/M8/M10.A 的状态按代码回写；保留真正未完成项。
- `tasks-ultra-fine.md`：W5.i/W6.h 从“已闭环”改为“页面闭环、数据待 W3”。
- `tasks-fine.md`：标注“历史细项，已被 v2 吸收”。

## 附录 C：不建议在本轮做的事

- 不要重新拆分/回退模块（M0 已过时）。
- 不要在 W3 ADR 之前批量改 65 × 2 个原生页。
- 不要把 sandbox `Success` 当作真实支付完成。
- 不要在缺 SDK/凭据时把 gap 标成 `ready`。
- 不要为了“跑通”而让 feature 互相 import 或让 core 反向依赖 feature。
