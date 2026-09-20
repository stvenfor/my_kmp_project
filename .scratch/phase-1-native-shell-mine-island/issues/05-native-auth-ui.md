# 05 — Auth UI 三端原生（登录/注册）

**What to build:** 登录与注册为各端 Native Shell UI；用户可完成登录/注册并回到原意图 Tab；认证流绝不进入 Mine Compose Island。

**Blocked by:** 03 — 闪屏 + 隐私 + 四 Tab 壳（三端）；04 — Shared Presentation：会话 + Soft-auth 状态

**Status:** done

- [ ] 三端均有原生登录与注册，视觉对照 Flutter 误差 ≤ 2%
- [ ] 登录成功后清除 soft-auth 门闸并展示对应 Tab 内容
- [ ] 认证路由不注册、不导航进 Mine Compose Island
