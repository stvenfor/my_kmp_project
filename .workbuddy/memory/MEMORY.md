# 项目长期记忆（my_kmp_project）

## 双项目关系（2026-10-14 用户确认）

- **当前项目**：`/Users/qpy/Desktop/github/my_kmp_project`（GitHub: `stvenfor/my_kmp_project`），KMP 三端工程（Android/iOS/HarmonyOS），正在进行 Flutter `my_ai_project` → KMP 全量对齐；产品边界见 `CONTEXT.md`（Native Shell UI + Mine Compose Island + Shared Business Logic）。
- **目标/参考项目**：`/Users/qpy/Desktop/github/kmp-cmp-example`（gitcode: `CPF-KMP-CMP/kmp-cmp-example`，开源 KMP 示例工程，本地有副本）。
- **约定**：当前项目的扩展会用到目标项目的内容——**一部分借鉴/移植，一部分仅作参考**。目标项目不是运行时依赖，是参考源。
- 已有对齐痕迹：`composeApp/build.gradle.kts` 的 OHOS 二进制发布任务（`publish*BinariesToHarmonyApp` / `...X64`）注释标注"CPF 对齐"，即参照目标项目做法。
- 目标项目的核心参考价值（HarmonyOS 集成范式）：NAPI 桥接（`@CName` → `napi_init.cpp` 注册）、`libkn.so`/`libkn_api.h` 发布到 `harmonyapp/entry`、汇编 `.incbin` 嵌二进制资源进静态库、kexe 设备运行验证、`deployAndroid/deployIos/deployHarmony` 部署任务链。
- 不确定某项属于"借鉴/移植"还是"仅参考"时，先问用户，不擅自决定。

## 禁词令（2026-10-20 用户指令，长期有效）

- **规则**：项目内禁止出现某三个中文词根（竞品功能词，原文见当日用户消息，本文一律不复述）。已有使用其它文字替换；新增代码/资源/文档/截图一律不得引入。
- **已执行**：主力禁词等长替换为「跟读」（103 处 / 22 文件文本层）；5 张烙字截图 9 处像素级替换（详见 2026-10-20 日志）。
- **英文同源词已改（2026-10-22 用户指令，全大小写）**：该词的英文形态统一改为 `transliter` / `Transliter` / `TRANSLITER`。覆盖 Kotlin 标识符与路由字符串、Swift/ArkTS、三端资源文件名（compose drawable / iOS imageset / OHOS media 各 58 个）、`.imageset` 目录名、openspec 文档与 evidence、`.scratch` 截图，共 222 个文件改名 + 100 文件 733 处文本。全仓 `?i` 校验残留为 0。
- **同步脚本注意**：Flutter SoT 侧资源路径仍带该旧词，`scripts/sync-flutter-assets.sh` 的 `sanitize()` 里有一段映射（旧词按片段拼装，避免把禁词写回仓库），改脚本时别删。
- **代价**：路由字符串已随之改名，与 Flutter SoT 的 route 表不再是逐字一致（行为等价，仅字面不同）。
- **教训**：禁词不只存在于文本——从 Flutter SoT 同步的截图资产可能烙字，改资源时要连图片一起查；另 `grep "a\|b"` 交替在本环境 BSD grep 下失效，搜多词必须 `grep -E` 或 Grep 工具（ripgrep 默认跳过 gitignore，查 .workbuddy 要 `--no-ignore`）。批量改名时**别只遍历 `is_file()`**——目录名（如 `.imageset`）会漏掉。
