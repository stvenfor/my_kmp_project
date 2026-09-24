# Phase-1 UI Parity Checklist (ADR 0002)

Visual Source of Truth: Flutter `my_ai_project`. Bar: ≤2% error.

## Ownership (enforced)

| Layer | Android | iOS | Harmony |
|-------|---------|-----|---------|
| Splash / Privacy | Jetpack | SwiftUI | ArkTS |
| Tab shell + Home/Chat/Community/Mine **roots** | Jetpack | SwiftUI | ArkTS |
| Auth / soft gate | Jetpack | SwiftUI | ArkTS |
| Deferred root entries | Jetpack stub | SwiftUI stub | ArkTS stub |
| Mine **secondary** only | CMP `MineIsland` | CMP host | CMP host |

| Surface | Android | iOS | Harmony | Notes |
|---------|---------|-----|---------|-------|
| Splash | [x] | [ ] | [ ] | Native shells exist. Same-device pixel gate is Android-only. |
| Privacy | [x] | [ ] | [ ] | Native. iOS/Harmony screenshots passed the consent screen before roots. |
| Tab shell | [x] | [ ] | [ ] | Custom 49dp bar + Cupertino-ish `TabIcons` + selected pill (44×28 @ 12% Accent). |
| Home root | [x] | [ ] | [ ] | Android **1.93%**. Layout tuned to SoT `01-home.png` (banner/feature rhythm). Latest Flutter `HomeFeatureGrid` (44dp / max 9) deferred until fresh SoT. |
| Chat root | [x] | [ ] | [ ] | Android **0.93%**. |
| Community root | [x] | [ ] | [ ] | Android **1.38%** (16:9 video, feed media crops, reply rich text). |
| Mine Root | [x] | [ ] | [ ] | Android **1.96%** via CMP `MineHomeContent`. |
| Auth | [x] | [ ] | [ ] | Native. |
| Deferred stub | [x] | [ ] | [ ] | 「一期后置 · 原生占位」 |
| Mine Island | [x] | [ ] | [ ] | CMP host wired. |
| Shared Design Tokens | [x] | [x] | [x] | `:core:design` |

## Android pixel gate (emulator-5554, 2026-09-24)

Content ROI crops status bar and system nav. Pass = MSE ≤ 2% or mean-abs ≤ 2%.

| Surface | MSE | Mean abs | Pass |
|---------|-----|----------|------|
| Home | 1.93% | 4.34% | yes |
| Chat | 0.93% | 1.50% | yes |
| Community | 1.38% | 3.90% | yes |
| Mine | 1.96% | 3.59% | yes |

Shots: `.scratch/phase-1-native-shell-mine-island/screenshots/{flutter,kmp}/`. Report: `screenshots/diff/parity-report.json`.

## Legacy Shared Compose removal gate

- [x] Main path does not use `App()` / `AppShell` / `SyncedProductShell`
- [ ] Delete leftover commonMain product screens after pixel accept (Q12=B) — **Android four-tab roots now pass; deletion unblocked**
