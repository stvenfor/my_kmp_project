# 03 — 闪屏 + 隐私 + 四 Tab 壳（三端）

**What to build:** 用户经历闪屏 → 隐私同意 → 带四个 Tab（首页 / 聊天 / 社区 / 我的）的主壳；Tab 可切换；壳层视觉对照 Flutter 达到 UI Parity Bar。

**Blocked by:** 02 — 主路径改走原生壳（切断 Legacy 主路径）

**Status:** done

- [ ] 闪屏与隐私流程在三端可走通，行为对齐 Flutter（隐私先于主壳；不强制冷启动登录）
- [ ] 四 Tab 壳可切换，选中态与布局对照 Flutter 误差 ≤ 2%
- [ ] 使用 Shared Design Tokens，无端侧私有色板漂移
