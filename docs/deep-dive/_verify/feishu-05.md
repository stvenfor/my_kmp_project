# 05 · 门禁与工程流程

> `scripts/` 就是这个团队的「可执行宪法」，`openspec/` 是「决策病历」。读懂这两处，比多读 100 个 `.kt` 更有价值。

## 第一部分：脚本清单（每个都有存在理由）

| 脚本 | 守什么 | 为什么存在（真实事故） |
|-|-|-|
| `scripts/verify-parity.sh` | 静态门禁 + 五目标编译 + 全模块单测 + JUnit 汇总 | 任务名**不带冒号**＝在每个有该任务的子工程执行。写死 `:composeApp:` 会让新模块静默漏检，这就是历史上「绿灯但其实没跑」的根因 |
| 同文件内嵌的 Python lexer | Kotlin 注释 / 括号闭合 | KDoc 里写 `core/*` 这类内容会开启**永不闭合**的嵌套块注释，整个文件解析失败，级联出成百上千 `Unresolved reference`；已有两个 agent 因此浪费构建周期 |
| `scripts/check-layer-deps.sh` | 分层方向（feature↛feature、不向上、component 里无 `projects.feature.*`） | 曾实测 24 条 feature→feature 违规，且 CI 从未调用该脚本，违规静默累积 |
| `scripts/check-harmony-pane-dispatch.sh` | ArkTS 侧 65 个 `Native…Pane`：route → id → 渲染分支 → import 全链闭环 | 曾有 3 个 Pane 定义了但从未分发（页面永远进不去） |
| `scripts/check-ios-page-dispatch.sh` | Swift 侧 65 个 `Native…Page`：route → `resolve()` → guard → struct | 曾有专属页被 `genericList` 兜底吞掉；也防「具体前缀被父前缀抢走」 |
| `scripts/audit-flutter-drift.sh` | 上游 HEAD 前进时 diff 路由表与 `/api/v1` 端点集合 | 只读；`--record` 才更新 `notes/flutter-baseline.md` 里的基线 |
| `scripts/parity-evidence.sh` | 生成测试证据文档（660 tests / 68 类 / 0 失败，逐类列行） | agent 自述两头都不可信，只有跑绿的读数算交付 |
| `scripts/parity_task_audit.py` | 清单完成度审计 | 当前仍硬编码 v1 路径 `parity-flutter-to-kmp/tasks-execution.md`，支持 v2 是 M13.6 |
| `scripts/screenshot_diff_gate.py`（+ `test_screenshot_diff_gate.py`） | Android 像素级对齐，mse ≤ 2.0 | M13.10 待恢复为硬门禁 |
| `scripts/with-gradle-lock.sh` | 并发构建串行化（与 verify-parity 共用同一把 advisory lock） | 见过 666s 锁等待后失败，并被误怪「别人的文件坏了」 |
| `scripts/check-layer-deps.sh` 之外，`run.sh` / `run-android.sh` / `run-ios.sh` / `run-harmony.sh` | 三端一键运行 | `run-harmony.sh` 内部包装 `runscript/runOhosApp-Mac.sh` |

跑法：

```bash
bash scripts/verify-parity.sh --static-only     # 秒级，先看静态
bash scripts/check-layer-deps.sh
bash scripts/check-ios-page-dispatch.sh
bash scripts/check-harmony-pane-dispatch.sh
bash scripts/audit-flutter-drift.sh             # 慢（要扫整个 Dart 仓），>2 分钟属正常
```

## 第二部分：CI 现状

`.github/workflows/ci.yml` 一条流水线，只跑 Android：

- `bash scripts/verify-parity.sh --static-only`
- `./gradlew testDebugUnitTest :composeApp:assembleDebug -PandroidOnly=true --no-daemon --stacktrace`
- paths 触发包含 `component/**`、`feature/**`（新模块不可能绕开）
- 产物收集用通配 `**/build/reports/tests/`、`**/build/test-results/`
- `main` push 上传 debug APK，`v*` tag 出 GitHub Release
- iOS / OHOS 不在托管 runner 上，靠本地脚本

## 第三部分：openspec 流程

