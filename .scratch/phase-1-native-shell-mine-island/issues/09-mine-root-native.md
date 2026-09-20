# 09 — Mine Root 三端原生

**What to build:** 「我的」Tab 主页为三端原生（非共享 Compose），对照 Flutter 达到 UI Parity Bar；可发出打开 Mine Compose Island 路由或 Deferred Destination Stub 的导航意图。

**Blocked by:** 03 — 闪屏 + 隐私 + 四 Tab 壳（三端）；01 — Shared Design Tokens 落地；04 — Shared Presentation：会话 + Soft-auth 状态

**Status:** done

- [ ] Mine Root 三端原生实现，对照 Flutter 误差 ≤ 2%
- [ ] 二级入口可触发「开岛」意图；后置跨 feature 入口触发原生占位意图
- [ ] Mine Root 不使用 Mine Compose Island 渲染自身
