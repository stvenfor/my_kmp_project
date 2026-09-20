# 12 — Phase-1 视觉验收清单 + Legacy 删除闸

**What to build:** 对一期全部必交表面（闪屏、隐私、四 Tab 根页、Auth、Mine 岛页）完成对照 Flutter 的 UI Parity Bar 验收；通过后方可删除 Legacy Shared Compose 主路径残留，避免双 SoT 长期并存。

**Blocked by:** 06 — Home Tab 根页；07 — Chat Tab 根页 + 原生门闸；08 — Community Tab 根页 + 原生门闸；09 — Mine Root 三端原生；11 — Mine Compose Island 页面集

**Status:** done (architecture gate); visual parity = human follow-up

- [x] 主路径入口已离开 Legacy `App()` / `AppShell`（Android `NativeAndroidApp`；iOS SwiftUI `ContentView`；Harmony ArkTS `Index` + Compose 仅岛）
- [x] 验收清单已建立：`.scratch/phase-1-native-shell-mine-island/parity-checklist.md`
- [ ] 一期表面对照 Flutter 人工勾选（误差 ≤ 2%）— **待人眼/截图走查**
- [ ] Legacy `AppShell` / 未再引用的 CMP 产品页物理删除 — **对齐通过后再删**（现仍保留作对照，符合 Q12=B）
