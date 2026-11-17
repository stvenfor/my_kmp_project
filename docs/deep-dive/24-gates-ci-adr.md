# 24 · 工程门禁、CI、决策记录与 parity 工作法

> 范围：`scripts/` 全部 15 个文件（逐个整篇读完）、`.github/workflows/ci.yml`、`docs/adr/0001–0005` 全部 5 篇、`openspec/` 目录与工作法、`runscript/` 本地运行链，以及有证据的本机约束。
> 证据口径：每条事实附仓库相对路径与行号；标「实测」的条目给出命令与当日输出。
> 撰写过程中**未运行 Gradle、未 build、未 commit**。所有实跑均为纯静态 shell/python（`bash -n`、`scripts/check-*.sh`、`scripts/verify-parity.sh --static-only`、`scripts/audit-flutter-drift.sh`、`scripts/parity-evidence.sh`、`python3 scripts/parity_task_audit.py`、`python3 scripts/test_screenshot_diff_gate.py`）。

---

## 0. 为什么「架构真相」有一半在这些脚本里

本仓库三条产品纪律都不是靠口头约定维持，各自有一个可执行判定器：

| 纪律 | 判定器 | 判定方式 | 证据 |
|------|--------|----------|------|
| feature ↛ feature；低层不得向上 import；模块级不许依赖兄弟 feature | `scripts/check-layer-deps.sh` | find + grep 纯文本扫描，违规计数后 `exit 1` | `scripts/check-layer-deps.sh:1-11`、`:61-64` |
| iOS 每个 `Native…Page` 必须被 `resolve()` 的某个 guard 命中；Harmony 每个 `Native…Pane` 必须有 id → 渲染分支 → import | `scripts/check-ios-page-dispatch.sh`、`scripts/check-harmony-pane-dispatch.sh` | awk/grep 解析 Swift / ArkTS 派发表**本身**，不写死任何页面名 | `scripts/check-ios-page-dispatch.sh:1-21`、`scripts/check-harmony-pane-dispatch.sh:1-14` |
| 与 Flutter `my_ai_project` 的 HEAD / 端点 / 路由是否漂移 | `scripts/audit-flutter-drift.sh` | 三轴集合 diff，任一轴击穿即非零 | `scripts/audit-flutter-drift.sh:4-10`、`:184-193` |
| parity 清单是否真的做完 | `scripts/parity_task_audit.py`（Gate Z.1.1） | 勾选项统计，`[ ]` / `[~]` 非零即 FAIL | `scripts/parity_task_audit.py:2-5`、`:48-56` |
| 像素还原门槛（Android 硬门禁） | `scripts/screenshot_diff_gate.py` | ROI 裁剪 + 双线性归一 + mse/mad 比对 bar | `scripts/screenshot_diff_gate.py:2-14`、`:99`、`:135` |

而托管 CI **只把其中一条链的前两环放进去**：`bash scripts/verify-parity.sh --static-only`（`.github/workflows/ci.yml:72-75`），即 Kotlin 词法门 + 分层依赖门。其余静态门（iOS 派发、Harmony 派发、drift 巡检、任务审计）目前全部靠人/agent 手工执行——这是本分册最重要的现状结论，详见 §5.5。

---

## 1. 门禁总表

| # | 脚本 | 拦什么 | 输入 | 失败口径（exit code / 报什么） | CI 跑否 | 关键行号 |
|---|------|--------|------|--------------------------------|---------|----------|
| 1 | `scripts/verify-parity.sh` | 串行验证总入口：静态门 + 多 target 编译 + JUnit 汇总 | `--offline`、`--android-only`、`--static-only` | 词法门命中 → `exit 1` 并列前 40 条；未知参数 → `exit 2`；等锁超时 → `exit 1`；Gradle 非零 → `exit $build_status`；日志含 `^e: `/`FAILURE:`/`Execution failed` → `exit 1`；`tests=0` 或有 failure/error → `exit 1` | **是**（仅 `--static-only`） | `:11-21`、`:23-51`、`:53-166`、`:170-173`、`:177-201`、`:203`、`:206-232` |
| 2 | `scripts/check-layer-deps.sh` | 三条依赖轴（详见 §3） | 无参（自带 `find` 范围） | 每违规打印 `VIOLATION: …`，末尾 `Found N layer violation(s)…` + `exit 1`；全清打印 `OK: layer gate clean …` | **是**（由 #1 的 `:168` 调用） | `:1-11`、`:16`、`:19-28`、`:33-38`、`:41-59`、`:61-65` |
| 3 | `scripts/parity-evidence.sh` | 「单测没跑也绿灯」——把 JUnit XML 逐类摊开成中文表格 | 可选 `output.md` 落盘路径 | 无 test-results → `exit 1` 并提示先跑 #1；有 failure/error → `exit 1` | 否 | `:1-6`、`:16-25`、`:31-42`、`:44-66` |
| 4 | `scripts/with-gradle-lock.sh` | 窄构建与 #1 抢同一个 Gradle daemon 锁 | 任意命令 + 参数（如 `./gradlew :feature:ai:testDebugUnitTest`） | 无参 → `exit 2`；等锁超时 → `exit 1`；否则**原样透传被包命令的退出码** | 否 | `:1-6`、`:11`、`:13-29`、`:31-37` |
| 5 | `scripts/parity_task_audit.py` | `tasks-execution.md` 残留未完项（Gate Z.1.1） | 无参（路径写死 `:14`） | 文件缺失 → `exit 2`；有 `[ ]`/`[~]` → 打印前 40 条 + `GATE Z.1.1 FAIL — continue implementation; do not archive.` + `exit 1`；全清 → `GATE Z.1.1 PASS` + `exit 0` | 否 | `:2-6`、`:14`、`:18-20`、`:27-44`、`:46-57` |
| 6 | `scripts/check-ios-page-dispatch.sh` | iOS 原生页派发链断裂 / 已知路由落到 `genericList` | 无参，无 Gradle、无 Xcode | 每页打印 `FAIL …` + 末尾 `OFFENDERS:` + `check-ios-page-dispatch: FAILED` + `exit 1` | 否 | `:1-21`、`:33-62`、`:64-142`、`:145-165`、`:167-202`、`:218-266`、`:269-275` |
| 7 | `scripts/check-harmony-pane-dispatch.sh` | ArkTS pane 派发链断裂（双向） | 无参，无 Gradle、无 DevEco | `FAIL …` + `OFFENDERS:` + `…: FAILED` + `exit 1` | 否 | `:1-14`、`:24-59`、`:68-96`、`:99-105` |
| 8 | `scripts/audit-flutter-drift.sh` | 上游 Flutter SoT 漂移（HEAD / 端点 / 路由三轴） | `--record`、`--flutter <abs>`；env `FLUTTER_REPO` | 前置条件不满足 → `exit 2`；任一轴击穿 → `drift axes flagged: N` + `exit 1`；全对齐 → `no drift …` + `exit 0` | 否 | `:1-21`、`:38-41`、`:49-92`、`:110-135`、`:139-157`、`:161-180`、`:184-193` |
| 9 | `scripts/screenshot_diff_gate.py` | Android 像素还原度超门槛 | `--flutter --kmp --out-dir [--route --bar --top --bot --width --height]` | 图缺失 → `exit 2`；`mse_pct > bar` → 打印 `FAIL route=…` + `exit 1`；否则 `PASS` + `exit 0`。**两种结果都写证据四件套** | 否 | `:2-14`、`:28-43`、`:46-70`、`:74-84`、`:86-88`、`:99`、`:102-128`、`:130-135` |
| 10 | `scripts/test_screenshot_diff_gate.py` | #9 这个「缝」本身没坏（同色必过、异色必挂） | 无参 | `assert` 抛出即非零；通过打印 `test_screenshot_diff_gate: OK` | 否 | `:2`、`:18-19`、`:28-51`、`:53-75` |
| 11 | `scripts/sync-flutter-assets.sh` | 三端资源脱钩（Flutter 资源没进 `:core:design`，或没扇出到 iOS/OHOS 原生壳） | env `FLUTTER_ROOT`、`DRY_RUN=1` | 源目录不存在 → `raise SystemExit(...)` 非零；Phase 2 若 `SRC` 缺失 → `源目录不存在：…` 退出 | 否 | `:1-8`、`:15-19`、`:23-29`、`:34-51`、`:53-77`、`:110-122`、`:129-238` |
| 12 | `scripts/run.sh` | 平台别名派发入口 | `android\\|and\\|a`、`ios\\|i`、`harmony\\|ohos\\|h` + 透传参数 | 未知平台 → 提示 + 用法（`usage` 里 `exit 0`） | 否 | `:1-13`、`:17-24`、`:29-38` |
| 13 | `scripts/run-android.sh` | Android 构建/装机/启动 + 模拟器自启动 | `-s/--serial`、`--avd`、`--assemble-only`、`--no-boot` | SDK 找不到 → `exit 1`；无 adb 设备且 emulator 缺失 / 无 AVD / 起机超时 / `boot_completed` 超时 → `exit 1` | 否 | `:1-13`、`:18-24`、`:49-79`、`:87-128`、`:130-136`、`:138-141` |
| 14 | `scripts/run-ios.sh` | 模拟器选择 + `xcodebuild` + install/launch | `--device`、`--udid`、`--build-only` | 非 macOS → `exit 1`；工程缺失 / 指定模拟器不存在 / 无可用 iPhone / 找不到 `.app` → `exit 1` | 否 | `:1-13`、`:48-55`、`:57-68`、`:72-121`、`:128-147`、`:149-159`、`:161-165` |
| 15 | `scripts/run-harmony.sh` | 一键跑鸿蒙（转发到 `runscript/`） | 全部转发给 `runscript/runOhosApp-Mac.sh` | 非 Darwin 且无 `.bat` → `Error: unsupported host OS` + `exit 1` | 否 | `:1-13`、`:19-29` |

**只有 #1、#2 在 CI 里被执行**（`bash -n` 实测 12 个 `.sh` 全部语法通过；命令：`for f in scripts/*.sh runscript/runOhosApp-Mac.sh; do bash -n "$f"; done`）。

---

## 2. 逐个脚本详讲

### 2.1 `scripts/verify-parity.sh` —— 串行验证总入口

三段式（`## 1/3 static gates` `:53` → `## 2/3 gradle` `:187` → `## 3/3 result` `:196`）。

**(a) Kotlin 词法门**（`:58-166`，内嵌 `python3 - <<'PY'`，扫描根由 `:58` 的 argv 给出：`"composeApp/src" "core"`）。

它存在的理由写在 `:54-57`：Kotlin 块注释**可嵌套**，所以 KDoc 里出现 `core/*` 这类路径会开一个永不闭合的注释，该文件解析失败后它声明的**每一个符号**在全树级联成 `Unresolved reference`；注释原话是「two agents already lost build cycles to this」。判定器是一个手写词法器 `findings()`：

| 构造 | 行号 | 判定 |
|------|------|------|
| 行注释 `//` | `:71-74` | 跳到行首，不计入嵌套 |
| 块注释 `/* */` | `:75-90` | `depth` 计数，遇 `/*` +1、`*/` -1；到 EOF 仍 `depth>0` → `"<start>: unterminated block comment"` |
| 三引号串 | `:92-110` | 关键细节：连续引号串里**按 Kotlin 语义用最后三个引号闭合**（`:99-103`），所以 `""""…""""` 这类「原始串含引号」写法不会误判 |
| 单行串 | `:111-122` | 允许 `\\` 转义；遇裸换行或 EOF 未闭合 → `unterminated string` |
| 字符字面量 | `:123-133` | 同上 → `unterminated char literal` |
| 括号栈 | `:134-142`、`:145-146` | `}`/`]`/`)` 与栈顶不配对 → `"'c' closes nothing"`；EOF 残留栈**只报前 3 个**开括号位置（防刷屏） |

