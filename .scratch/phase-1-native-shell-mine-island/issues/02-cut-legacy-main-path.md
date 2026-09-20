# 02 — 主路径改走原生壳（切断 Legacy 主路径）

**What to build:** 应用冷启动后的主路径进入 Native Shell UI 骨架，不再把 Legacy Shared Compose 当作运行时主壳；旧共享 Compose 仍可留在仓库作对照，但不被主路径依赖。

**Blocked by:** 01 — Shared Design Tokens 落地

**Status:** done

- [ ] 三端主入口进入原生壳路径（可先为最小可运行骨架）
- [ ] Legacy Shared Compose 不再作为默认主路径渲染壳/Tab
- [ ] Legacy Shared Compose 仍可被开发者对照查看（未强制删除）
