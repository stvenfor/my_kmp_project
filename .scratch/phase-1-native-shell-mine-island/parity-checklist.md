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
| Splash | [x] | [x] | [x] | Native |
| Privacy | [x] | [x] | [x] | Native |
| Tab shell | [x] | [x] | [x] | Native |
| Home root | [x] | [x] | [x] | Native; Android uses `home_feature_*.png` |
| Chat root | [x] | [x] | [x] | Native + soft-auth |
| Community root | [x] | [x] | [x] | Native + soft-auth |
| Mine Root | [x] | [x] | [x] | Native |
| Auth | [x] | [x] | [x] | Native |
| Deferred stub | [x] | [x] | [x] | 「一期后置 · 原生占位」 |
| Mine Island | [x] | [x] | [x] | Shared CMP only |
| Shared Design Tokens | [x] | [x] | [x] | `:core:design` |

## Legacy Shared Compose removal gate

- [x] Main path does not use `App()` / `AppShell` / `SyncedProductShell`
- [ ] Delete leftover commonMain product screens after pixel accept (Q12=B)
