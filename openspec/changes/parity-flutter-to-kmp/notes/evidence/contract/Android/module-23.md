# Module — Contract drop Legacy non-Mine SecondaryRouteIsland (#23)

## Ownership after cutover

| Target | Non-Mine secondaries | Mine secondaries |
|--------|----------------------|------------------|
| Android | `NativeAndroidMain` overlays (`HomeRouteHost` / `CommunityRouteHost` / `ContentRouteHost` / …) | `MineIsland` |
| iOS | `NativeFeatureHost` only (SecondaryRouteHost removed from `ContentView`) | `MineIslandViewController` |
| OHOS | ArkTS `nativePane` / `nativePaneIdFor` → `feature` fallback | Compose Mine island host (`openComposeHost(0,…)`) |
| Shared | `SecondaryRouteIsland` hosts **MineRouteHost only**; non-Mine → blocked page | Mine stack may still navigate mall/wallet/pay children |

## Smoke (Android deeplink, 2026-09-29)

Sampled: `home/search`, `home/all_services`, `wallet`, `mall`, `friend`, `web`, `classroom/my_class`, `video/short`, `ai/stream`, `community/publish`, `settings` — all opened product UI (not Legacy blocked page).

## Docs

- ADR 0002 §8 / ADR 0003 consequences updated
- `parity-inventory.md` #23 note
