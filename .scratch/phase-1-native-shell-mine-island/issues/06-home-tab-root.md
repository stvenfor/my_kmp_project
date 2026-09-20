# 06 — Home Tab 根页三端原生（含后置占位入口）

**What to build:** 首页 Tab 根页三端原生对齐 Flutter（布局与主入口做满）；指向一期后置 feature 的入口仍可见，点击进入 Deferred Destination Stub（原生占位或安全 no-op），不进岛、不实现完整目标页。

**Blocked by:** 03 — 闪屏 + 隐私 + 四 Tab 壳（三端）；01 — Shared Design Tokens 落地

**Status:** done

- [ ] Home 根页对照 Flutter 误差 ≤ 2%（根页本身，不含后置深页）
- [ ] 后置业务入口视觉保留；点击为原生占位/no-op
- [ ] 根页数据/空错态由 Shared Presentation Logic 驱动
