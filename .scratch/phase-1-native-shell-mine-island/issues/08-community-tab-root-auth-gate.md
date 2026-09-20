# 08 — Community Tab 根页 + 原生门闸

**What to build:** 已登录用户看到对齐 Flutter 的社区 Tab 根页；未登录用户看到原生 Auth 门闸并可进入原生登录；行为与 Chat 侧 soft-auth 一致，且不进入 Mine Compose Island。

**Blocked by:** 03 — 闪屏 + 隐私 + 四 Tab 壳（三端）；05 — Auth UI 三端原生（登录/注册）

**Status:** done

- [ ] 已登录：Community 根页对照 Flutter 误差 ≤ 2%
- [ ] 未登录：展示原生门闸，可导航至原生登录
- [ ] 登录返回后展示 Community 根页内容，无卡在门闸
