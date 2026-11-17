# 01 · 总览与学习路线

> 适用仓库：`/Users/qpy/Desktop/github/my_kmp_project`（Kotlin Multiplatform + Compose Multiplatform，三端 Android / iOS / HarmonyOS）  
> 规模实测：29 个 Gradle 工程、359 个 `.kt`、62 个测试文件、7 个 Swift 文件、21 个 ArkTS 文件、31 个 `build.gradle.kts`  
> 本页是全套学习的目录，其余 6 页按阶段展开。

## 一句话定位

这个仓库不是一个「Demo」，而是一套**正在做跨语言对齐工程**的 KMP 单体：业务行为以上游 Flutter 仓库为真相源，KMP 侧负责用自身架构重新实现，并且用一组脚本把「实现对不对」变成可执行门禁。

## 必须先接受的三个反直觉前提

| 前提 | 含义 | 出处 |
|-|-|-|
| 产品真相不在本仓库 | 业务行为以 `/Users/qpy/Desktop/github/my_ai_project`（Flutter，HEAD `d7876a1`）为准。KMP 侧任何「看起来更合理」的实现，都要先用 Dart 的 `file:line` 证伪 | `openspec/changes/sync-flutter-full-parity/tasks-ultra-fine-v2.md` 顶部 |
| KMP 不等于三端同一套代码 | 公共代码只占约三分之一；OHOS 用的是 eazytec fork（`kotlin 2.2.21-0.3.0`、`compose 1.9.2-0.3.0`），多个库在 ohosX64 上根本没有产物，于是衍生出「中间源集」这套补丁机制 | `gradle/libs.versions.toml` |
| 只有 `:composeApp` 产二进制 | iOS framework、OHOS `libkn.so` 都从这一个模块出去，其余 28 个模块是库。改共享代码后必须先 publish 再跑 DevEco | `AGENTS.md` 的 OHOS publish contract 一节 |

不接受这三条，后面读代码会一路误解：会以为 `commonMain` 是全部、会以为改完直接跑设备就行、会以为 UI 好看就算对齐。

## 学习阶段总表

| 阶段 | 目标 | 产出验收 | 对应页 |
|-|-|-|-|
| 第 0 步（30 分钟） | 建立正确心智模型 | 读完 `AGENTS.md`、`MODULES.md`、5 篇 ADR | 本页 |
| 第 1 步（半天） | 三端跑通 + 门禁绿 | `verify-parity: OK` | 02 |
| 第 2 步（1–2 天） | 补齐 KMP 前置知识 | 能自绘源集 dependsOn 图 | 03 |
| 第 3 步（1 天） | 读懂分层与 29 模块 | 能说清依赖方向 | 03 |
| 第 4 步（3 天） | 跟读四条主干线 | 每条线讲清一个易改坏语义 | 04 |
| 第 5 步（1 天） | 读懂验证体系 | 会跑并会解释 9 个脚本 | 05 |
| 第 6 步（1 天） | 读懂工程流程 | 理解 R5 与 gap registry | 05 |
| 第 7 步（3–5 天） | 三个动手练习 | 练习 3 三端同源且脚本绿 | 06 |

## 建议时间表

| 天 | 内容 | 结束标准 |
|-|-|-|
| D1 | 第 0 + 1 步 | 三端跑起来、门禁绿 |
| D2–D3 | 第 2 步 | 能画出源集 dependsOn 图 |
| D4 | 第 3 步 | 能说清 29 个模块的层与方向 |
| D5–D7 | 第 4 步 | 每条线讲清一个「容易顺手改坏」的语义 |
| D8 | 第 5 + 6 步 | 能解释 R5 与 gap registry |
| D9–D13 | 第 7 步 | 练习 3 三端同源 + 两个分发脚本绿 |

## 终检 6 问（答不上就回到对应页）

1. 为什么 `:composeApp` 是唯一二进制产出方？改了共享代码下一步做什么？
2. `KeyValueStore` 为什么是 KV 唯一契约，而 `multiplatform-settings` 不能同时服务 Android 和 OHOS？
3. AI / realtime / chat 为什么必须三条通道？失败语义能否互相翻译？
4. 为什么禁止裸 `TopAppBar`？`ImmersiveInsets.shellContentInsets` 在 `bottomBarVisible` 为 true / false 时分别给什么？
5. `verify-parity.sh` 里任务名前缀冒号的有无，各自导致什么后果？
6. 缺外部 SDK（融云 AppKey、微信 appId、JPush、Go BFF）时的正确收尾动作是什么？

## 方法论收口

这个项目的知识不在代码里，而在「代码 + 门禁 + Dart 真相源」三者的一致性里。  
读代码只完成 40%，剩下 60% 是跑脚本和追 `file:line` 证据。

## 术语最小集

| 术语 | 含义 |
|-|-|
| SoT | Source of truth，本流程指上游 Flutter 仓库 |
| Parity | 行为对齐：同输入下 KMP 与 Flutter 的可观测输出等价 |
| 中间源集 | 为绕开某端缺产物而新建的 `commonMain` 与平台源集之间的过渡源集 |
| Compose 岛 | 原生壳里嵌一小块 Compose Multiplatform UI 的做法（ADR-0002） |
| gap registry | 平台能力缺口登记表，状态取 `ready` / `partial` / `missing` / `stub` |
| R5 | 诚实性规则：环境缺真 SDK 时登记 gap，绝不伪称 ready |
| 门禁 gate | 可执行的校验脚本，CI 与本地共用同一入口 |
