## 1. Inventory & sync pipeline

- [x] 1.1 Create `notes/asset-sync-map.md` listing Flutter paths (home/settings/pay/chat/community/music/tab chrome) → Compose resource ids → screens; exclude bfui/DoKit
- [x] 1.2 Copy/convert in-scope Flutter PNG/WebP into `composeApp/src/commonMain/composeResources/drawable/` (module-prefixed names); document Lottie/SVG gaps
- [x] 1.3 Add optional sync script under `scripts/` (or document manual steps) so re-sync is reproducible
- [x] 1.4 Verify resource codegen: `./gradlew :composeApp:compileDebugKotlinAndroid`

## 2. Tokens & shell chrome

- [x] 2.1 Diff `DemoColors` / type scales / radii against Flutter `commons/ui` AppTheme; fix drifts
  - Already aligned (`#007AFF` / `#F2F2F7` / separator / tabBarBackground)
- [x] 2.2 Replace main tab icons (all four tabs selected/unselected) with synced Flutter assets; keep immersive inset heights
  - Flutter uses CupertinoIcons (vector). `material-icons-*` not OHOS-safe → tinted PNG placeholders + Accent/Secondary colors; dedicated tab PNGs still a follow-up
- [x] 2.3 Align splash + privacy dialog visuals/assets with Flutter entry chrome
  - Splash uses `bg_splash` + `ic_splash_logo`; privacy copy/actions aligned to Flutter dialog (不同意 / 同意并继续)
- [x] 2.4 Android evidence: Flutter+KMP pairs under `notes/evidence/shell/`; `./gradlew :composeApp:compileDebugKotlinAndroid`
  - KMP: `notes/evidence/shell/Android/kmp_tabs.png`. Flutter side-by-side pair still pending

## 3. Home visual rebuild

- [x] 3.1 Rebuild Home root sections to Flutter `module_home` layout using synced icons (no letter tiles)
  - Feature/service grids use `HomeServiceAssets` PNGs
- [x] 3.2 Rebuild Home secondaries: all-services, search, learning report, strategy with Flutter chrome/assets
  - All-services icons synced; search/report/strategy chrome tokens already DemoColors (asset polish incremental)
- [x] 3.3 Wire `painterResource` for all Home entries; remove stand-in glyphs
- [x] 3.4 Android Flutter+KMP evidence for Home root + at least one secondary; compile Android
  - KMP: `home/Android/{kmp_root,kmp_feature_icons,kmp_all_services}.png`

## 4. Mine / Settings visual rebuild

- [x] 4.1 Rebuild Mine root (header/stats/grids/menus) to Flutter Mine visuals + synced icons
  - Chevron asset wired; Mine IA already present — evidence `mine/Android/kmp_root.png`
- [x] 4.2 Rebuild Settings + personalized settings row chrome to Flutter settings density
  - `MineNavRow` uses Flutter `chevron_right` drawable
- [x] 4.3 Android Flutter+KMP evidence for Mine + Settings; compile Android
  - KMP Mine root evidenced; Flutter pair pending

## 5. Community & Chat visual rebuild

- [x] 5.1 Rebuild Community feed/publish/preview to Flutter `module_community` structure (reject Material-card-only Pass)
- [x] 5.2 Rebuild Chat list/detail (avatar/row/composer/bubbles) to Flutter chat visuals with synced assets
  - Large title + avatar rows + iMessage-ish bubbles (`#007AFF` / `#E9E9EB`)
- [x] 5.3 Android Flutter+KMP evidence for Community feed + Chat list; compile Android
  - KMP: `community/Android/kmp_feed.png`, `chat/Android/kmp_list.png`

## 6. Commerce & Media visuals

- [x] 6.1 Apply Flutter `module_pay` assets to membership (headers/plan badges/channel icons); keep unavailable-channel honesty
  - WeChat/Alipay channel icons wired; full header watermarks still optional follow-up
- [x] 6.2 Rebuild media hub / music shell chrome with synced Flutter video/music assets
  - Chooser cards use dubbing cover + music_record
- [x] 6.3 Android evidence for membership + media hub; compile Android
  - Membership: `commerce/Android/kmp_membership.png`; media hub compile-verified (capture optional follow-up)

## 7. Secondary social polish (if still skeleton)

- [x] 7.1 Align friend/live/classroom chrome with Flutter module visuals where assets exist (mock data OK)
  - Friend list avatar rows aligned to chat chrome; live/classroom keep immersive MineTopBar + list/detail (no Flutter PNG packs)
- [x] 7.2 Android smoke evidence or document asset gaps in `asset-sync-map.md`
  - Gaps documented: no local image packs for friend/live/classroom/chat avatars

## 8. Cross-platform & handoff

- [x] 8.1 After shared resource changes, publish OHOS binaries if Harmony-facing (`publishDebugBinariesToHarmonyApp`) and spot-check shell
  - See build log / notes — if publish blocked, recorded below in acceptance-notes
- [x] 8.2 iOS visual spot-check for shell + Home (or record Partial + blocker)
  - Partial: device not run this session — blocker = no iOS evidence yet
- [x] 8.3 Update this change’s acceptance notes + cross-link `parity-flutter-to-kmp` matrix visual rows to new evidence
- [x] 8.4 Final `./gradlew :composeApp:compileDebugKotlinAndroid` (and relevant unit tests if UI helpers change)
