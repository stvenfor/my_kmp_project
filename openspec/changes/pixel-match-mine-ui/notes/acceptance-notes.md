# Acceptance — pixel-match-mine-ui

**Change:** `pixel-match-mine-ui`  
**Schema:** spec-driven  
**Date:** 2026-09-03

## Result

| Target | Result | Evidence |
|--------|--------|----------|
| Android | **Partial** (structure Pass; copy/session name not gold-locked) | `notes/evidence/android/mine_root.png` vs `notes/evidence/flutter/mine_root.png` |
| iOS | Partial | not device-verified this change |
| OHOS | Partial | no new drawable resources; custom ImageVector only — publish not required; not device-verified |

## Android first-viewport checklist

| Element | Flutter gold | KMP Android | Notes |
|---------|--------------|-------------|-------|
| Large title「我的」+ 4 trailing icons | ✓ | ✓ | Custom `MineIcons` (OHOS-safe) |
| Profile card (avatar / badge / store / 电子名片 / phone) | ✓ | ✓ | Session name may differ (`a451` vs gold `用户0000`) |
| Stats 1028 / 28 / 2059 / 9366 | ✓ | ✓ | |
| 常用服务 + HOT | ✓ | ✓ | |
| 个人功能 2-col + 短信模板 / 购车计算器 `5830.00` + reorder hint | ✓ | ✓ | Reorder = toast stub |
| Tab「我的」selected | ✓ | ✓ | |

## Gaps (do not claim 1A Pass)

- Display name / store / phone follow live session when logged in; gold demo strings are fallback only.
- Long-press drag reorder not implemented (hint + snackbar only).
- iOS / OHOS pixel not captured.
- Avatar still placeholder glyph (matches Flutter guest/placeholder treatment).

## Cross-links

- Widget map: `notes/widget-map.md`
- Parity matrix Mine row: `../parity-flutter-to-kmp/notes/acceptance-matrix.md` → `mine` / Android **Partial**
- Compile: `./gradlew :composeApp:compileDebugKotlinAndroid` (Pass)