驱动段 `:149-165`：对每个扫描根 `rglob("*.kt")`，跳过路径含 `/build/` 的（`:152`）；命中则打印 `Kotlin lexer gate — unclosed constructs:` 到 stderr、最多 40 条 + `... N more`，并附「usual causes: a nested /* opener (e.g. `feature/**`) inside a KDoc, or a source file truncated mid-write.」再 `sys.exit(1)`（`:156-164`）。全清打印 `kotlin comment/bracket scan: OK`（`:165`）。

> **覆盖缺口（实测口径）**：扫描根只有 `composeApp/src` 与 `core`，**不含 `feature/` 与 `component/`**（`:58`）。`feature/` 有 17 个模块、`core/` 有 10 个（实测 `ls feature | wc -l` = 17、`ls core | wc -l` = 10），所以 `feature/*` 里一个未闭合注释只能由 Gradle 编译阶段抓出来。

**(b) 分层门**：`bash scripts/check-layer-deps.sh || exit 1`（`:168`），细节见 §3。

**(c) `--static-only` 出口**：`:170-173` 打印 `verify-parity: static gates only, gradle skipped` 后 `exit 0`——这正是 CI 唯一执行的路径（`.github/workflows/ci.yml:75`）。

**(d) 任务集**（`:177-185`）：

```
tasks=("testDebugUnitTest" ":composeApp:compileDebugKotlinAndroid")
# 非 --android-only 时追加：
compileKotlinIosX64 / compileKotlinIosArm64 / compileKotlinIosSimulatorArm64
compileKotlinOhosArm64 / compileKotlinOhosX64
```

注释在 `:175-176` 给了设计意图：**任务名不带冒号 = 在每个有该任务的子工程里执行**；写死 `:composeApp:...` 会让新增模块的单测/native 编译永远进不了门禁。所以 `testDebugUnitTest` 与 5 个 `compileKotlin*` 都是裸名，只有 `:composeApp:compileDebugKotlinAndroid` 是限定名（装配层必须被钉住）。同一条纪律在 `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/MODULES.md:67-68` 也写着，并把历史病根称为「绿灯但其实没跑」。

**(e) 跑 Gradle**（`:188-194`）：先 `acquire_lock`（`:27-46`，见 §4.3），再 `./gradlew "${tasks[@]}" $offline --stacktrace 2>&1 | tee verify-parity.log`，用 `PIPESTATUS[0]` 取 Gradle 自己的退出码（`:192`），随后显式 `rmdir_lock` + `trap - EXIT`（`:193-194`）。

**(f) 结果判定**：`:197-201` 非零则 `BUILD FAILED (exit N) — last 60 lines:` 把 `verify-parity.log` 尾巴打到 stderr；`:203` 用 `grep -E "^e: |FAILURE:|Execution failed"` 兜住「退出码 0 但日志里有编译错误」这种形态，命中即 `exit 1`；`:206-232` 用 python 解析全仓 JUnit XML（`:209` 的 glob 是 `**/build/test-results/**/TEST-*.xml`，跳过 `node_modules` 与 `/harmonyApp/`，`:211`），逐模块打印 `classes/tests/failures/errors`（`:221-224`），并在 `:225-227` 把 **`total==0` 判为失败**（注释 `:204-205`：sed/awk 老写法会静默报 `tests=0`，等于「单测没跑也绿灯」），`:228-230` 有 failure/error 也失败。全清 → `verify-parity: OK`（`:233`）。

### 2.2 `scripts/check-layer-deps.sh` —— 三条依赖轴

见 §3 全文展开（含实测违规数）。

### 2.3 `scripts/parity-evidence.sh` —— 单测证据摊平

头部注释即口径（`:1-6`）：证据只反映**最近一次未过滤的** `verify-parity.sh` 运行，因为带 `--tests` 的过滤跑会清空该目录，所以「跑过窄测试之后再采证」= 假证据。

- 目录发现不再写死两个路径：`:16-22` 用 `rglob("TEST-*.xml")` 收集所有含 `/build/test-results/` 的父目录，排除 `harmonyApp`、`node_modules`。注释 `:15` 说明原因：拆出 `:core:*` / `:feature:*` 后写死目录会**静默漏掉新模块**。
- 解析失败的 XML 不静默跳过，而是记为 `(p.stem, -1, -1, -1, "UNPARSEABLE")`（`:34-36`），从而在表格里以负数暴露。
- 输出为中文 Markdown 表（`:48-58`），标题「# 单测证据（per-class）」，采集时间取 `datetime.date.today()`，有 failure/error 的行加 ` ⚠`（`:57`）。
- 可选第一参数写盘（`:60-63`，自动 `mkdir(parents=True)`），无论是否写盘都会打印全文（`:65`）。
- 退出码 `0 if (f==0 and e==0) else 1`（`:66`）。

**实测**：`bash scripts/parity-evidence.sh` → `合计：**664 tests, 0 failures, 0 errors, 0 skipped**，70 个测试类。`（数据源为仓内 70 个 XML；`find . -path '*/build/test-results/*' -name 'TEST-*.xml' | wc -l` = 70。注意这些 XML 的时间戳来自 `verify-parity.log` 最后一次全量跑，即 2026-10-17 01:21。）

### 2.4 `scripts/with-gradle-lock.sh` —— 与 #1 共用同一把锁

头部注释（`:1-6`）直接点名要复现的故障：「observed failure: 666s lock wait, then a spurious "other files are broken" attribution」——并发抢 Gradle daemon 时，等待方拿到的是守护进程锁超时，而报错文本会把人误导成「源码坏了」。

实现与 `verify-parity.sh:27-46` 是同一算法：锁目录 `${TMPDIR:-/tmp}/qoder-gradle-verify.lock`（`:13`）、pid 文件 `<lock>/pid`（`:14`）、`mkdir` 原子性当 test-and-set（`:18`）、`kill -0` 探活回收死锁（`:20-22`）、10 秒轮询（`:28`）、`${GRADLE_LOCK_WAIT:-3600}` 秒超时（`:15`、`:23-26`）。差别在收尾：`with-gradle-lock` 用 `trap cleanup EXIT`（`:32-33`）并在末尾**原样透传被包命令的退出码**（`:35-37`），因此可以安全地套在任何窄构建外面。

**用法**（`:6`）：`scripts/with-gradle-lock.sh ./gradlew :feature:ai:testDebugUnitTest`。仓内被引用记录只有一处运行笔记（`.workbuddy/memory/2026-10-20.md:10`：`./gradlew help --console=plain` 55s、29 模块配置通过）。

### 2.5 `scripts/parity_task_audit.py` —— Gate Z.1.1

- 目标文件写死为 `openspec/changes/parity-flutter-to-kmp/tasks-execution.md`（`:13-14`），缺失 → `MISSING <path>` + `exit 2`（`:18-20`）。
- 只统计以 `- [` 开头的行（`:27`）：`- [n/a]` 计入 `na` 并跳过（`:29-31`）、`- [x]` 计入 `done`（`:32-34`）、`- [ ]` 与 `- [~]` 分别进 `open_items` / `partial`（`:36-44`）。
- 一个刻意的设计：`:38-40` 里「行内同时含 `missing` 和 (`registry` 或 `gap`)」**并不豁免**——注释写 `still counts as open until explicitly [n/a] or done with registry row`。也就是说「已在缺口登记表里挂了号」不能代替勾选状态本身，必须显式改成 `[n/a]` 才算结。
- 失败输出只列前 40 条（`:49-53`），结尾硬话：`GATE Z.1.1 FAIL — continue implementation; do not archive.`（`:54`）——把「不许 archive」写进脚本，对应 OpenSpec 工作流（§7.2）。

**实测**：`python3 scripts/parity_task_audit.py` → `[x]=585  [n/a]=25  [ ]=0  [~]=0` + `GATE Z.1.1 PASS — no open/partial items.`，退出码 0。

### 2.6 `scripts/check-ios-page-dispatch.sh` —— iOS 派发链回归守卫

守卫目标写在 `:1-21`：M10.A.2.i 的回归，链是

```
route/label → NativeRouteResolver.resolve() → `path == "…"` guard → Native…Page(
```

**它不写死任何页面**，路由↔结构体映射和 resolver 语义全部从源码本身解析（`:8-19` 列出 5 个信息源）：

| 信息源 | 解析方式 | 行号 |
|--------|----------|------|
| 结构体定义 | `grep -E '^struct Native[A-Za-z0-9]+Page: View'` → `iosApp/iosApp/NativeSecondaryPages.swift` | `:33-36` |
| 渲染对（guard → 页面） | `awk '/^struct NativeFeatureHost/,/^}/'` 切出宿主 body，再扫 `path == "…"` / `pathOrLabel == "L"`，**下一行**的 `Native…Page(` 与之配对；同一行多个 `path ==` 全部收下 | `:38-59` |
| resolver | 内嵌 `resolve.awk`（`:64-142`）重放 `NativeRouteResolver`：先 harvest `labelToPath` 字典（`:68-80`）与 `normalizePath`（if 链 `:92-118`，`case …: return "…"` 交换表 `:82-91`），再按源码顺序重放 | `:64-142` |
| 已交付路由 | `core/router/**` 的 `AppRoutePath.kt` + `FeatureRoutes.kt` 里所有 `"/…"` 字面量；页面内 `onOpen?("…")` 字面量；`ContentView` 的 `toastOnlyKeys` | `:145-153` |
| 壳自有 | `ContentView.swift` 里 `case` 分支路径 + toast 集合 + `openOwnedRoute…openMineIslandWithSoftAuth` 区段内的 `route/key/path.hasPrefix("…")` | `:159-165` |

`resolve.awk` 的 `npath()`（`:121-131`）**先走 if 链、再走 switch 表、最后 identity**，与 Swift 侧真实顺序一致；identity 结果用哨兵 `"\001identity"`（`:120`）区分「规则命中但返回原值」与「没命中」。重放段（`:133-141`）先剥 scheme（`://` 之后）、query（`?`）、fragment（`#`），以 `/` 开头才当路径解析，否则查 `labelToPath`。

两条判定（`:218-239`、`:243-266`）：

1. **每个定义的结构体必须既被实例化、又至少有一条可达 guard**。`path == "/x"` 型 guard 必须是 `resolve()` 的**输出**（`producible.txt`），`pathOrLabel == "L"` 型则是拿原始 key 比较，所以必须是**已交付 key 本身**（`:215-217` 注释解释这个不对称）。两种失败各有文案：`never instantiated in the NativeFeatureHost chain` / `guards [%s] never produced by NativeRouteResolver.resolve()`（`:231`、`:234`）。
2. **已知路由不许在有专用结构体的情况下落 `genericList` 占位**。若 resolved 与原始路径都不在 guards、不在 shell-owned、不在 exempt 白名单，则再用「某条可达 guard 是本路由的**严格路径前缀**」（`:253-258`）判定是否「结构体写好了但映射漏了这一条」，命中报 `genericList although %s is dedicated to it (guard %s)`，否则报 `genericList and not declared out-of-scope`（`:260`、`:263`）。

**白名单（out-of-scope registry）**：`:171-202` 内嵌 heredoc 列出 30 条路由，注释 `:167-170` 说明理由——这些路由落 `genericList` 是因为**根本没有对应的 `Native…Page` 结构体**，凭空发明 SwiftUI 属范围外；逐条归属证据在 `openspec/changes/sync-flutter-full-parity/notes/ios-generic-list-2026-10-17.md`。清单里可看到三类：`/bfui/*` 模板 demo（17 条，正是 `openspec/changes/parity-flutter-to-kmp/proposal.md:16` Non-goals 里明排除的 `module_bfui`）、鉴权页（`/login`、`/login/otp`、`/login/password`、`/register` —— ADR 0002 第 3 条规定 Auth UI 归原生壳）、调试页（`/settings/im_debug`、`/settings/realtime_debug`、`/settings/linking_debug`、`/settings/dialog_demo`、`/settings/bluetooth_demo`）与 `/pay`、`/media/entry`、`/community/topic_select`、`/community/video_play`。

**实测**（`bash scripts/check-ios-page-dispatch.sh`，退出码 0）：

```
defined structs 65 · rendered structs 65 · dispatch route ids 76
known Kotlin routes 122 · producible paths 136 · shell-owned paths 49
→ check-ios-page-dispatch: OK — all 65 Native pages wired route → resolve() → guard → struct; 30 route(s) declared out-of-scope
```

**违规长什么样**（真实代码形态举例）：在 `iosApp/iosApp/NativeFeatureCatalog.swift:479-485` 的派发链里，一个合规条目形如

```swift
} else if path == "/home/learning_report" || pathOrLabel == "学习报告" {
```

若新加 `struct NativeFooPage: View` 到 `NativeSecondaryPages.swift` 而忘了在 `NativeFeatureHost` 里加 `path == "/x"` 分支 → 报 `FAIL NativeFooPage never instantiated in the NativeFeatureHost chain`；若加了分支但 `/x` 不在 resolver 的任何输出里（`normalizePath` 没有归一规则、`labelToPath` 也没有）→ 报 `guards [/x] never produced by NativeRouteResolver.resolve()`。历史上真实踩过的正是后者：`NativeFeatureCatalog.swift:559` 的注释写着「AppRoutePath.classroomVideoDetail — previously fell through to genericList」（`:644` 是 `genericList` 兜底分支，`:648` 是它的实现）。

### 2.7 `scripts/check-harmony-pane-dispatch.sh` —— ArkTS 派发链守卫

链形（`:4-6`）：`route → nativePaneIdFor() pane id → Index.ets render branch → struct (+ Index.ets import)`。四个解析源同样不写死页面名：

| 集合 | 来源与正则 | 行号 |
|------|-----------|------|
| 定义 | `grep -E '^export struct Native[A-Za-z0-9]+Pane'` 取第 3 字段，来自 `harmonyApp/entry/src/main/ets/pages/NativeSecondaryPages.ets` | `:24-26` |
| 映射 id | `awk '/^export function nativePaneIdFor/,/^}/'` 内所有 `return '<id>'`，**剔除空串与通用兜底 `'feature'`** | `:28-32` |
| 渲染对 | `Index.ets` 里 `this.nativePane === '<id>'` 之后第一条 `Native…Pane(`；关键是 `:44` 先跳过 `this.Native…Pane(` ——那是 `@Builder` 方法调用（如 `NativeFeaturePane`），不是结构体构造 | `:34-52` |
| import | `Index.ets` 中 `import { … } from './NativeSecondaryPages'` 块里的 `Native…Pane` 名（跨行缓冲拼接） | `:54-59` |

正向判定（`:68-86`）：结构体必须同时具备「有渲染分支」「渲染 id 被 `nativePaneIdFor` 真的返回过」「在 `Index.ets` 里被 import」，三种失败文案分别是 `no Index.ets render branch`、`render id '<ids>' is never returned by nativePaneIdFor`、`not imported in Index.ets`（`:73-79`）。

**反向对称判定**（`:88-96`）：`nativePaneIdFor` 返回了某个 id 但 `Index.ets` 没有对应渲染分支 → `FAIL mapped id <id> has no Index.ets render branch`。注释 `:88-89` 点明这是**同一类 bug 的另一个方向**——后果是打开一个空白覆盖层而不是它的 pane。

**实测**（`bash scripts/check-harmony-pane-dispatch.sh`，退出码 0）：`defined structs : 65`、`mapped pane ids : 65`、`render pairs : 65 struct(s) rendered`，尾行 `OK — all 65 Native panes wired route → id → render branch → import`。渲染分支在 `harmonyApp/entry/src/main/ets/pages/Index.ets:1989-2021` 一带（`if (this.nativePane === 'search') { … }` 形态）。

### 2.8 `scripts/audit-flutter-drift.sh` —— 双仓 drift 巡检（M14.3）

三轴定义在 `:4-10`；约束与安全边界在 `:19-21`：只跑 `git rev-parse / git log / find / grep / python3`，**绝不写 Flutter 仓**；只有显式 `--record` 才改本仓的一个文件（baseline note 的 `baseline_head` 行）。SoT 默认路径 `${FLUTTER_REPO:-/Users/qpy/Desktop/github/my_ai_project}`（`:25`）。前置检查四条，任一不满足 `exit 2`（`:38-41`）。

**核心是一个内嵌 resolver**（`:49-92`）：它把带 `$CONST` 组合的字符串字面量**解成最终路径再比**，否则 `/api/v1/mall` + `"$BASE/cart"` 会被比成两堆碎片。做法是两遍（`:56`）：先用无插值的常量填表（`:65-67`），再迭代 4 轮解析嵌套常量（`:68-74`，且要求解出来的值不含 `$`、`{`、`:` 才收录）；剩下解析不了的占位统一成 `:v`（`:59-61`），末尾去掉右斜杠（`:62`）。模式分岔在 `:80-85`：

- `ep` 模式只收 `/api/v1…`、`/im`、`/im/…`；
- `route` 模式**反向排除** `/api*` 与 `/im*`，只留长度 >1 的，即路由表。

Kotlin 侧生产代码取法在 `:98-103`：`find core feature component composeApp/src -name '*.kt'` 后用一条 grep 白名单剔掉测试面（`commonTest|androidUnitTest|jvmTest|nativeTest|iosTest|ohosTest|UnitTest|Test\.kt|/test/`）。

**轴 (a)**（`:110-135`）：`git -C $FLUTTER_ROOT rev-parse HEAD` 与 baseline note 里 `^baseline_head:` 行比对（解析式 `:114`，note 文件 `:26` 指向 `openspec/changes/sync-flutter-full-parity/notes/flutter-baseline.md`）；不一致则打印「Flutter is N commit(s) ahead」（`rev-list --count`，`:124`）并 `drift_total++`；`--record` 时用 `sed -i.bak` 就地改写那一行（`:128-129`），否则提示「run with --record to freeze this HEAD」。baseline 为空也算 drift（`:116-118`）。

**轴 (b)/(c)**（`:139-180`）：两侧集合都用 `comm -23`（Flutter 有、KMP 无 = missing）与 `comm -13`（KMP 有、Flutter 无 = extra）；路由侧权威表是 Flutter 的 `commons/wys_router/lib/src/route/route_path.dart`（`:163-165`），Kotlin 侧只扫 `core/router/src/commonMain`（`:166-167`）。**注意 missing 与 extra 都算 drift**（`:155`、`:157`、`:178`、`:180`）——KMP 自己多出来的端点/路由同样会被拦。

**实测**（`bash scripts/audit-flutter-drift.sh`，退出码 1）：

| 轴 | 结果 |
|----|------|
| (a) HEAD | `current d7876a1 … → aligned`（与 `openspec/changes/sync-flutter-full-parity/notes/flutter-baseline.md:15` 记录一致，不计数） |
| (b) 端点 | Dart 91 / Kotlin 93；missing **1** = `/api/v1/payments/prepay`；extra **3** = `/api/v1/im/friends/requests/:v/respond`、`/api/v1/mall`、`/api/v1/user/huawei/login` |
| (c) 路由 | Dart 表 102 / Kotlin `core/router` 135；missing **6**（全部是旧词那一族：`/classroom/homework/dubbing`、`/home/dubbing_feed`、`/video/dubbing/{videos,videos/detail,works,works/detail}`）；extra **39** |
| summary | `drift axes flagged: 4` + `headlines: endpoints missing=1 extra=3 · routes missing=6 extra=39` |

关于那 6 条 missing：它们**不是漏迁**，而是本仓把旧英文词统一改名成 `transliter`（`/video/transliter/videos` 等正出现在 extra 列表里）。改名的纪律写在 `scripts/sync-flutter-assets.sh:34-38`（并解释旧词按片段拼装的原因）；drift 脚本没有对这条映射做豁免，所以改名之后轴 (c) 会**持续报这 6 条 missing**，直到 baseline note 或脚本引入对应词表。这是一条「门禁口径与改名口径未同步」的真实缝隙。extra 里的 `/mine/fan_group` 也值得注意——`openspec/config.yaml:29` 明写「Do not reintroduce FanGroup … branding」。

### 2.9 `scripts/screenshot_diff_gate.py` —— Android 像素硬门禁

口径（`:2-6`）：Flutter Android SoT vs KMP Android 截图，报 `mse_pct` / `mad_pct`，门槛默认 `mse_pct <= 2.0`。退出码在 docstring `:14` 写死：`0 pass / 1 fail / 2 usage-IO`。

流水线：

1. **ROI 裁剪** `_crop_roi`（`:28-34`）按高度比例切，默认 `top=0.04, bot=0.90`（`:81-82`）——目的写在 `openspec/changes/parity-flutter-to-kmp/notes/pixel-acceptance-sop.md:25`：把状态栏/导航栏 chrome 裁掉，否则沉浸式 insets 的差异会淹掉内容差异。`y1<=y0` 抛 `ValueError("invalid ROI top=… bot=…")`。
2. **归一化** `_load_rgb`（`:37-43`）：`convert("RGB")` → float32 → 裁 ROI → 双线性 resize 到 `540×960`（`:83-84`）。两张图先裁后缩，尺寸不一致也不会炸。
3. **度量**（`:57-62`）：`mse = mean(diff²)`、`mad = mean(|diff|)`，再各自按 255 归一为百分比（`:60` 注释说明这是为对齐 Phase-1 报告的 0-100 口径）。
4. **热力图**（`:63-64`）：`mean|diff| ×4` 放大后 clip 到 0-255，写成 R+B 洋红通道（洋红 = 差异区，一眼看穿）。
5. **判据**（`:99`）：`passed = mse_pct <= bar`。
6. **证据四件套**（`:102-107`、`:127-128`）：`{route}.flutter.png`、`{route}.kmp.png`、`{route}.heat.png`、`{route}.diff.json`——**PASS 和 FAIL 都写**，因为验收要的是可复核证据而不是只有绿灯。
7. `meta` 块（`:119-125`）把门槛、ROI、测量时刻，以及一句纪律写进 JSON：`"ios_ohos": "open-path + sampling only (not hard gate per ADR 0003)"`。

`{route}.diff.json` 里的通过字段名为 **`pass_`**（尾下划线，避开 `pass` 关键字）。

**实测**：仓内 `find openspec/changes -name '*.diff.json' | wc -l` = **75**，其中 `"pass_": true` **69** 个、`"pass_": false` **6** 个；失败的 6 个是 `commerce/Android/pay_membership`、`video/Android/video_short`、`video/Android/video_short_publish`、`video/Android/video_transliter_videos_detail`、`video/Android/video_transliter_works`、`video/Android/video_transliter_works_detail`。样例通过项：`openspec/changes/parity-flutter-to-kmp/notes/evidence/home/Android/home_ledger.diff.json` 为 `mse_pct 1.3707 / mad_pct 3.5249 / bar 2.0`，`measured_at 2026-09-27T10:43Z`。

### 2.10 `scripts/test_screenshot_diff_gate.py` —— #9 的缝测试

不是单测框架，而是一个 subprocess 冒烟：造两张纯色 PNG（`_solid`，`:18-19`），同色必须 `returncode==0` 且 `mse_pct==0.0` 且 `pass_ is True`（`:28-51`）；把第二张换成 `(200,0,0)` 并把 bar 压到 `0.01`，必须 `returncode==1` 且 `pass_ is False` 且 `mse_pct > 0.01`（`:53-75`）。跑在 `tempfile.TemporaryDirectory()` 里，不污染证据目录。

**实测**：`python3 scripts/test_screenshot_diff_gate.py` → `test_screenshot_diff_gate: OK`，退出码 0。附带一条环境警告：Pillow 11.3 报 `DeprecationWarning: 'mode' parameter is deprecated and will be removed in Pillow 13 (2026-10-15)`（来自 `:19` 的 `Image.fromarray(..., "RGB")`）——升级 Pillow 到 13 之前需要去掉这处 `mode` 位置参数。

### 2.11 `scripts/sync-flutter-assets.sh` —— 资源同步 + 三端扇出

两个 Phase，各一段内嵌 python。

**Phase 1（`:9-127`）Flutter → `:core:design` drawable**

- 目标目录（`:16`）是 `core/design/src/commonMain/composeResources/drawable`，注释 `:15` 明确「拆分后 `composeApp` 不再持有 composeResources」，与 `AGENTS.md`（Compose resources 条款）、`composeApp/build.gradle.kts:246-253` 的 `harmonyResourceRoots` 三处一致。
- 源只有 4 个包（`:23-28`）：`features/{home,pay,settings,music}/assets`，各带一句「喂给哪些屏幕」的说明；扩展名白名单 `{.png,.webp,.jpg,.jpeg}`（`:29`）。`.svg/.json` 不复制但**记进 gaps 清单**（`:61-63`），其它扩展名静默跳过。
- 改名函数 `sanitize()`（`:40-47`）：转小写 → 非 `[a-z0-9]` 全变 `_` → 合并连续 `_` 并 strip → **禁词→`transliter` 映射** → 首字符是数字则加 `n_` 前缀（R 资源名不能以数字开头）。禁词本身按片段拼装（`:38` `LEGACY_TOKEN = "dub" + "bing"`），`:34-37` 解释两条理由：不这么写重跑同步会把旧名再生成一份、与仓内现有资源脱钩；同时满足「仓库内不得出现该字符串」。`display()`（`:49-51`）在写 map 文档时做同样替换，否则 map 会把禁词带回仓库。
- 撞名处理（`:69-70`）：同 `name` 不同源文件时追加 `_<md5(源路径)[:6]>`，避免后写覆盖前写。
- 产物 map 落在 `openspec/changes/sync-flutter-ui-visual/notes/asset-sync-map.md`（`:17`），表头固定 5 列 `module | flutter_path | compose_id | screens | file`（`:90-93`），尾部四段人工说明：Tab chrome 用 CupertinoIcons 向量而非 PNG（`:96-98`）、Chat/Community 无本地图包（`:100-102`）、Gaps/skips（`:104-107`，截断到 100 条）、Fonts（`:108`）。
- `DRY_RUN=1`（`:19`、`:110-122`）：不落盘、不写 map，改为与现有目录做三向对比（同名 / 新增 / 目录里多出的非本脚本产物），用来核对改名风险。

**Phase 2（`:129-238`）`:core:design` → iOS imageset + OHOS media**

存在理由写在 `:129-131`：三端原生壳（ADR 0002）各自读本地图、**不从 composeResources 取图**，所以不扇出就会「二级页整页截图缺图」。

- `SKIP_IMAGESETS = {"AppIcon","AccentColor","LaunchImage"}`（`:143`）——非资源集不参与扇出，保护 `AGENTS.md`「Launch icon & splash assets」表里那套手工资产。
- **SHA1 幂等**（`:145-146`、`:198`、`:226`）：字节相同就什么都不做，只统计 created/updated。
- **扩展名保真**（`:157-160`）：源里的 `.jpg` 不能落成 `.png`；注释解释魔数还能解，但「工具链/审计会当成脏数据」。
- **历史脏数据自愈**：iOS 侧若目录里存在同名但扩展名不符的旧文件，改名修正并计入 `updated_ios`（`:166-180`）；OHOS 侧同样有 `legacy = OHOS / f"{stem}.png"` 的改名分支（`:214-221`）。
- `Contents.json` 的 `"filename"` 用一条正则改写（`:202-209`），注释 `:202`：磁盘名与 JSON 里的 filename 不一致时 Xcode 找不到图。
- iOS 生成 1x universal 的 imageset JSON（`:186-197`）；OHOS 落扁平 `<stem>.<ext>` 到 `harmonyApp/entry/src/main/resources/base/media`（`:140`、`:210-229`），目录不存在则跳过（`:211-212`）。

**实测口径**：Phase 1/2 本身会向仓库写文件，本分册禁写故未执行；但其产物在仓内可验证——`openspec/changes/sync-flutter-ui-visual/notes/asset-sync-map.md:7` 就是它生成的 `Regenerate:` 行，同文件 `:6` 记录的 `Copied files:` 是它上一轮的实际数量。两条已修 bug 见 `.workbuddy/memory/2026-10-22.md:16`（`FLUTTER_ROOT` 默认值从写死 `/Users/mac/…` 改为 `$HOME/…`，对应现 `:7`；`DEST` 从废弃的 `composeApp/…` 改到 `core/design/…`，对应现 `:16`）。

### 2.12 `scripts/run.sh` + 三端 run 脚本

`run.sh` 是别名派发器：`android|and|a` → `run-android.sh`，`ios|i` → `run-ios.sh`，`harmony|ohos|h` → `run-harmony.sh`（`:29-38`，全部 `exec`）。三个 run 脚本共用「注释即帮助」技巧：`usage()` 用 awk 打印自身头部注释块直到第一条非注释行为止（`run.sh:17-20`、`run-android.sh:26-29`、`run-ios.sh:26-29`）。未知平台打印错误后再 `usage`（`run.sh:34-37`，注意 `usage` 内部 `exit 0`，所以参数错误也是 0 退出）。

| 脚本 | 关键逻辑 | 行号 | 值得注意的语义 |
|------|----------|------|----------------|
| `run-android.sh` | SDK 解析顺序：`ANDROID_HOME` → `ANDROID_SDK_ROOT` → `local.properties` 的 `sdk.dir`（`sed` 取，带 `\r` 与转义冒号清洗）→ `$HOME/Library/Android/sdk` → `$HOME/Android/Sdk` | `:49-70` | 这是**唯一读 `local.properties` 的脚本类门禁**，见 §8 |
| | 起机：无 `adb device` 才启动 emulator，`-list-avds` 取第一个，日志 `/tmp/kmp-android-emulator.log`，两轮等待（设备出现 + `sys.boot_completed`），超时 `${ANDROID_BOOT_WAIT:-120}` | `:87-126` | `--no-boot` 直接跳过整段（`:88-90`） |
| | 构建 | `:130-131` | **恒为** `./gradlew :composeApp:installDebug -PandroidOnly=true` |
| | `--assemble-only` | `:133-136` | 名字有点误导：它**仍然执行 installDebug**，只是不 `am start`；打印 `OK: installed (assemble-only; not launching).` |
| | 启动 | `:138-141` | 先 `am force-stop` 再 `am start -n PKG/ACTIVITY`；`PKG`/`ACTIVITY` 可被 `ANDROID_APP_ID`/`ANDROID_ACTIVITY` 覆盖（`:18-19`） |
| `run-ios.sh` | 平台闸门：非 Darwin 直接失败 | `:48-51` | iOS 脚本不能在托管 Linux runner 上跑，这是 §5.4 的直接原因之一 |
| | bundle id：env `IOS_BUNDLE_ID` → 否则 `sed` 读 `iosApp/Configuration/Config.xcconfig` 的 `PRODUCT_BUNDLE_IDENTIFIER` 并剥掉 `$(TEAM_ID)`（默认空） | `:57-68` | 与 `deployment.gradle.kts:54` 的 `iosBundleId` 是同一事实的两处实现 |
| | 模拟器选择四级优先：`--udid` → `--device` 名（找不到就把可用 iPhone 打到 stderr 并 `exit 1`）→ 已 Booted 的 iPhone → 第一个可用 iPhone | `:72-115` | 三条 awk 都靠 `\(([A-F0-9-]{36})\)` 抓 UDID |
| | 构建/安装 | `:136-165` | `-derivedDataPath $ROOT/build/ios-sim`；`xcodebuild -quiet build` 后 `find … -name '*.app'`；`--build-only` 只建不装（`:156-159`）；正式跑先 `simctl uninstall` 再 install/launch（`:162-164`） |
| `run-harmony.sh` | 纯转发 | `:19-21` | Darwin 时 `exec "$ROOT/runscript/runOhosApp-Mac.sh" "$@"`；非 Darwin 且存在 `.bat` 时只打印「Windows 请用这个命令」并 `exit 1`（`:23-26`）；否则 `unsupported host OS` `exit 1`（`:28-29`） |

### 2.13 `runscript/` —— 本地运行链（OHOS 全链路）

`scripts/run-harmony.sh` 只是包装；真链路在 `runscript/runOhosApp-Mac.sh`（329 行）与 `runscript/runOhosApp-Win.bat`（Windows 等价物，参数集完全对齐：`-m/-b/-a/-d/-p/-h`，`runOhosApp-Win.bat:36-50`）。Mac 版带 Apache-2.0 头 + CPF 上游版权（`:1-17`），说明它与 `deployment.gradle.kts:4-5` 同源（`kmp-cmp-example` 血统）。

分节（脚本自带 `[N. …]` 标题）：

| 段 | 行号 | 做什么 | 失败口径 |
|----|------|--------|----------|
| 参数解析 | `:25-84` | 默认 `ohosArm64` / `127.0.0.1:5555` / bundle `com.example.harmonyapp` / ability `EntryAbility`；`POSITIONALS` 数组允许**选项与位置参数任意交错**（注释 `:62` 举例 `ohosArm64 62Q0226107024702 -m release`） | 未知选项 → 打印 usage（usage 内 `exit 0`，`:51`） |
| DevEco 版本门 | `:99-131` | `defaults read …/Info.plist CFBundleShortVersionString`，只比 major/minor，`MIN_VERSION="6.0.0"`（`:100`） | 目录缺失 `exit 1`；版本过低 **`exit 5`**（`:130`） |
| 环境注入 | `:135-141` | `DEVECO_HOME=$DEVECO_PATH/Contents`、`HDC_BIN=…/sdk/default/openharmony/toolchains/hdc`、把 `DEVECO_SDK_HOME`、jbr、node、ohpm、hvigor 塞进 `PATH`；先 `hdc shell aa force-stop`（`\|\| true`） | — |
| 任务名拼装 | `:143-160` | `HARMONY_APP_DIR` 取 `-p` 否则 `harmonyApp`；`BUILD_MODE=release` → `publishReleaseBinariesToHarmonyApp`，否则 `publishDebugBinariesToHarmonyApp`；`-p` 时给 Gradle 追加 `-PharmonyAppPath=` | 这正是 `AGENTS.md`「OHOS publish contract」里 `-PharmonyAppPath` 的入口 |
| Gradle 构建 | `:162-182` | `PLATFORM=ohosArm64` → 跑 publish 任务；`iosSimulatorArm64` → 跑 `linkDebugFrameworkIosSimulatorArm64`（复用同一脚本调 iOS framework） | 其它平台 → `Error: Unsupported platform` + **`exit 4`**（`:180-181`） |
| HAP 打包 | `:186-204` | `ohpm install --all` → `hvigorw.js --sync` → `assembleHap`（release 分支多带 `compileNative` 与 `requiredDeviceType=phone`，debug 分支用 `entry@default`），全程 `--parallel --incremental --daemon` | `harmonyApp` 目录不存在 → `exit 4`（`:190-193`） |
| 产物门 | `:207-231` | 期望 `entry/build/default/outputs/default/entry-default-signed.hap` | 不存在 → 打印期望路径 + 4 条可能原因 + 4 条建议动作 + **`exit 6`**（`:214-230`） |
| 设备门 | `:232-236` | `hdc list targets` 必须含 `TARGET_ID` | 否则 `Device … is offline!` + **`exit 5`** |
| 安装 | `:241-256` | `mkdir -p /data/local/tmp/debug_install` → `file send` → `bm install -p`；输出含 `failed` 则 `bm uninstall` + `sleep 5` 后重装 | 不阻断（重试一次后继续） |
| 启动 | `:273-301` | `DEBUG_MODE=debug` 时加 `-D`（attach 模式不加） | 输出含 `10106102` 或 `screen is locked` → 打印解锁三步 + **`exit 1`**（`:287-297`）；其它非零只警告不中断（`:299-301`） |
| 计时报告 | `:305-325` | 分三段报 SO 构建 / HAP 打包与推送 / 总耗时 | — |

与 OHOS publish contract 对得上的两处硬编码：`runOhosApp-Mac.sh:282-283`（`cpp/include/arm64-v8a/libkn_api.h` 与 `entry/libs/arm64-v8a/libkn.so`，注：该路径校验实际在 `deployment.gradle.kts:282-283`）在 Gradle 侧由 `deployment.gradle.kts:272-292` 的 `verifyHarmonyHapIntegration` 校验；产出方是 `composeApp/build.gradle.kts:261-277` 的 `publish{Debug,Release}BinariesToHarmonyApp`（`dependsOn("link{Debug,Release}SharedOhosArm64")`，并把 `harmonyResourceRoots` 拷到 `entry/src/main/resources/rawfile/composeResources/<pkg>/`）。ArkTS 侧的稳定符号契约在 `harmonyApp/entry/src/main/ets/pages/Index.ets:836-838`：`libentry.MainArkUIViewController` 取不到时直接 `this.errorMessage = 'MainArkUIViewController missing'`。

`ohosX64` 这条路是**明确关着的**：`composeApp/build.gradle.kts:279-284` 注册了 `publish{Debug,Release}BinariesToHarmonyAppX64`，但 description 自陈 `Currently blocked by toolchain: Konan_cxa_demangle on linkDebugSharedOhosX64.`；`deployment.gradle.kts:60-61`、`:295-299` 把 X64 校验任务放在 `-PharmonyX64=true` 开关后面（`deployment.gradle.kts:21-22` 注释同样记了这件事）。

---

## 3. `check-layer-deps.sh` 三条依赖轴逐条展开

头部注释 `:1-11` 就是设计说明书，末尾一句是关键前提：「拆分是渐进的，所以每条规则都同时覆盖『还在 `:composeApp` 里』和『已下沉成模块』两种形态」（`:10`）。计数器 `violations` 由 `report()` 统一 +1 并打印 `VIOLATION: …`（`:14-16`），最后 `>0` 即 `exit 1`（`:61-64`）。全清尾行是一句话总结：`OK: layer gate clean (no feature→feature imports, no upward imports, no feature→feature module deps).`（`:65`）。

### 轴 1：feature → feature（源码 import）

`:19-28`。遍历 `find "$ROOT/composeApp/src" "$ROOT/feature" -name '*.kt' -print0`（`:28`，NUL 分隔防空格路径）。每个文件先用 sed 从**路径**里抠出它属于哪个 feature：

```
feat="$(echo "$file" | sed -n 's|.*/feature/\([^/]*\)/.*|\1|p')"   # :20
[[ -z "$feat" ]] && continue                                       # :21
```

然后只喂 `^import com.example.my_kmp_project.feature\.` 的行（`:27`）给正则 `feature\.([a-zA-Z0-9_]+)`（`:23`），捕获到的 `other` 与 `feat` 不等就报 `"$file imports feature.$other"`（`:25`）。

三个要点：

1. **`app/` 层是合法豁免口**。`composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppCrossFeatureScreens.kt:8-23` 直接 import 了 home/mine/ai/classroom/commerce/friend/live/media 等多个 feature（实测 `grep -rh "^import …\.feature\." composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/ | sort -u | wc -l` = **27** 行唯一 import），门禁放行，因为它的路径不含 `/feature/<name>/`，`:21` 直接 `continue`。这正是注释 `:4-5` 讲的「跨模块 UI 走 app 装配 + CrossFeatureScreens 端口」，`composeApp/build.gradle.kts:131-132` 也把这条对应关系写在依赖列表旁边。
2. 匹配的是**包名段**而非目录段，所以 `feature.home.usedcar` 归给 `home`，不会误报成 `usedcar`。
3. **只扫 import 行**。同模块内不限定引用不触发轴 1；跨模块则一定触发编译或轴 3。

### 轴 2：低层不得向上 import

`:33-38`。遍历集合是 `composeApp/src`、`core`、`feature`、`component` 四棵树，但**只保留路径含 `/core/` 或 `/component/` 的 `.kt`**（`:37-38` 的 `\( -path '*/core/*' -o -path '*/component/*' \)`）。判定是一条 grep：`^import $PKG\.(feature|app)\.`（`:36`），命中即报 `"$file (lower layer) imports upward: $line"`。

注释 `:30-32` 说明为什么把 `feature/` 也放进遍历集合——为了兜住「某个已下沉模块内部又开出一个 `core/` 子包」这种形态。`:13` 的 `PKG=com.example.my_kmp_project` 与 `AGENTS.md`（Package root）一致。

### 轴 3（含 3b / 3c）：模块级依赖

光查 import 不够——注释 `:7-8` 讲得直白：feature 拆成 Gradle 模块后，`implementation(projects.feature.X)` **本身就是违规**，必须在 `build.gradle.kts` 层拦。于是三条同构循环，扫不同目录下的 `build.gradle.kts`，判定的都是 `projects.feature.`：

| 子轴 | 行号 | 扫描目录 | 违规文案 |
|------|------|----------|----------|
| 3 | `:41-45` | `find "$ROOT/feature" -name build.gradle.kts`（实测 17 个） | `declares a feature→feature module dependency:` |
| 3b | `:48-52` | `find "$ROOT/core" -name build.gradle.kts`（实测 10 个） | `(core layer) depends on a feature module:` |
| 3c | `:55-59` | `find "$ROOT/component" -name build.gradle.kts`（实测 1 个） | `(component layer) depends on a feature module:` |

`:43`、`:50`、`:57` 都做了 `${line//[$'\t']/ }` 制表符归一，保证输出在终端可读。

### 实测结果（2026-10-23）

| 命令 | 结果 |
|------|------|
| `bash scripts/check-layer-deps.sh` | `OK: layer gate clean …`，**exit 0，违规数 = 0** |
| `bash scripts/verify-parity.sh --static-only` | `kotlin comment/bracket scan: OK` → 同上 OK → `verify-parity: static gates only, gradle skipped`，exit 0 |

0 违规不是常态，仓内有两条硬历史快照：

- **6 条**：`openspec/changes/architect-production-foundation/notes/spike-i-exit-review.md:34`、`:62` 记 2026-09-04 时 6 条 feature→feature import（更早快照 8 条），并注明「allowed for Spike I exit honesty」——当时是**带着红灯出门**的。
- **24 条**：`openspec/changes/sync-flutter-full-parity/tasks-ultra-fine-v2.md:305` 记「实测 → **Found 24 feature→feature import(s)**」，并逐条点名 `ContentRouteHost` 引 ai/classroom/friend/live/media ×8、`MineRouteHost` 引 classroom/commerce/media、`HomeScreen` 引 content/mine ×2、`SoftAuthPresenter` 引 auth ×2、`FriendDirectory` 引 chat ×2；同一行给出根因判断：「脚本仅扫 `composeApp/src/commonMain/.../feature`，**CI 从未调用**，故违规静默累积」。这条 24 是在旧扫描范围下测的；今天脚本已扩到 `feature/` 与 `component/`（`:28`、`:37-38`、`:41-59`），所以 0 是「范围扩大 + 真把模块拆完」两件事叠加的结果。

**违规长什么样**（对照真实包名的三例）：

```kotlin
// 轴 1 —— :feature:friend 里引 :feature:chat
import com.example.my_kmp_project.feature.chat.<ChatScreen>
// 轴 2 —— core/* 里向上 import app/*
import com.example.my_kmp_project.app.AppContainer
// 轴 3 —— feature/home/build.gradle.kts 里依赖兄弟 feature
implementation(projects.feature.mine)
```

合规替代只有两种：跨功能跳转让 `:composeApp` 的 `app/AppCrossFeatureScreens.kt` 装配，或走 `:component` 的路由端口 `component/src/commonMain/kotlin/com.example.my_kmp_project/component/routing/CrossFeatureScreenPort.kt`（`AppCrossFeatureScreens.kt:5` 引的正是它）。同一条纪律的四处文字：`settings.gradle.kts:36-37`（「feature 模块之间禁止相互依赖（R7 + check-layer-deps.sh 第 1/3 条）」）、`component/build.gradle.kts:4`、`composeApp/src/commonMain/kotlin/com.example.my_kmp_project/MODULES.md:20-22`、`openspec/changes/sync-flutter-full-parity/proposal.md:25`（R7）。

---

## 4. `verify-parity.sh` 全流程

### 4.1 三阶段与早退出口

```
:53   ## 1/3 static gates   ← 内嵌词法器（:58-166）+ check-layer-deps（:168）
:170  --static-only → exit 0（跳过整个 Gradle 阶段）  ← CI 走这条
:187  ## 2/3 gradle         ← acquire_lock（:188）+ ./gradlew（:191）+ rmdir_lock（:193）
:196  ## 3/3 result         ← 退出码/tail 60/grep 日志（:197-203）+ JUnit 汇总（:206-232）
```

`cd "$root"`（`:8-9`）保证相对路径与 `**/build/test-results/**` 的 glob 起点一致。

### 4.2 模式矩阵

| 模式 | 解析处 | 行为差异 | 典型用途 |
|------|--------|----------|----------|
| 无参 | `:14-21` | 7 个任务：`testDebugUnitTest`、`:composeApp:compileDebugKotlinAndroid`、3×iOS `compileKotlin*`、2×OHOS `compileKotlin*`（`:177-184`） | 三端全量自查（`MODULES.md:64`） |
| `--android-only` | `:17`、`:179` | 只留 2 个 Android 任务，跳过 5 个 native 编译 | 只改共享 Kotlin 逻辑时（`MODULES.md:63`） |
| `--offline` | `:16`、`:191` 的 `$offline` | 给 Gradle 追加 `--offline`；阶段标题会打印 `offline=on/off`（`:187`） | 镜像仓库抖动/断网时复用本地缓存 |
| `--static-only` | `:18`、`:170-173` | 词法门 + 分层门后 `exit 0`，**完全不碰 Gradle** | CI（`ci.yml:75`）、零成本预检 |
| 未知参数 | `:19` | `unknown arg: …` 到 stderr + `exit 2` | — |

### 4.3 advisory lock（协作式串行锁）

参数（`:23-25`）：锁目录 `${TMPDIR:-/tmp}/qoder-gradle-verify.lock`，锁内容 `pid` 一个文件，超时 `lock_deadline = now + ${GRADLE_LOCK_WAIT:-3600}` 秒。

`acquire_lock()`（`:27-46`）的四个动作：

| 动作 | 行号 | 说明 |
|------|------|------|
| 抢锁 | `:29-33` | `mkdir "$lock_dir"` 的原子性当 test-and-set；成功即 `trap 'rmdir_lock' EXIT` 并写 `$$` 进 pid |
| 死锁回收 | `:34-38` | 读 holder pid，`kill -0` 探活；进程已死则 `rm -f pid && rmdir lock_dir && continue` —— 崩溃的调用者不会留下永久锁 |
| 超时 | `:39-42` | 到点打印 `timed out waiting for gradle lock (holder pid=…)` 并 `return 1`（调用点 `:188` 转 `exit 1`） |
| 轮询 | `:43-44` | 每 10 秒重试，并把 holder pid 打到 stdout（可见的等待，不是静默阻塞） |

释放有两条路：正常路径在 Gradle 结束后显式 `rmdir_lock` + `trap - EXIT`（`:193-194`，避免后续 JUnit 汇总阶段还占着锁）；异常路径靠 EXIT trap（`:30`）。`rmdir_lock()`（`:48-51`）先删 pid 文件再 `rmdir` 目录。

**这把锁为什么存在**：头部注释 `:2-3` 说得是运维事实——「guarded by an advisory lock so concurrent agent builds cannot exhaust the daemon lock timeout」。`scripts/with-gradle-lock.sh:2-4` 补上观测到的病例（666s 锁等待 + 误归因）。它是 **advisory（协作式）** 而不是强制的：绕过它直接敲 `./gradlew` 完全能跑，只是会参与制造那个 666s。

### 4.4 任务名「不带冒号」这条纪律

`:175-176` 的注释是本文件最重要的一句工程决策：

> 任务名不带冒号 = 在**每个**有该任务的子工程里执行。写死 `:composeApp:...` 会让新增模块的单测/native 编译永远不进门禁（拆模块之后这就是漏检的根因）。

配套事实：`build.gradle.kts:37-38` 的 `subprojects {}` 只命中 `:core:*` 与 `:feature:*`（`:35` 的过滤器），这 27 个库模块在 `skipNative` 为假时才拿到 5 个 native target（`build.gradle.kts:57`、`:94-102`）——所以裸名 `compileKotlinOhosArm64` 会同时在 `:composeApp` 与所有库模块上执行。同一条纪律在 `MODULES.md:67-68` 与 `openspec/changes/sync-flutter-full-parity/tasks-ultra-fine-v2.md:345` 各写了一遍。

### 4.5 产出证据

| 产物 | 路径 | 生成处 | 消费方 |
|------|------|--------|--------|
| Gradle 全量日志 | `verify-parity.log`（仓库根，`tee`） | `:191` | `:198-199`（失败 tail 60）、`:203`（错误行 grep）；仓内现存快照时间戳 2026-10-17 01:21、177380 字节 |
| JUnit XML | `**/build/test-results/**/TEST-*.xml` | Gradle | `:206-232` 汇总；`scripts/parity-evidence.sh:16-22` 摊平成中文表 |
| 单测证据 md | 由 `parity-evidence.sh [output.md]` 决定 | `parity-evidence.sh:60-63` | parity 清单/验收报告 |

**与 parity 清单的关系**：`verify-parity.sh` 是「实现是否还能编、测是否还在跑」的机器证据；清单本身是 `openspec/changes/parity-flutter-to-kmp/notes/parity-inventory.md`（状态口径 `:8`：`todo | wip | logic-pass | packet-pass | excluded | gap-only`）+ `notes/acceptance-matrix.md` + `notes/platform-gap-registry.md`。三者由 `pixel-acceptance-sop.md:43` 串成一条纪律：「更新 acceptance-matrix + parity-inventory；挂上证据目录；保持 platform-gap-registry 诚实（不许假 pixel ready）」。当前清单实测行数：`grep -oE "\| (todo|wip|logic-pass|packet-pass|excluded|gap-only) \|" …parity-inventory.md` → **19 个 `logic-pass` + 7 个 `packet-pass`**（文件共 149 行）。

---

## 5. CI 现状与差距

### 5.1 触发面

| 事件 | 条件 | 行号 |
|------|------|------|
| `push` | 仅 `main`，且必须命中 11 条 paths 白名单 | `:3-19` |
| `pull_request` | 仅 `main`，同一份 paths 白名单 | `:20-34` |
| `workflow_dispatch` | 无条件 | `:35` |

白名单覆盖 `composeApp/** core/** component/** feature/** ohosAggregate/** gradle/** scripts/** *.gradle.kts gradle.properties gradlew gradlew.bat .github/workflows/ci.yml`。**`docs/**`、`iosApp/**`、`harmonyApp/**`、`openspec/**` 不在白名单内**——也就是说改原生壳 Swift/ArkTS 代码不会触发 CI（而 `scripts/check-ios-page-dispatch.sh` 恰恰只管这部分，见 §5.5）。

并发与权限：`concurrency.group = ci-${workflow}-${pr number 或 ref}` + `cancel-in-progress: true`（`:37-39`，同一 PR 新 push 会掐掉旧跑）；顶层 `permissions: contents: read`（`:41-42`），只有 release job 单独升到 `contents: write`（`:122-123`）。

### 5.2 `android` job 逐步骤

单 job 矩阵：`runs-on: ubuntu-latest`、`timeout-minutes: 60`、名称 `Android build & test`（`:45-48`）。

| 步 | 行号 | 做什么 | 备注 |
|----|------|--------|------|
| Checkout | `:51-52` | `actions/checkout@v4` | — |
| Set up JDK 17 | `:54-58` | `actions/setup-java@v5`，temurin 17 | 与 `build.gradle.kts:78-80`、`:90` 的 `VERSION_17`/`JVM_17` 对齐 |
| Set up Android SDK | `:60-61` | `android-actions/setup-android@v3` | — |
| Install SDK packages | `:63-64` | `sdkmanager --install "platforms;android-36" "build-tools;35.0.0"` | `android-36` 对应 `libs.versions.toml` 的 compileSdk；`AGENTS.md` 的 `android.suppressUnsupportedCompileSdk=36`（`gradle.properties:14`）就是为它兜底 |
| Write local.properties | `:66-67` | `echo "sdk.dir=$ANDROID_HOME" > local.properties` | 仓内 `local.properties` 已 gitignore（`.gitignore:25-26`），所以 CI 必须自己造 |
| Set up Gradle | `:69-70` | `gradle/actions/setup-gradle@v4` | 提供缓存 + wrapper 校验 |
| **Static gates** | `:72-75` | `bash scripts/verify-parity.sh --static-only` | 中文注释 `:72-73` 明确这一步 = 「Kotlin 词法扫描 + 分层门禁（feature→feature、下层反向 import、component 里出现 `projects.feature.*`）」，并写「拆模块后漏一条就是整片红」 |
| Assemble debug & unit tests | `:77-81` | `set -o pipefail; ./gradlew testDebugUnitTest :composeApp:assembleDebug -PandroidOnly=true --no-daemon --stacktrace 2>&1 \\| tee gradle-ci.log` | `testDebugUnitTest` 用裸名（全模块），`:composeApp:assembleDebug` 用限定名；`-PandroidOnly=true` 让 `build.gradle.kts:57` 与 `deployment.gradle.kts:59` 一起关掉 iOS/OHOS target 与任务 |
| Summarize Gradle failure | `:83-94` | `if: failure()`；把 `gradle-ci.log` 尾 200 行塞进 `$GITHUB_STEP_SUMMARY` 的代码块，再用 8 个 pattern grep 出最多 40 行、逐行发 `::error::` | pattern：`FAILURE:` / `What went wrong` / `Execution failed` / `Could not ` / `BUILD FAILED` / `> Task :.* FAILED` / `^e: ` / `Caused by:`（`:92`） |
| Upload Gradle log on failure | `:96-106` | `if: failure()`；artifact `gradle-ci-log`，收 `gradle-ci.log` + `**/build/reports/tests/` + `**/build/test-results/`；`if-no-files-found: ignore`，保留 7 天 | 与 `verify-parity.sh:204-205` 的「JUnit XML 才是证据」呼应 |
| Upload debug APK | `:108-115` | `if: success() && (push \|\| workflow_dispatch)`；`composeApp/build/outputs/apk/debug/*.apk`，`if-no-files-found: error`，保留 14 天 | PR 不产 APK |

### 5.3 `release` job

`:117-137`：`needs: android`、`if: success() && startsWith(github.ref, 'refs/tags/v')`，下载 `composeApp-debug-apk` 后用 `softprops/action-gh-release@v2` 发 `apk/*.apk` 并 `generate_release_notes: true`。即「`v*` tag → GitHub Release」这条链只在 tag push 上触发，而 tag push 同样要过 `android` job（`on.push.tags` 在 `:6`，但 paths 过滤对 tag 事件不生效——见 §9 疑点 2）。

### 5.4 iOS / OHOS 为何不在托管 CI

三条互相独立的证据：

| 原因 | 证据 |
|------|------|
| runner 平台不对 | CI 唯一 job 是 `runs-on: ubuntu-latest`（`:47`）；`scripts/run-ios.sh:48-51` 在非 Darwin 直接 `exit 1`，`xcodebuild`/`simctl` 只在 macOS 存在 |
| OHOS 工具链是本机专有 | `runscript/runOhosApp-Mac.sh:99-108` 要求 `/Applications/DevEco-Studio.app`，`:137` 从 `DEVECO_HOME/sdk/.../hdc` 取 hdc，`:197-203` 要 `ohpm` + DevEco 自带 `hvigorw.js`；CI 里没有这些 |
| 仓库自己承认这是设计而非疏漏 | `AGENTS.md`（Build & run 表下）：「GitHub Actions … uploads debug APK on `main` push; GitHub Release on `v*` tags. **iOS/OHOS not on hosted runners.**」；`deployment.gradle.kts:18-19` 也写明 deploy/verify 任务「依赖真机/模拟器，**门禁从不跑它们**」 |

### 5.5 差距清单（哪些门禁没进 CI）

| 未进 CI 的门禁 | 它守的东西 | 不进 CI 的后果 | 证据 |
|----------------|-----------|----------------|------|
| `scripts/check-ios-page-dispatch.sh` | 65 个 iOS 原生页的派发完整性 + 30 条 out-of-scope 声明 | 只在 macOS 手工跑；`iosApp/**` 还不在 paths 白名单里，改了也不会触发任何 CI | 脚本 `:21`；`ci.yml:7-34` |
| `scripts/check-harmony-pane-dispatch.sh` | 65 个 ArkTS pane 的 id → 渲染分支 → import 三向一致 | 同上，且 `harmonyApp/**` 不在 paths 白名单 | 脚本 `:14`；`ci.yml:7-34` |
| `scripts/parity_task_audit.py` | Gate Z.1.1「清单没勾完不许 archive」 | 只能靠人/agent 自觉执行 | 脚本 `:5`；`.github/workflows/ci.yml` 全文无该调用 |
| `scripts/audit-flutter-drift.sh` | 上游 SoT 三轴漂移 | 漂移静默累积（当前实测已有 4 轴击穿，见 §2.8） | 脚本 `:10` |
| `scripts/screenshot_diff_gate.py` | Android 像素门槛 | 逻辑优先阶段本就降级为可选（ADR 0003 第 7 条），但恢复硬门禁的任务尚未排上：`tasks-ultra-fine-v2.md:317`（M13.10） | `CONTEXT.md:48` |
| `verify-parity.sh` 的 Gradle 阶段（含 5 个 native 编译） | 三端可编 | CI 只跑 `--static-only`，native 编译完全在本机验证 | `ci.yml:72-75`、`verify-parity.sh:170-173` |
| 词法门对 `feature/`、`component/` 的覆盖 | 未闭合注释/字面量 | 这两个目录树不在扫描根里，只能靠 Gradle 兜 | `verify-parity.sh:58` |

`openspec/changes/sync-flutter-full-parity/tasks-ultra-fine-v2.md:310` 已经把这条待办写成明文：「**M13.3** CI 增列 `bash scripts/check-layer-deps.sh` + `parity_task_audit.py` 两步」，状态 `[ ]`。注意该行的动机句「CI 从未调用 check-layer-deps」在今天**已不成立**——`ci.yml:75` 经 `verify-parity.sh:168` 确实调它；那条待办的真实价值已经变成「把 iOS/Harmony 派发门与任务审计也搬进 CI」。

## 6. 五个 ADR 逐篇

ADR 目录：`docs/adr/`，5 个文件共 140 行。编号是**决策顺序**，不是生效顺序——0002 §6 已被 0003 显式作废（`docs/adr/0003-full-parity-phase.md:6`），0005 与上游 Flutter 仓的 0005 编号巧合但内容同源（`docs/adr/0005-three-long-lived-channels-separate-transports.md:5`）。

### 6.0 一览

| ADR | 状态行 | 一句话决策 | 它被哪些脚本/文件强制执行 |
|-----|--------|------------|---------------------------|
| 0001 生产架构基座 | `:3` Accepted（grill 2026-09-04） | 拆成 `app`/`feature`/`component`/`core`/平台壳五层，OHOS 用胖聚合产物出一个 `libkn.so` | `scripts/check-layer-deps.sh` 全部三轴；`settings.gradle.kts:23-44`；`composeApp/build.gradle.kts:55-92` |
| 0002 原生壳 + Mine Compose 岛 | `:3` Accepted，§6 被 0003 作废 | UI 归各端原生，共享 CMP 只允许出现在 Mine **二级页及其子页** | `scripts/check-ios-page-dispatch.sh`、`check-harmony-pane-dispatch.sh`；`feature/shell/…/SecondaryRouteIsland.kt:72-74,87-90` |
| 0003 全量 parity 阶段 | `:3` Accepted（2026-09-27） | 取消「其余功能后置」，每条在范围内的路由都要进 Parity Inventory 并过验收包 | `scripts/screenshot_diff_gate.py`、`parity_task_audit.py`、`parity-evidence.sh`、`audit-flutter-drift.sh`；清单 `openspec/changes/parity-flutter-to-kmp/notes/parity-inventory.md` |
| 0004 Kotlin 独占 BFF JSON | `:3` Accepted（2026-09-29） | 所有信封/请求体/业务 `data` 的编解码只在共享 Kotlin 做，壳层不解析 | `ApiClient.kt:75-143`；`harmonyApp/entry/src/main/ets/auth/AuthApiClient.ets:4-7`；`scripts/audit-flutter-drift.sh` 轴 (b) |
| 0005 三条长连通道各归各的传输 | `:3` Accepted（2026-10-17） | AI 走 SSE、全局通知走 realtime WS、聊天走 IM 端口，失败语义不得互相翻译 | 声称有静态门禁但**当前无脚本实现**（见 §6.5 末）；实际约束靠 `feature/ai/build.gradle.kts:14-21` 的依赖面 |

### 6.1 ADR 0001 —— 生产架构基座

**决策一句话**：把这个已经长成四 Tab 产品面的单模块 demo 拆成五层多模块（Android/iOS 各自多模块、OHOS 出一个胖 `libkn.so`），用密封路由 + `AppContainer` + `AuthSessionState` Flow 单一会话真源打底，并允许后续迁到 Koin。

**为什么**（`docs/adr/0001-production-architecture.md:11`，我的转述）：demo 阶段留下了本地 `when` 导航、两套并行会话轨（`AccountFacade` 与 `AuthSessionState`）、没有垂直的 `component` 层；同时上面还有 Flutter 的 parity 压力。而 OHOS 的链接模型只肯给你一个 `.so`，所以「模块要能并行开发」和「OHOS 要单一产物」必须同时满足——这是它选「多模块 + 胖聚合」而不是「少模块」的原因。

**原文关键句定位**：层定义 `:16`；模块策略 `:17`；导航 `:18`；DI `:19`；会话 `:20`；首批组件 `:21`；Must 门禁 `:22`；Spike 执行 `:23`；token 单一源 `:24`。正负面后果 `:28-29`，待办 `:30`。

**这条决策在本仓落到了哪些行**：

| 约束 | 落点 | 说明 |
|------|------|------|
| 五层，`feature` ↛ `feature`，低层 ↛ 高层 | `scripts/check-layer-deps.sh:19-28`（轴 1）、`:33-38`（轴 2）、`:41-59`（轴 3） | ADR 只说「要有 dependency guards」（`:17`），脚本是它的唯一可执行化身 |
| 模块清单 | `settings.gradle.kts:23-44`（`composeApp` + 10 个 `core:*` + `component` + 17 个 `feature:*` + `ohosAggregate`） | `:36-37` 处注释把 R7 与门禁脚本名直接绑死 |
| OHOS 胖聚合出一个 `libkn.so` | `composeApp/build.gradle.kts:55-92`，其中 `:58-60` 对 RELEASE 显式 `optimized = false` | 后者是为绕开 `DevirtualizationAnalysis` OOM，`AGENTS.md:129` 重申「别动它」 |
| 密封 `AppRoute` + `Navigator` 先行 | `core/router/…/AppNavigator.kt:9-24`（`sealed interface AppRoute`，14 个 case）、`:29-37`（`AppNavigator`）、`:39`（`LocalAppNavigator`） | 「optional Navigation Compose later」至今未引入——仓内无 Navigation Compose 依赖 |
| `AppContainer` 是过桥，之后迁 Koin | `composeApp/src/commonMain/…/app/AppContainer.kt:14-28`，注释 `:16` 直接写「migrate to Koin modules after Spike I (ADR 0001 / Q15=D)」 | 实测：`gradle/libs.versions.toml` 与 `composeApp/build.gradle.kts` 里 **grep 不到 koin**，迁移没发生，fat container 仍是现役 |
| `AuthSessionState` 是会话 SoT，`AccountFacade` 降级为仓库 | `component/src/commonMain/…/component/auth/AuthSessionState.kt:12`（注释「Sole reactive session source of truth for UI soft-gates (ADR 0001)」）、`:24-25`（`StateFlow<Snapshot>`）、`:34-37`（`sync()` 从 `AccountFacade.current()` 拉） | 注意 SoT 住在 `:component`，仓库住在 `:core:account`——方向正好符合轴 2 |
| Must 门禁矩阵 | `openspec/changes/architect-production-foundation/notes/must-matrix.md:1-17` | 沙箱支付标 **Partial**、push 标 **Partial**（厂商 SDK 未接），即「Missing Must ⇒ release Fail」（`:17`）目前有两项没达标 |
| 跨功能屏装配归 app 层 | `composeApp/src/commonMain/…/app/AppCrossFeatureScreens.kt:21-23`（注释：app → feature 允许，feature → feature 被 R7 / `check-layer-deps.sh` 禁止） | 这是「用装配层替代 feature 互依赖」的具体形态 |

**待办状态（`:30`）**：Follow-ups 列了 Detekt / Gradle import guards、must-matrix.md、Koin modules、DesignSystem 迁移。实测：must-matrix.md 已存在；`build.gradle.kts`/`libs.versions.toml` 里 grep 不到 detekt；Koin 未引入；token 迁移见 `openspec/config.yaml:15`（`DemoColors → DesignSystem target`），即仍是 alias 状态。

### 6.2 ADR 0002 —— 原生壳 + Mine Compose 岛

**决策一句话**：UI 所有权默认归各端原生（iOS SwiftUI / Android Jetpack Compose / Harmony ArkTS），KMP 只共享业务逻辑与可观测的 UiState/ViewModel；唯一允许共享 CMP 像素的地方是 Mine 的二级页及其子页（Mine root 仍原生）。

**为什么**（`:9` 的转述）：从 Flutter 仓同步产品时必须在「一套 CMP 产品 UI」和「每端原生 UI + 只共享逻辑」之间二选一。长期形态是全原生重写，但 Mine 的二级流用一套共享 CMP 仍然划算——所以留下一个明确的、边界很小的岛，而不是含糊的「大部分共享」。

**原文定位**：默认归属 `:13`；岛范围 `:14`；Auth UI 在岛外 `:15`；托管方式（原生壳推入 Compose 容器，返回先弹岛内路由再关容器）`:16`；视觉 SoT 与 ≤2% 门槛 `:17`；Phase 1 范围 `:18`（**§6 已被 ADR 0003 作废**）；token 单源 `:19`；遗留 commonMain 产品 Compose 只归 Mine `:20`。后果 `:24-26`（Android 因此承载两套 Compose 栈）。

**落地位置**：

| 约束 | 落点 |
|------|------|
| 岛只属于 Mine，非 Mine 路由被拒 | `feature/shell/…/SecondaryRouteIsland.kt:26-32`（KDoc「Mine-only Compose secondary host (ADR 0002 / #23)」）、`:72-74`（`navigate()` 拒绝非 Mine push）、`:87-90` + `:96-105`（`LegacyNonMineBlocked`：显示「请从原生壳打开 / 非 Mine 路由已迁出 Legacy 岛 (#23)」） |
| iOS 端「原生壳推入岛」的三个导出 | `composeApp/src/iosMain/…/MainViewController.kt:16`（旧全 App Compose 入口被 `@Deprecated("Native SwiftUI shell owns the app; …")`）、`:28`（`MineIslandViewController`）、`:51-64`（`SecondaryRouteViewController`，`onRequestClose` 里 `dismissViewControllerAnimated`，正是 `:16` 的托管语义） |
| OHOS 端同理 | `composeApp/src/ohosMain/…/MainArkUIViewController.kt:123`、`:282-314`（ArkTS 通过 CName 导出取 Kotlin 会话/派发，壳保持薄） |
| 派发表必须完整（原生壳 owning 的代价） | `scripts/check-ios-page-dispatch.sh`（实测 65 页全链闭合 + 30 条 out-of-scope 声明）、`scripts/check-harmony-pane-dispatch.sh`（实测 65 pane 全链闭合） |
| 认证 UI 在岛外、走原生壳 | `iosApp/iosApp/ContentView.swift:118`、`:246`；`harmonyApp/entry/src/main/ets/pages/Index.ets:836-838` |
| 「Android 两套 Compose 栈」 | `composeApp/build.gradle.kts` 同时配 `androidTarget` 与 CMP 资源根，`:246-253` 把 `harmonyResourceRoots` 指到 `my_kmp_project.core.design.generated.resources` |

ADR 0002 `:17` 那句「Parity bar: ≤2% error vs SoT」是 `scripts/screenshot_diff_gate.py:74-84` 里 `--bar 2.0` 默认值的出处；`:20` 那句遗留 CMP 是「reference/debt, not the main path」正是上面 `@Deprecated` 的措辞来源。

### 6.3 ADR 0003 —— 全量 parity 阶段取代 Phase-1 后置

**决策一句话**：进入 Full Parity Phase——每条在范围内的 Flutter 产品路由都要登记进 Parity Inventory 并过 Route Acceptance Packet；同时用 Native-First Cutover 把遗留 `SecondaryRouteIsland` 烧掉；能力缺口要诚实登记，绝不允许假报 `ready`。

**为什么**（`:10` 的转述）：Phase-1 交了壳、Tab 根和岛基线，把别的功能挂起了；现在产品要求整页迁移并给出明确验收线。而 iOS/Harmony 上仍有一堆非 Mine 二级页挂在遗留岛上，这本身就违反 0002 的所有权划分——所以「后置」这个天花板必须撤掉。

**原文定位**：进清单 `:14`；保持 0002 归属 `:15`；Native-First Cutover 烧岛 `:16`；能力缺口可用 mock 但必须诚实登记 `:17`；Screenshot Diff Gate 只对 Android 是硬门（iOS/Harmony 只需打通主路径 + 抽样）`:18`；实施单元是 Parity Domain Ticket（小域，约 2–4 条路由）`:19`；**逻辑优先阶段**（`:20`，2026-09-29，change `logic-first-parity`）——硬验收换成 Logic Acceptance Packet，Screenshot Diff Gate 降为可选，且别再把 SoT 位图当主交付。后果 `:24-26`。

**落地位置**：

| 约束 | 落点 | 实测 |
|------|------|------|
| Parity Inventory | `openspec/changes/parity-flutter-to-kmp/notes/parity-inventory.md`（149 行） | 58 条路由行，状态列分布：`logic-pass` 19、`pass` 15、`done` 10、`packet-pass` 7、空 7（用 awk 按分隔符切第 5 列分组计数得到，命令含表格分隔符故不写入本页表格） |
| Gate Z.1.1（清单没跑完不许 archive） | `scripts/parity_task_audit.py:48-57` | `python3 scripts/parity_task_audit.py` → `[x]=585 [n/a]=25 [ ]=0 [~]=0`，`GATE Z.1.1 PASS`，退出码 0 |
| Screenshot Diff Gate 只 Android 硬 | `scripts/screenshot_diff_gate.py:124`（meta 里 `"ios_ohos": "open-path + sampling only (not hard gate per ADR 0003)"`）；`parity-inventory.md:12` 同义复述 | `notes/evidence/**` 下 75 个 `.diff.json`：69 个 `pass_=true`、6 个 `false`（`pay_membership` 3.92、`video_short` 5.46、`video_transliter_works_detail` 24.44、`video_transliter_videos_detail` 27.45、`video_transliter_works` 5.05、`video_short_publish` 2.23） |
| 缺口诚实登记，不许假 ready | `openspec/changes/parity-flutter-to-kmp/notes/platform-gap-registry.md:7`（明确说 harness 条目不代表任何 SDK 缺口 ready）、`:38-46`（realtime ohosX64、connectivity、融云、IAP、微信授权码、逐请求头，全部带 `// GAP(R5)` 落点） | 代码侧同样带标记，例如 `component/…/realtime/AppRealtimeClient.kt`、`feature/pay/…/PayApi.kt`（5 处 `GAP(R5)`）、`feature/auth/…/UserAuthHttpApi.kt` |
| 逻辑优先 ⇒ 位图非主交付 | `parity-inventory.md:5`；`CONTEXT.md`（Logic Acceptance Packet / logic-pass 词条） | 上面 6 个 `pass_=false` 全部集中在 pixel 阶段遗留，路由行已按 logic-pass 口径推进 |
| 烧岛（Native-First Cutover） | `SecondaryRouteIsland.kt:87-90` 的拒绝分支 + `MainViewController.kt:16` 的 `@Deprecated` | iOS/Harmony 侧 65 页/pane 已由原生派发闭合（两个门禁脚本实测退出码 0） |

### 6.4 ADR 0004 —— Kotlin 独占 BFF JSON

**决策一句话**：全部 BFF JSON（标准信封、请求体、业务 `data`）都在共享 Kotlin 里用 `@Serializable` + 共用 `NetworkJson` 处理，`ApiClient` 以 reified 编解码为主路径，调用方不碰 `JsonElement`；非标准信封只在 Kotlin 侧留 Envelope Fallback；Harmony 登录也必须进 Kotlin，壳里不得维护平行权威会话。

**为什么**（`:8` 的转述）：CPF 的 `kotlinx-serialization`（`1.9.1-0.3.0`）已经覆盖 OHOS，但仓里网络层与 Auth 还在手解 `JsonElement`，且 Harmony Auth UI 在 ArkTS 用 `JSON.parse` 平行打 BFF，跟 iOS 的「壳 UI → AuthBridge → Kotlin」不一致。看上去像「Harmony 用不了 serialization」，实际上是**把语言边界当成了业务分叉**——这是本篇 ADR 最核心的一句诊断（`:8` 末）。`:16` 补上唯一真实限制：纯 ArkTS/Swift 运行时无法加载 Kotlin 序列化库，解法是「调用进 Kotlin」，不是「再写一套 JSON」。

**原文定位**：`:12`（reified 主路径）、`:13`（Envelope Fallback 只在 Kotlin）、`:14`（ArkTS 可留 Account Kit 拿授权码，但 `huawei/login` 等 BFF 仍进 Kotlin）、`:15`（Account Session SoT 在 Kotlin，停掉壳内平行存储）、`:16`（唯一「实在用不了」）。后果 `:20-21`。

**落地位置**：

| 约束 | 落点 |
|------|------|
| reified 编解码为主路径 | `core/network/…/ApiClient.kt:75`（`getApi`）、`:84`（`getListPage`）、`:92`（`postApiDecoded`）、`:101-104`（`postApi`，`:104` 即 `NetworkJson.encodeToString(body)`）、`:107-110`、`:113-116`、`:119-122`、`:125-128`、`:131-134`、`:137`、`:143`。`:99` 的 KDoc 原话「callers never touch [JsonElement]」 |
| 单一 `Json` 实例 | `core/network/…/NetworkJson.kt:6-11`（`ignoreUnknownKeys/isLenient/encodeDefaults/explicitNulls=false`）；`:19-24` 是唯一例外 `NetworkJsonExplicitNulls`，`:13-18` 的注释逐条列出哪些 BFF 字段必须显式发 null（Flutter 侧对齐要求） |
| Envelope Fallback 只在 Kotlin | `core/network/…/ApiEnvelopeParser.kt:10`（「(e.g. OAuth HTTP 401) use [Envelope Fallback] only — not a second business parser」）、`:24`（gateway 401 无业务 code 的分支）、`:95`、`:101`（`decodeEnvelopeData`） |
| 内部 `JsonElement` 只留在基座签名 | `ApiClient.kt:32,37,42,47,52` 的 `parseData: (JsonElement?) -> T?`——那是 reified 层下面的实现细节，feature 层拿到的是 DTO |
| ArkTS 退役 BFF 解析 | `harmonyApp/entry/src/main/ets/auth/AuthApiClient.ets:4-7`（「Thin ArkTS façade over Kotlin [AuthBridge] / BFF JSON (kotlinx-serialization). Does not parse BFF responses — Kotlin owns all envelope encode/decode.」）；`:15-33` 全部从 `libentry.so` 的 `AuthToken/AuthDisplayName/AuthUserId/AuthPhone` 取值，`:39` 直接 `AuthLoginWithPassword` |
| iOS 同构（本来就是这样） | `composeApp/src/iosMain/…/MainViewController.kt:81-99`（`AuthBridge.isLoggedIn/displayName/logout/loginWithPassword` 导出给 Swift） |
| OHOS NAPI 侧同构 | `composeApp/src/ohosMain/…/MainArkUIViewController.kt:123`（`AuthBridge.applySession`）、`:282`（`KnAuthCopyToken`）、`:285-289`（`KnAuthCopyDisplayName`） |
| 端点/路由集合与上游对齐（防「壳里另起一套请求」） | `scripts/audit-flutter-drift.sh:139-157`（轴 b：`/api/v1` 端点集合 diff） |

ADR 0004 的后果 `:20` 说「Harmony `AuthApiClient.ets` 的 BFF 路径应退役」——实测该文件已缩到 67 行、零 `JSON.parse`，退役已完成；`:21`「信封特例集中在 `core:network`，业务 feature 只声明 DTO」也成立（特例只在 `ApiEnvelopeParser.kt`）。

### 6.5 ADR 0005 —— 三条长连通道各归各的传输

**决策一句话**：AI 流式只走 SSE、realtime WS 只承载全局通知与断线补投、聊天只走 IM 端口；三条通道的失败语义不许互相翻译；AI 的「停留会话」id 只允许活在 `feature/ai` 的页面状态里，不得持久化。

**为什么**（`:9-17` 的转述）：上游 Flutter 有三条「服务器持续往客户端推」的通道，形态完全不同（一次性 `POST` + `text/event-stream`；长连 WS + ticket 鉴权 + `seq` 单调水位 + `notifyId` 去重环；厂商 SDK 自管连接与存储）。KMP 拆模块后它们分别落在 `:core:network`、`core/sse`、`:component:realtime`、`:component:chat`，**都归在「流式」这个大类里，很容易被顺手合并成一条通用流**——尤其 AI 接 SSE 时，最省事的做法就是复用已经在跑的 realtime WS。这条 ADR 的存在就是为了挡住这个「省事」。

**原文定位**：`:21`（决策 1，SSE + camelCase 请求体 `conversationId`/`clientRequestId`，禁止 WS 承载 AI token 流）、`:22`（决策 2，WS 只有 `seq` + `notifyId` 两条业务语义，不对外提供请求-流式响应接口）、`:23`（决策 3，聊天唯一入口 `ChatPort`/`ImEngine`，真融云接入前由 gap registry 登记 R5，不得用 SSE 或自研 WS 伪造「聊天已接通」）、`:24`（决策 4，失败语义：SSE 前流抛 `SseException` 且 body 绝不喂 parser；WS 走重连状态机 + `RealtimeWsError`；IM 走 SDK 回调码）、`:25`（决策 5，Visit Conversation，离开即丢弃、不进 `:core:account`、不进 `KeyValueStore`）。后果 `:29-32`。

**落地位置**：

| 约束 | 落点 |
|------|------|
| AI 只用 `SseTransport` + `core.sse.SseParser` | `feature/ai/…/AiSseApi.kt:5`（import `core.network.SseTransport`）、`:7`（import `core.sse.SseParser`）、`:16`（契约表 #96 注释）、`:24`（camelCase 注释）、`:28`（`transport: SseTransport = NetworkFacade.api()`）、`:32`（`COMPLETIONS_PATH = "/api/v1/sse/completions"`）、`:45`（「这种 body 绝不喂 [SseParser]」）、`:56-57`、`:76-83`（body 构造） |
| AI 模块依赖面小到看不见 realtime | `feature/ai/build.gradle.kts:14-21`：只有 `projects.core.ui` / `projects.core.network` / `projects.core.router`，**没有 `projects.component` / `:component:realtime`** |
| SSE 前流失败语义 | `core/network/…/Sse.kt:54`（`interface SseTransport`）、`:73`（`open class SseException`）、`:82`+`:91-105`（`fromPreStream`，逐字段对等 Dart `SseException.fromPreStream`）、`:145`（未升级 / 非 200 时读净 body 后抛）；`:48` 与 `:70` 直接标注 Dart 源行号 |
| WS 失败语义自成一套 | `core/network/…/realtime/RealtimeWsError.kt:11`（`sealed class RealtimeWsError`）、`:20`（`Unsupported`）、`:30`（`BodyUnparsable`）、`:40`（`TicketInvalid`）；`:7-8` 的注释说明为什么不能蹭 `NetworkError.Transport` |
| WS 传输与 client 分离 | `core/network/…/realtime/RealtimeWsTransport.kt:27`（`interface RealtimeWsTransport`）；`component/…/realtime/AppRealtimeClient.kt`（重连状态机、入站路由） |
| `seq` 水位与去重环的键名即语义 | `component/…/realtime/RealtimeSeqStore.kt:15-16`（对齐 Dart `sp_keys.dart:4`/`:5`）、`:44`（`LAST_SEQ = "realtime_last_seq"`）、`:45`（`NOTIFY_DEDUP = "realtime_notify_dedup_json"`），并有 `RealtimeSeqStoreTest.kt:29-30` 把两个字符串钉成断言 |
| 聊天唯一入口 | `component/…/chat/ChatPort.kt`、`component/…/chat/ImEngine.kt`；BFF 侧只有 `/api/v1/im/**`：`feature/chat/…/ImSessionApi.kt:44`、`:124`、`feature/chat/…/ImMessageBackupApi.kt:138`、`feature/friend/…/ImFriendApi.kt:25,28,31,34` |
| 融云未接入 ⇒ 诚实登记 R5 | `platform-gap-registry.md:41`（RongCloud IM 三项全 `missing`，并列出 HTTP 侧已落的 5 个 API 文件，`// GAP(R5)`） |
| OHOS 两架构分叉封在传输层 | `platform-gap-registry.md:38`（ohosX64 无 CPF Ktor klib ⇒ 工厂抛类型化桩异常）；`composeApp/build.gradle.kts:66-71` 的 `-lnet_http` 只对 ohosX64 生效 |
| Visit Conversation 不持久化 | 实测 `grep -rn "KeyValueStore" feature/ai/src` **无命中**；`grep -rn "conversationId" feature/ai/src` 只出现在 `AiSseApi.kt`（`:53,56,76,82,83`）与 `AiSseApiTest.kt`（`:62,77,139,149,154,159`），即它确实只活在传输/页面参数层，没有任何落盘路径 |

**一个必须记录的门禁缝隙**：ADR 0005 `:32` 声称「门禁可静态检查：`feature/ai` 里出现 `AppRealtimeClient` / `RealtimeWsTransport` 引用即为违规；`feature/chat` 里出现 `SseTransport` 同理」。**这条检查目前没有任何脚本实现**——`scripts/` 下 15 个文件里没有一个 grep 过 `AppRealtimeClient`/`RealtimeWsTransport` 作为违规模式，`check-layer-deps.sh` 的三轴也不覆盖「同层内特定符号」这种粒度。今天这两条不变式成立，只能靠我手工验证：`feature/ai/build.gradle.kts:14-21` 的依赖面 + `feature/ai/src` 内无 realtime 引用。也就是说 `:32` 写的是「可静态检查」（能力主张），不是「已被检查」（现役状态）。

## 7. `openspec/` 工作法与现存 change 清单

### 7.1 目录契约

```
openspec/
├── config.yaml                  # schema + 注入给 AI 的 context + 分 artifact 的 rules
├── specs/                       # 「主 spec」= 已部署的真相，按 capability 分目录
│   └── <capability>/spec.md     # 12 个文件
└── changes/                     # 「change」= 一次未部署的意图
    ├── <change-name>/
    │   ├── .openspec.yaml       # 只有 schema + created 两个键
    │   ├── proposal.md          # what & why
    │   ├── design.md            # how
    │   ├── specs/<capability-path>/spec.md   # delta，不是主 spec
    │   ├── tasks.md             # 实施步骤（- [ ] / - [x]）
    │   └── notes/               # 本仓自定义：证据、清单、gap registry
    └── archive/<YYYY-MM-DD>-<name>/
```

`openspec/config.yaml:1` 声明 `schema: spec-driven`；`:4-24` 是每次生成 artifact 前注入的项目 context（五层、包根、模块布局、中间源集 `networkKtorMain`/`accountSettingsMain`、以及 `:21` 的「feature ↛ feature」、`:24` 的「不许重新引入 FanGroup/tfent/T-Family/paopao」）；`:26-33` 是硬规则：proposal 限 500 词、必含 Non-goals、必点名 Android/iOS/OHOS 影响，tasks 偏好小且可验证、优先 `./gradlew :composeApp:compileDebugKotlinAndroid`；`:35-42` 给 apply/archive 两个操作各一条 guidance（apply：commonMain 优先 + 改完跑聚焦编译测试；archive：说清「落进 specs 的」与「仍在范围外的」）。

### 7.2 六个动作各自落在哪

工作流定义在 `.cursor/commands/opsx-*.md`，技能镜像在 `.cursor/skills/openspec-*/`。

| 动作 | 命令文件 | 写哪里 | 关键纪律（原文行号） |
|------|----------|--------|---------------------|
| **propose** | `opsx-propose.md` | 新建 `changes/<name>/` 与 4 类 artifact | `:10` 明确「只授权 planning，**不许编辑项目代码**；artifact 完成后在同一轮回复里停止，即使初始请求要求实现」；`:12-16` 列出默认 spec-driven 的四件产物；`:18` 说明 `<capability-path>` 是相对 `specs/` 的目录，实现时必须「start the apply workflow explicitly」 |
| **apply** | `opsx-apply.md` | 改源码；在 tasks 文件里把 `- [ ]` → `- [x]` | `:92` 与 `:166`「完成一个任务立刻勾一个」；`:41` 读 `contextFiles`（artifact id → 具体路径，随 schema 变）；`:164` 若实施暴露问题则暂停并建议回改 artifact；`:179-180` 声明这不是阶段锁死的工作流，可随时穿插 |
| **update** | `opsx-update.md` | 只改 planning artifact | `:7` 「Revise … keep them coherent. **Never edit code.**」 |
| **sync** | `opsx-sync.md` | `openspec/specs/<capability>/spec.md` | `:8-10`：这是 **agent-driven** 的智能合并（不是机械覆盖，例如只加一个 scenario 就不必复制整条 requirement）；`:16` 无参时从上下文推断、只有一个 active change 时自动选、歧义则 `openspec list --json` 让用户选 |
| **archive** | `opsx-archive.md` | `mv` 整个 changeRoot 进 `changes/archive/` | `:12` 主 spec 路径解析；`:84-98` 先比对 delta 与主 spec 再问「Sync now (recommended) / Archive without syncing」；`:106-112` 写主 spec 前先取 specs-rule 快照；`:115` 必须**内联**跑 sync 而不是丢后台——`:115` 的理由很硬：后台 sync 还没读完，step 5 就把 `changeRoot` 搬走了，结果是「change 归档了、主 spec 没更新」；`:117-123` sync 后要对**每个**有 delta 的 capability 复比，任一不符就停手不归档；`:129` `mkdir -p archive`；`:132` 目标名 = 已有 `YYYY-MM-DD-` 前缀则原样、否则前置当天日期，**绝不叠第二个日期**；`:135-139` 目标已存在则报错，否则 `mv "<changeRoot>" "<changesDir>/archive/<target-name>"` |
| **explore** | `opsx-explore.md` | 只读 | 配合 `docs/agents/domain.md:5-11`：动代码前先读 `CONTEXT.md` 与 `docs/adr/` 里相关 ADR，缺了就静默继续、不许反过来建议创建 |

所有动作共享 `:20`（各文件里都有）那段 store 选择规则：一旦选了 `--store <id>` 就在整个工作流里保持 sticky。本仓未使用 store，命令直接作用于最近的本地 `openspec/` 根。

### 7.3 `openspec/specs/` 现状 = 已归档 delta 的并集

主 spec 目前 12 个文件：`app-shell`、`core-uuid`、`feature-chat`、`feature-commerce`、`feature-community`、`feature-home`、`feature-media`、`feature-settings`、`modularity`、`parity/logic-acceptance`、`platform-bridges`、`user-auth`。

来源可完整反推（`find openspec/changes/archive/*/specs -name '*.md'` 与 `find openspec/specs -name '*.md'` 的并集恰好一致）：

| 归档 change | 部署了哪些 capability |
|-------------|----------------------|
| `archive/2026-09-02-migrate-my-ai-to-kmp` | 10 个：app-shell / feature-chat / feature-commerce / feature-community / feature-home / feature-media / feature-settings / modularity / platform-bridges / user-auth |
| `archive/2026-09-29-adopt-cpf-uuid` | `core-uuid` |
| `archive/2026-09-29-logic-first-parity` | `parity/logic-acceptance`（注意是**带子路径**的 capability，正是 `opsx-archive.md:12` 强调「preserve the full path」的那个形态） |

结论：`openspec/specs/` 只反映 3 次归档；9 个在飞 change 的 delta **全部尚未部署**。这是设计使然（未 archive ⇒ 未 sync），但要看清「主 spec 不是当前实现的描述，而是已验收部分的描述」。

### 7.4 现存 change 逐目录状态

状态没有声明字段可查——`.openspec.yaml` 只有 `schema` 与 `created` 两个键（实测 8 个文件皆如此）。下面的判定来自「有无 tasks.md、checkbox 计数、有无 delta specs、文件构成」四项交叉。计数命令：

```bash
awk '/^- \[x\]/{x++} /^- \[ \]/{o++} END{printf "done=%d open=%d\n",x,o}' <file>
```

| change | created | 产物 | 实测状态判定 |
|--------|---------|------|--------------|
| `parity-flutter-to-kmp` | 2026-09-02 | design/proposal/specs(16)/tasks + 三个派生 tasks 文件 + notes | **主力在飞**。四份清单：`tasks.md` 32✓/21☐、`tasks-fine.md` 3✓/159☐、`tasks-ultra-fine.md` 372✓/90☐、`tasks-execution.md` 585✓/0☐。`notes/` 有 9 项（acceptance-matrix、acceptance-report、boundary-gate、evidence、full-parity-phase-spec、parity-inventory、pixel-acceptance-sop、platform-gap-registry、w0-test-accounts）。`tasks-execution.md` 已全绿 ⇒ Gate Z.1.1 PASS，但另三份仍有 21/159/90 条未勾 ⇒ **不可 archive**（见 §7.5 的口径冲突） |
| `sync-flutter-full-parity` | 2026-10-15 | proposal + notes + `tasks-ultra-fine.md`(103✓/66☐) + `tasks-ultra-fine-v2.md`(0✓/162☐) | **在飞，无 delta spec、无 tasks.md**（只有 v1/v2 两份 ultra-fine）。v2 是刚开的新一轮，0 完成；ADR 0005 `:5` 把本篇列为 Related，说明通道分离的落地挂在这里 |
| `sync-flutter-ui-visual` | 2026-09-03 | design/proposal/specs(8)/tasks 27✓/0☐ | **任务全绿、delta 未部署**：8 个 capability（含 `ui-visual-parity`）在 `openspec/specs/` 里没有对应主 spec ⇒ 处在「该 sync + archive」的门口 |
| `architect-production-foundation` | 2026-09-04 | design/proposal/specs(4)/tasks 19✓/0☐ + notes | **同上**。ADR 0001 `:7` 直接指向这个目录；4 个 delta（app-shell/modularity/platform-bridges/user-auth）其实已被 migrate 那次归档部署过，所以它更像一个「架构基座的历史档案」而非待办 |
| `adopt-cpf-ktor-ohos` | 2026-09-13 | design/proposal/specs(1)/tasks 21✓/0☐ + notes | **全绿待部署**：delta 是 `core-network`，主 spec 里没有 ⇒ 未 sync |
| `pixel-match-mine-ui` | 2026-09-03 | design/proposal/specs(2)/tasks 12✓/0☐ + notes | **全绿待部署**：delta 是 `feature-settings`（主 spec 已有）+ `mine-visual-parity`（无） |
| `adopt-cpf-coil-image` | 2026-09-29 | design/proposal/specs(1)/tasks 13✓/0☐ | **全绿待部署**：delta `core-image` 未进主 spec；代码侧已落 `imageCoilMain` 源集（`AGENTS.md:60` 表格行） |
| `unify-core-kv-storage` | 2026-10-16 | **只有** proposal.md + 空的 `specs/` 目录 | **刚提议、未成形**：没有 `.openspec.yaml` 之外的 tasks/design，`specs/` 下 0 个 md ⇒ 连「一个 requirement」都还没写。可执行性上它不具备 archive 条件 |
| `sync-flutter-0929-im-auth-voice` | （无 `.openspec.yaml`） | proposal/design/tasks，`tasks.md` 0✓/74☐，`specs/` **空** | **在飞且从未动过**：74 条全部未勾，且缺 `.openspec.yaml`（唯一一个没有该文件的目录）。IM/语音/认证这条线看起来被 `sync-flutter-full-parity` 接管了 |

一个有用的横向读法：`done` 总数最大的是 `parity-flutter-to-kmp/tasks-execution.md`（585），而它恰好是 `scripts/parity_task_audit.py:14` **唯一硬编码**的文件——门禁只认这一份清单。这解释了为什么「execution 全绿 + fine/ultra-fine 大面积未勾」能同时成立：门禁的口径是 execution 那份，另三份是不同精度的重述，不受 Gate Z.1.1 约束。

### 7.5 parity 工作法（把 ADR 0003 变成可执行的循环）

这套循环里每个环节都有一个仓库内的物理载体：

| 环节 | 载体 | 谁推动 |
|------|------|--------|
| 立范围 | ADR 0003 `:14` 要求「每条在范围内的路由进 Inventory」 | 人/agent 在 grill 里定 |
| 拆单元 | Parity Domain Ticket（小域，约 2–4 条路由），ADR 0003 `:19`；清单在库内、同步到 GitHub issues | `docs/agents/issue-tracker.md:1-12` 给出全套 `gh` 约定；`docs/agents/triage-labels.md:5-11` 给出五个标签（`ready-for-agent` 就是「可交给 AFK agent」） |
| 逐条实现 | `openspec/changes/parity-flutter-to-kmp/tasks*.md` 的 checkbox；apply 工作流每完成一条立刻勾（`opsx-apply.md:92,166`） | agent |
| 取静态证据 | `bash scripts/verify-parity.sh`（词法 + 三轴 + 测试）→ `verify-parity.log`；`bash scripts/parity-evidence.sh <out.md>` 把 JUnit XML 摊成中文表 | 脚本 |
| 取像素证据（Android 硬门） | `python3 scripts/screenshot_diff_gate.py …` 写 `{route}.{flutter,kmp,heat}.png` + `{route}.diff.json`，落 `notes/evidence/<domain>/Android/` | 脚本 |
| 诚实登记缺口 | `notes/platform-gap-registry.md` 的 `missing/partial` 三端列 + 代码里的 `// GAP(R5)` | 人/agent，ADR 0003 `:17` 禁止假 `ready` |
| 巡检上游漂移 | `bash scripts/audit-flutter-drift.sh`（`--record` 才动基线） | 脚本 |
| 归档前审计 | `python3 scripts/parity_task_audit.py` 的 Gate Z.1.1 | 脚本；FAIL 时原话是「continue implementation; **do not archive**」 |
| 部署真相 | `/opsx-archive` 内联 sync → `openspec/specs/` | agent |

**这套工作法里最容易被误读的一点**：`logic-pass` 不等于像素过关。实测 6 个 `pass_=false` 的 `.diff.json` 仍留在 `notes/evidence/**` 里，对应路由在 inventory 中以 `logic-pass` 前进——这是 ADR 0003 `:20`（逻辑优先阶段）明许的状态，不是数据脏。等该阶段在 `CONTEXT.md` / inventory 里被显式结束，pixel 硬门才恢复（`:20` 末句「Do not ship new SoT body bitmaps as primary delivery」也是这条阶段的纪律）。

## 8. 本机约束与坑（只写有证据的）

### 8.1 机器本地文件与镜像

| 事实 | 证据 | 影响 |
|------|------|------|
| `local.properties` 只有一个键 `sdk.dir` | 实测 `sed -n 's/=.*/=<redacted>/p' local.properties` → 单行 `sdk.dir=…` | 这是「同一份仓在两台机器上能不能起来」的唯一必配项；被 `.gitignore:25-26`（`local.properties` + `**/local.properties`）屏蔽；`AGENTS.md:45` 标注「Machine-local SDK paths — never commit」 |
| Android SDK 路径三级回落 | `scripts/run-android.sh:49-70`：`ANDROID_HOME` → `ANDROID_SDK_ROOT` → 从 `local.properties` 里 `sed -n 's/^sdk\.dir=//p'` | 只读 `local.properties` 这一行，不解析其他键；CI 用 `ci.yml:66-67` 现写一个 |
| 两个仓库镜像是**必需的**，不是加速项 | `settings.gradle.kts:7`（pluginManagement）与 `:17`（dependencyResolutionManagement）都排第一位：`https://maven.eazytec-cloud.com/nexus/repository/maven-public/`；`mavenLocal()` 在 `:6` 和 `:16` 被注释掉 | CPF（Compose Multiplatform OHOS 适配线）只在这个 nexus 上：`gradle/libs.versions.toml:31` `kotlinx-serialization = "1.9.1-0.3.0"`、`:33` `ktor = "3.3.3-0.3.0"`、`:41` `coil = "3.3.0-0.3.0"`、`:49` `uuid = "0.8.4-1.0.0"`。这些带 `-0.x.0` 后缀的版本号在 Central 上不存在 |
| 但有些包**必须**回落官方源 | `libs.versions.toml:42-43`：CPF `coil-network-okhttp:3.3.0-0.3.0` 是 metadata-only（无 androidJvm 产物），所以 `coil-okhttp = "3.3.0"` 走 stock Central | 镜像不是「全都换」，而是按包挑；注释里就写着这个反例 |
| 全局 `init.gradle` 会改写所有 settings 的仓库 | `~/.gradle/init.gradle:1-2` 自述目的：「解决国内访问 maven central / google / gradle plugin portal 出现 403」；`:23-33` 在 `gradle.beforeSettings` 里把 aliyun 四个镜像插到 `pluginManagement.repositories` **最前面**（`gradlePluginPortal()` 排在 `:29`，即第 4 位） | 见 §8.2：这条注入正是 buildSrc 走不通的原因 |
| 根工程自己也要防镜像抢解析 | `build.gradle.kts:40-55` 的仓库顺序，其中含 `excludeGroup("com.android.tools.build")` | AGP 明确不让镜像供，避免拿到残缺元数据 |

### 8.2 `buildSrc` 在本机不可用

`build.gradle.kts:25-27` 原话（转述）：共享的 KMP 接线放根工程而不是 buildSrc，因为这台机器被 `~/.gradle/init.gradle` 钉在阿里云 + eazytec 镜像上，`org.gradle.kotlin.kotlin-dsl` 的 plugin marker 在这些源里解析不到——**已实测 `:help` 直接失败**，所以 buildSrc 约定插件这条路走不通。`AGENTS.md:49-53` 独立复述了同一件事，并给出「因此新增一个模块需要：`settings.gradle.kts` include + 自己的 `build.gradle.kts`（plugins + `api(projects…)` + `android { namespace }`）」。

物理佐证：`ls -d buildSrc` → `No such file or directory`（实测）。

这条约束的代价是**可复现性**：接线逻辑住在根 `build.gradle.kts:70-129`（`:94-102` 挂 5 个 native target，`:104-116` 挂 coroutines/serialization/atomicfu/kotlin-test），靠 `:35` 的 `libraryModulePaths` 过滤 + `:59-60`「VersionCatalogsExtension 要到子工程自己求值时才注册，所以只能在回调体里取」这种手法工作。换一台没有该 `init.gradle` 的机器，buildSrc 会变得可用——但那时根工程这套接线也得重写。

### 8.3 Gradle 9：`exec` 已被移除

`build.gradle.kts:14-17` 原话（转述）：wrapper 已升到 Gradle **9.4.1**，`Project.exec` 及脚本级 `exec` 在 Gradle 9.0 被移除（官方迁移指南 "Removal of Project#exec, Project#javaexec, and script-level counterparts"），`deployment.gradle.kts` 内 29 处已全部改为注入的 `ExecOperations` 服务。

实测核对（同一台机器上）：

| 命令 | 结果 |
|------|------|
| `grep -c "execOperations.exec" deployment.gradle.kts` | **30** |
| `grep -c "^\s*exec {" deployment.gradle.kts` | **0** |
| `grep -n distributionUrl gradle/wrapper/gradle-wrapper.properties` | `:3` = `https\://mirrors.tencent.com/gradle/gradle-9.4.1-bin.zip`（wrapper 本体也走国内镜像） |

取用方式在 `deployment.gradle.kts:30-45`：`ExecOperationsHolder` 通过 `objects.newInstance(ExecOperationsHolder::class)` 注入，之后 30 个调用点统一写 `execOperations.exec { … }`。

### 8.4 advisory lock：并发 agent 会互相踩

`scripts/verify-parity.sh:27-46` 与 `scripts/with-gradle-lock.sh:13-29` 是**同一把锁**的两份实现（`mkdir "${TMPDIR:-/tmp}/qoder-gradle-verify.lock"`），算法：`mkdir` 当 test-and-set、`kill -0` 判定持锁者是否已死并回收、10s 轮询、超时由 `GRADLE_LOCK_WAIT`（默认 3600）控制。

为什么必须有：`scripts/with-gradle-lock.sh:2-4` 记录了一次真实事故——**等锁 666 秒后失败，并被误怪成「别人的文件坏了」**。Gradle daemon 的文件锁在同一仓并发时是硬阻塞，而这套工程里同时跑构建的往往是并行 agent。

用法差别：`verify-parity.sh` 把锁内建在流程里（`:188-194`，`tee` 之后 `rmdir_lock` + `trap - EXIT`）；`with-gradle-lock.sh` 是给**窄命令**用的包装器（`:6` 的示例 `scripts/with-gradle-lock.sh ./gradlew :feature:ai:testDebugUnitTest`），`:35-37` 原样透传被包命令的退出码，因此可以安全嵌在别的脚本里。仓内唯一一次实际使用记录在 `.workbuddy/memory/2026-10-20.md:10`（`./gradlew help --console=plain` → 55s、29 模块配置通过）。

### 8.5 任务名「不带冒号」与 configuration cache

`build.gradle.kts:18-19`：deploy/verify 任务都在 `doLast` 里直接起进程，**与 configuration cache 不兼容**且依赖真机/模拟器，「门禁从不跑它们」（三端日常跑用 `scripts/run-{android,ios,harmony}.sh`）；而 `gradle.properties:8` 是 `org.gradle.configuration-cache=true`——两者共存的前提正是门禁不碰这些任务。`verify-parity.sh:175-185` 的任务名故意不带前导冒号（详见 §4.4）。`deployment.gradle.kts:282-283` 断言发布路径，`composeApp/build.gradle.kts:259-298` 是 `publish{Debug,Release}BinariesToHarmonyApp[X64]` 的实现，`:259` 的 `if (!androidOnly)` 与 `build.gradle.kts:57` 的 `skipNative` 同一口径。

### 8.6 OHOS 相关的硬限制

| 限制 | 证据 |
|------|------|
| RELEASE 链接必须关优化 | `composeApp/build.gradle.kts:58-60`（`optimized = false`）；`AGENTS.md:129`「避免 `DevirtualizationAnalysis` OOM……除非有实测余量否则别动」 |
| ohosX64 目前出不了产物 | `composeApp/build.gradle.kts:281-283` task description 原文 `Currently blocked by toolchain: Konan_cxa_demangle on linkDebugSharedOhosX64.`；`platform-gap-registry.md:38` 同口径（无 CPF Ktor/WebSockets klib ⇒ 抛类型化桩异常，保持 GAP(R5)） |
| 渲染后端与链接由属性决定 | `gradle.properties:22` `rendererBackend=fusion-renderer`（`:23` 注释掉的 `skia`）、`:24` `ohosSkikoVersion=0.9.22.2-0.3.0`；`composeApp/build.gradle.kts:66-71` 的 `-lnet_http` **只对 ohosX64** 追加，`:76-91` 是 fusion-renderer 链接清单；`AGENTS.md:118` 要求「linker opts 跟着这个 flag 走」 |
| 原生缓存必须关 | `gradle.properties:16` `kotlin.native.cacheKind=none` |
| DevEco 版本是硬门 | `runscript/runOhosApp-Mac.sh:99-131` `MIN_VERSION="6.0.0"`，低于 `exit 5`；经 `scripts/run-harmony.sh:19-21` 只在 Darwin 走通 |

### 8.7 静态门禁依赖的解释器版本

实测：`bash --version` → **3.2.57(1)-release**（macOS 自带，非 bash 4/5）；`python3 --version` → **3.9.6**；Pillow **11.3.0**；numpy **2.0.2**。三条由此得出的约束：

1. 12 个 `scripts/*.sh` 一律 `#!/usr/bin/env bash`，`runscript/runOhosApp-Mac.sh` 是 `#!/bin/bash`（逐个 `head -1` 实测）。13/13 `bash -n` 通过，且 `grep -rn "declare -A\|local -A" scripts/ runscript/` **无命中** ⇒ 确实只用 bash 3.2 能吃的语法；新门禁想当然用 `declare -A` 会在这台机器上直接炸。
2. `screenshot_diff_gate.py` 依赖 Pillow + numpy，用 `Image.Resampling.BILINEAR`（`:42`）与 `Image.fromarray(..., mode=...)`（`:41,107`）。
3. **Pillow 13 会打断 #10**：实跑 `python3 scripts/test_screenshot_diff_gate.py` → `test_screenshot_diff_gate: OK`（退出码 0），但 stderr 带 `DeprecationWarning: 'mode' parameter is deprecated and will be removed in Pillow 13 (2026-10-15)`（源 `:19`）。升级前要先去掉 `mode` 位置参数。

### 8.8 其余有仓内出处的坑

| 坑 | 证据 |
|----|------|
| 带空格模拟器名 / `#` 号 SDK 路径 | `run-android.sh:49-70` 的 `sed` 只截 `sdk.dir=` 行；`run-ios.sh:72-115` 用 `\(([A-F0-9-]{36})\)` 从 `simctl list` 抠 UDID 而非名字 |
| 签名 team 占位符未替换会拿到脏 bundle id | `run-ios.sh:57-68`：读 `Config.xcconfig` 后显式减掉 `$(TEAM_ID)` |
| Xcode 派生数据必须收进仓内 | `run-ios.sh:136-147` 强制 `-derivedDataPath $ROOT/build/ios-sim` |
| HAP 装不上要先卸再装；屏锁错误码固定 | `runOhosApp-Mac.sh:241-256`、`:273-301`（识别 `10106102` 后 `exit 1`） |
| 无签名 HAP / 设备离线 / 平台不支持是三个退出码 | 同上 `:207-231` `exit 6`、`:232-236` `exit 5`、`:162-182` `exit 4` |
| 改图标后 launcher 显示缓存 | `AGENTS.md:98`「uninstall/reinstall」；`run-ios.sh:161-165` 每次都是 uninstall→install→launch |
| 改共享 Kotlin 必须先 publish 再在 DevEco 跑 | `AGENTS.md:127`（并警告 `Index.ets` 载入的 `MainArkUIViewController` 符号名除非两边同改否则别动） |
| compileSdk 36 会告警、慢镜像要加长超时 | `gradle.properties:14` `suppressUnsupportedCompileSdk=36`、`:18-19` http 超时 100000ms；`ci.yml:63-64` 显式装 `platforms;android-36 build-tools;35.0.0` |
| 上游 Flutter 仓是硬前提 | `audit-flutter-drift.sh:25` `FLUTTER_REPO:-/Users/qpy/Desktop/github/my_ai_project`，`:38-41` 不存在即 `exit 2`；`sync-flutter-assets.sh` 同带 `FLUTTER_ROOT` |
| 资源同步脚本会写进仓 | `sync-flutter-assets.sh:16` 目标 `core/design/src/commonMain/composeResources/drawable`；`:19`+`:110-122` 只有 Phase 1 有 `DRY_RUN=1`。本文全程未执行它（§9 疑点 3） |
| 旧英文禁词只能按片段拼装 | `sync-flutter-assets.sh:38` `LEGACY_TOKEN = "dub" + "bing"` + `:40-47` `sanitize()`；`.workbuddy/memory/MEMORY.md:17` 警告「删了这段映射，重跑同步会把旧名重新生成一份」 |
| 半数门禁没有可执行位，只能 `bash`/`python3` 起 | 实测 `ls -l scripts/`：`check-ios-page-dispatch.sh`、`check-harmony-pane-dispatch.sh`、`test_screenshot_diff_gate.py` 是 `644`，其余 12 个是 `755` ⇒ 本文所有实跑命令一律写 `bash scripts/x.sh` / `python3 scripts/x.py`，不依赖 `./scripts/x.sh` |

## 9. 未验证疑点

| # | 疑点 | 为什么没验证 |
|---|------|--------------|
| 1 | `openspec` CLI 自身行为（`list/status/instructions/archive` 的实际输出） | 本机是否有该可执行文件未确认。§7.2 引的是 `.cursor/commands/opsx-*.md` 里对 CLI 的**调用约定**；归档目录名与 `.openspec.yaml` 内容只能反推，不能证实由哪条命令产出 |
| 2 | `ci.yml:6` 的 `on.push.tags: v*` 与 `:7-34` 的 `paths` 白名单并存时，paths 对 tag push 是否生效 | 要真推一个 tag 才能观测，超出只读约束。§5.3 因此只描述配置、不断言 `v*` 会被拦 |
| 3 | `sync-flutter-assets.sh` Phase 1（`:9-127`）/ Phase 2（`:129-238`）的实际产出 | 会往 `core/design/…/drawable` 与三端原生资源目录写文件；`DRY_RUN=1` 只覆盖 Phase 1 的映射表分支，Phase 2 无等价开关 |
| 4 | 缺失门禁该由谁补（托管 CI 还是本机 pre-commit/agent） | `tasks-ultra-fine-v2.md:310`（M13.3）只写「CI 增列两步」且动机句已过期（§5.5 末）；没有文件指定 iOS/Harmony 派发门的执行主体 |
| 5 | `verify-parity.sh:188-232` 的 Gradle 阶段与 JUnit 聚合分支今日是否仍绿 | 未跑 Gradle。§4.5 的 664 tests / 70 类读数来自仓内既有 XML，时间戳属 2026-10-17 01:21 那次全量跑 |
| 6 | drift 轴 (c) 的 6 条改名导致的 missing 是否已被接受为长期噪声 | 脚本里没有豁免表（`--record` 只动基线 `:128`），仓内也无 issue 记录这条口径冲突 |
| 7 | ADR 0005 `:32` 承诺的两条静态检查是否有落地计划 | `tasks-ultra-fine*.md` 未见对应任务；`notes/boundary-gate.md` 是否覆盖此语义未逐行确认 |
| 8 | `screenshot_diff_gate.py` 的 `--top/--bot/--width/--height` 偏离默认后，与既有 75 份 `.diff.json` 是否仍可比 | `:121` 的 meta 记了 ROI 参数，但仓内没有「参数变更 ⇒ 全量重跑」的记录，跨参数比较口径未定 |

