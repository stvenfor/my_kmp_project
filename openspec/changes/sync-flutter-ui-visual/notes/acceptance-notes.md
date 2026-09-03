# Acceptance notes — sync-flutter-ui-visual

Updated: 2026-09-03

## Diagnosis confirmed
Gap was **both** missing assets and skeleton UI. Flutter product packs (`home` ~78, `pay` ~31, …) are now synced into `composeResources/drawable` (~114 files) via `./scripts/sync-flutter-assets.sh` + `notes/asset-sync-map.md`.

## Android KMP evidence (this change)
| Surface | Path |
|---|---|
| Shell tabs | `notes/evidence/shell/Android/kmp_tabs.png` |
| Home root / icons / all-services | `notes/evidence/home/Android/` |
| Community feed | `notes/evidence/community/Android/kmp_feed.png` |
| Chat list | `notes/evidence/chat/Android/kmp_list.png` |
| Mine root | `notes/evidence/mine/Android/kmp_root.png` |
| Membership + pay icons | `notes/evidence/commerce/Android/kmp_membership.png` |

Flutter side-by-side `flutter.png` pairs: **pending** (pixel SOP still requires Flutter captures on same device class).

## Remaining open tasks
- Flutter side-by-side `flutter.png` pairs (pixel SOP)
- Dedicated Cupertino-equivalent tab PNGs (material-icons not OHOS-safe)
- iOS device visual spot-check

## OHOS
`publishDebugBinariesToHarmonyApp` was run in this apply wave — see session build log for SUCCESS/FAILURE; re-run after further Compose resource churn before DevEco.

## Cross-link
See `parity-flutter-to-kmp/notes/acceptance-matrix.md` — visual Partial rows should point here for Android KMP captures; 2A SDK work stays on parity change.
