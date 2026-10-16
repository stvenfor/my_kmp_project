# 项目长期记忆（my_kmp_project）

## 双项目关系（2026-10-14 用户确认）

- **当前项目**：`/Users/qpy/Desktop/github/my_kmp_project`（GitHub: `stvenfor/my_kmp_project`），KMP 三端工程（Android/iOS/HarmonyOS），正在进行 Flutter `my_ai_project` → KMP 全量对齐；产品边界见 `CONTEXT.md`（Native Shell UI + Mine Compose Island + Shared Business Logic）。
- **目标/参考项目**：`/Users/qpy/Desktop/github/kmp-cmp-example`（gitcode: `CPF-KMP-CMP/kmp-cmp-example`，开源 KMP 示例工程，本地有副本）。
- **约定**：当前项目的扩展会用到目标项目的内容——**一部分借鉴/移植，一部分仅作参考**。目标项目不是运行时依赖，是参考源。
- 已有对齐痕迹：`composeApp/build.gradle.kts` 的 OHOS 二进制发布任务（`publish*BinariesToHarmonyApp` / `...X64`）注释标注"CPF 对齐"，即参照目标项目做法。
- 目标项目的核心参考价值（HarmonyOS 集成范式）：NAPI 桥接（`@CName` → `napi_init.cpp` 注册）、`libkn.so`/`libkn_api.h` 发布到 `harmonyapp/entry`、汇编 `.incbin` 嵌二进制资源进静态库、kexe 设备运行验证、`deployAndroid/deployIos/deployHarmony` 部署任务链。
- 不确定某项属于"借鉴/移植"还是"仅参考"时，先问用户，不擅自决定。