```
openspec/
  config.yaml
  specs/          ← 12 个能力域的稳定契约（app-shell、core-uuid、feature-chat、feature-commerce、
                   feature-community、feature-home、feature-media、feature-settings、modularity、
                   parity、platform-bridges、user-auth）
  changes/        ← 10 个有意为之的改造记录
  changes/archive/
```

推荐的 agent 循环（`AGENTS.md` 写死）：`/opsx-propose` → `/opsx-apply` → 专家 persona → 必要时 ruflo 多智能体 → `/opsx-archive`。

十个 change 目录值得逐个先读 `proposal.md` 再看实现，因为每个都记录「当时为什么不能那样做」：

| change | 主题 |
|-|-|
| `adopt-cpf-ktor-ohos` | OHOS 用 CPF Ktor（arm64 走 CIO，x64 回落 cinterop） |
| `adopt-cpf-coil-image` | OHOS 图像走 CPF Coil，x64 占位 |
| `adopt-cpf-uuid` | UUID 走 CPF，x64 stub |
| `unify-core-kv-storage` | KV 收进 `:core:storage` 单一契约，删 `/tmp` 回落 |
| `sync-flutter-full-parity` | 本轮主战场：M0–M14 + P1–P5 超细粒度清单 |
| `sync-flutter-0929-im-auth-voice` | IM / 鉴权 / 语音增量 |
| `sync-flutter-ui-visual`、`pixel-match-mine-ui` | 像素对齐 |
| `architect-production-foundation` | 分层与 ADR 起点 |
| `parity-flutter-to-kmp` | v1 清单 + `notes/platform-gap-registry.md` |

## 第四部分：parity 工作法（本项目的主营方法论）

1. **判错方之前先引 Dart 原文。** 任何 parity 失败（测试 vs 生产二选一）都必须先打开 `my_ai_project` 对应 Dart 文件读到具体行，再决定改哪一边，结论里带 `file:line`。
2. **禁止为了让测试变绿而删 / 弱化断言。** 可以改期望，但必须保留被覆盖的行为断言。
3. 给协作 agent 的「疑似根因」只能标注为假设，不能写成指令。历史上有一条假设被 agent 用 Dart 证据成功否证。
4. 契约表与漂移：`notes/api-contract-2026-10-16.md`（114 行端点契约）、`notes/flutter-baseline.md`（SoT HEAD `d7876a1d105e6bcff4b4a6d5367519e0bbcc8ecb`）。
5. 当前漂移读数（2026-10-17 实测）：endpoints missing=1 / extra=3，routes missing=0 / extra=33。extra 多数是 KMP 侧合法别名（`/mine/business`、`/mine/store_qr`、`/mine/sms_template`），不是缺陷。

## 第五部分：R5 诚实性规则（最重要）

环境缺真 SDK 时（融云 AppKey、微信 appId、JPush key、Go BFF `:8080` HTTP / `:9090` gRPC、Xcode / DevEco 整包构建），一律：

```kotlin
// GAP(R5): 需要融云 AppKey，环境不可用
```

并在 `openspec/changes/parity-flutter-to-kmp/notes/platform-gap-registry.md` 登记一行：能力 + `android/ios/ohos` 三列状态（`ready` / `partial` / `missing` / `stub`）+ blocker + follow_up。

**绝不标 ready。** 需要补登的 6 行是 M13.4：`analytics grpc`、`realtime ws`、`sse`、`apple/huawei iap`、`voice record`、`screen guard`、`relational storage`。

## 第六部分：当前真实进度

| 段 | 状态 |
|-|-|
| P1–P5 架构前置（core 下沉、KV 统一、17 feature 拆模块、composeApp 收窄） | 全部 ✅（2026-10-17，21 模块 660 tests / 68 类 / 0 失败） |
| M0 修红恢复编译 | 已完成（清单未逐条勾，属 M13.8 待办） |
| M1 API 契约层（50 条端点） | 大部分落盘并有测试，收尾核对中 |
| M2 realtime WS、M3 SSE | 逻辑与测试已落，剩接线与 OHOS 实测 |
| M4–M9、M10.B、M11–M14 | 162 条未收口，其中依赖外部凭据的部分按 R5 走 gap |

一句话：**看不见的执行路径读不出理解，没跑绿的交付不算交付。**
