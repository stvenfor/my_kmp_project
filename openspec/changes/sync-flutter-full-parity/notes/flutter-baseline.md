# Flutter 上游基线（SoT HEAD）

> 本文件记录 KMP 侧对齐的 `my_ai_project` commit，供 `scripts/audit-flutter-drift.sh` 读取比对。
> 只有人工执行 `bash scripts/audit-flutter-drift.sh --record` 才会更新这里的 HEAD；脚本默认只读不改。

- SoT 仓库：`my_ai_project`（只读产品真相源，本机路径 `/Users/qpy/Desktop/github/my_ai_project`）
- 基线 HEAD：`d7876a1d105e6bcff4b4a6d5367519e0bbcc8ecb`
- 基线简称：`d7876a1`
- 基线说明：`feat(auth): unify 401/403 session decisions and wys_network forbidden handling`
- 记录日期：2026-10-17
- 待办真相源（路由/入口表）：`commons/wys_router/lib/src/route/route_path.dart`（103 条 RoutePath）
- 端点真相源：Dart 全仓 `'/api/v1/…'` + `'/im/…'` 字符串字面量（见 `notes/api-contract-2026-10-16.md`）

<!-- 脚本按行解析下面这一行，勿改 key 名： -->
baseline_head: d7876a1d105e6bcff4b4a6d5367519e0bbcc8ecb

## 说明

`tasks-ultra-fine-v2.md` 顶部声明「SoT：`my_ai_project` @ `d7876a1`（工作树干净，无新增 commit）」，
但此前没有独立的 HEAD 记录文件。本文件补上该缺口：`audit-flutter-drift.sh` 用它判断上游是否前进，
并在 Flutter `HEAD` 前进时 diff 路由表与 `/api/v1` + `/im` 端点集合（M14.3）。
