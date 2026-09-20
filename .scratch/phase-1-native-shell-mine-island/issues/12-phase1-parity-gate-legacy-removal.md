# 12 — Phase-1 视觉验收清单 + Legacy 删除闸

**What to build:** 对一期全部必交表面（闪屏、隐私、四 Tab 根页、Auth、Mine 岛页）完成对照 Flutter 的 UI Parity Bar 验收；通过后方可删除 Legacy Shared Compose 主路径残留，避免双 SoT 长期并存。

**Blocked by:** 06 — Home Tab 根页；07 — Chat Tab 根页 + 原生门闸；08 — Community Tab 根页 + 原生门闸；09 — Mine Root 三端原生；11 — Mine Compose Island 页面集

**Status:** done

- [ ] 一期表面均有对照 Flutter 的验收记录（或等价清单勾选），误差 ≤ 2%
- [ ] 确认主路径无依赖 Legacy Shared Compose
- [ ] Legacy Shared Compose 产品主路径残留已删除或已开出明确收尾子任务且无运行时引用
