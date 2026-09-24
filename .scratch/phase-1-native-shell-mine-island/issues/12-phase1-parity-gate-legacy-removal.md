# 12 — Phase-1 视觉验收清单 + Legacy 删除闸

**What to build:** 对一期全部必交表面完成对照 Flutter 的 UI Parity Bar 验收；通过后方可删除 Legacy Shared Compose。

**Blocked by:** 06–11

**Status:** architecture gate closed (ADR 0002 ownership enforced 2026-09-21)

- [x] 主路径入口已离开 Legacy `App()` / `AppShell` / `SyncedProductShell`
- [x] Android Jetpack 壳（`NativeAndroidMain`）+ iOS SwiftUI + Harmony ArkTS 四 Tab 根页
- [x] Compose 仅 `MineIsland`；延期入口三端原生占位
- [x] 验收清单：`.scratch/phase-1-native-shell-mine-island/parity-checklist.md`
- [x] 一期表面 Android 对照（emulator-5554，内容区 MSE）：Home **1.93%**、Chat **0.93%**、Community **1.38%**、Mine **1.96%** — 全部通过（2026-09-24）
- [ ] Legacy commonMain 产品页物理删除 — Android 像素闸已过，可执行（Q12=B）；iOS/Harmony 仍为 scaled follow-up
