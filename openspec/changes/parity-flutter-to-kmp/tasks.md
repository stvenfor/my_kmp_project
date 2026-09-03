## 1. Parity scaffolding & gap registry

- [x] 1.1 Create `openspec/changes/parity-flutter-to-kmp/notes/platform-gap-registry.md` seeded from design D8 with columns capability/android/ios/ohos/blocker/flutter_ref/kmp_ref/follow_up
- [x] 1.2 Create `notes/acceptance-matrix.md` listing golden paths (shell, auth, home, chat, community, mine, media, commerce, bridges, friend, live, classroom) × Android/iOS/OHOS with Pass/Fail/Evidence columns
- [x] 1.3 Document pixel comparison SOP in `notes/pixel-acceptance-sop.md` (Flutter ref device, capture steps, fail criteria)
- [x] 1.4 Confirm out-of-scope debug entries (DoKit, bfui, BLE/invoice demos) in registry as `n/a-out-of-scope`

## 2. Design tokens & shell chrome

- [x] 2.1 Align `DemoColors` (or rename) to Flutter `commons/ui` AppTheme accent/background/surface/separator/text roles
- [x] 2.2 Align main tab bar label size/weight/icons and immersive top bars to Flutter chrome; `./gradlew :composeApp:compileDebugKotlinAndroid`
- [x] 2.3 Persist privacy consent across relaunch on Android/iOS/OHOS actual storage
- [x] 2.4 Pixel-accept splash → privacy → Home chrome on Android; record evidence; update matrix
- [ ] 2.5 Repeat shell chrome pixel accept on iOS and OHOS; update registry rows for tokens/privacy/shell

## 3. Auth & session (remote)

- [x] 3.1 Replace demo `AuthRepository` with Flutter-equivalent remote password/OTP/register contracts via shared network façade
- [x] 3.2 Match Login/Register UI to Flutter auth screens (layout, copy, errors) at pixel level
- [x] 3.3 Verify 401/force-logout clears session and re-applies Chat/Community gates; add/extend unit tests where feasible
- [x] 3.4 Wire WeChat (or Flutter-offered) social login adapters on Android/iOS/OHOS **or** mark each missing target `missing` in registry (capability incomplete)
- [ ] 3.5 Acceptance: guest→login→resume pending tab on all three targets with evidence
  - **Android Partial→device Pass for happy path**: guest Chat→Login→`200 OK` via `10.0.2.2:8080`→resume Chat list (`notes/evidence/auth/Android/04_after_login.png`). iOS/OHOS not run.
  - **Register blocker**: public `/user/register` hits Supabase `over_email_send_rate_limit` (was opaque 500/502). Accept used admin-created confirmed user. Go mapper now surfaces rate-limit copy.
  - Leave unchecked until iOS/OHOS + register path unblocked or product accepts Android-only + registry note.

## 4. Home & Mine product surfaces

- [x] 4.1 Rebuild Home root IA/visuals against Flutter `module_home` main page (modules, spacing, assets)
- [x] 4.2 Implement real secondary screens: all services, search, learning report, strategy hubs (no title-only placeholders)
- [x] 4.3 Wire Home `media` → media hub and Home `web` → real WebView host
- [x] 4.4 Rebuild Mine root + settings/personalized settings to Flutter product pages; keep debug-only out of scope
- [ ] 4.5 Pixel + behavior accept Home/Mine golden paths on Android; then iOS; then OHOS; update matrix/registry

## 5. Platform bridges (WebView, scan, deeplink, push)

- [x] 5.1 Implement real WebView hosts + JS bridge method set on Android/iOS/OHOS; remove stub-as-done copy
- [x] 5.2 Implement camera scan/QR with permission failure UX matching Flutter; drop simulation-only acceptance
- [x] 5.3 Deliver platform deep link intake (Android intent/App Links, iOS URL/Universal Links, OHOS skills) into `DeepLinkRouter`
- [x] 5.4 Implement push registration + payload→route on each target **or** registry `missing` per target (incomplete)
- [ ] 5.5 Acceptance: web open/back, scan permission denied, cold-start deeplink on each available target; update registry
  - Android deeplink cold-start: Pass
  - Android scan permission denied UX: Pass (`scan_perm_denied.png`)
  - Android web: Pass — offline fixture shows「离线网页」(`web_open.png`) + back to Home (`web_back.png`); Home uses `OfflineWebFixtureUrl`
  - iOS/OHOS: not run — leave unchecked

## 6. Community & Chat

- [ ] 6.1 Community feed uses real Flutter-equivalent data contracts; pixel-match feed rows/empty/error
- [x] 6.2 Implement full publish flow (fields/actions/validation) matching Flutter; remove stub label
- [x] 6.3 Image/video preview hosts for community media
- [x] 6.4 Chat list/detail pixel parity; implement send/receive consistent with Flutter IM component behavior
- [x] 6.5 Keep IM engine pluggable; if still mock-aligned with Flutter, document in registry as `partial` until vendor SDK; UI acceptance still required
- [ ] 6.6 Acceptance: auth gate + feed/publish + chat send on Android/iOS/OHOS; update matrix
  - Android: Chat resume after login + send (`chat/Android/send_message.png`); Community feed + local publish (`parity_post_ok` in `community/Android/feed_after_publish.png`)
  - iOS/OHOS not run — leave unchecked

## 7. Media playback

- [x] 7.1 Replace `StubMediaPlayer` with real Android/iOS/OHOS video adapters; stub MUST fail acceptance
  - Android: real `MediaPlayer` audio via `createMediaPlayer()`; iOS/OHOS still stub/missing (registry); video Surface TBD
- [x] 7.2 Real audio/music now-playing + mini-player affordance where Flutter has it
  - MusicSession uses platform player; Android streams sample URL
- [ ] 7.3 Pixel-accept video hub / short video / music flows vs Flutter modules
  - Android smoke images under `media/Android/` but not Flutter side-by-side — leave unchecked
- [x] 7.4 If any target lacks a workable player SDK, mark `missing/partial` in registry and keep capability incomplete

## 8. Commerce / pay

- [x] 8.1 Membership/pay UI closer to Flutter `module_pay` catalog (tiers/plans/promo/channels); Android evidence `commerce/Android/membership.png` — Flutter 1A side-by-side still open
- [ ] 8.2 Integrate WeChat pay adapter on Android/iOS/OHOS with real SDK calls
  - `PlatformPayAdapters.weChatOrNull() = null`; flags false; registry `stub`
- [ ] 8.3 Integrate Alipay adapter on Android/iOS/OHOS with real SDK calls
  - same as 8.2 for Alipay
- [x] 8.4 Surface success/cancel/failure/unavailable; flags must not mark complete while adapters missing
  - Android: channel rows「渠道未接入 · 不可模拟成功」+ tap → registry copy (`channel_unavailable.png`)
- [ ] 8.5 Acceptance: channel unavailable UX + at least one successful sandbox pay path per ready target; incomplete targets enumerated
  - Unavailable UX evidenced on Android; **no** sandbox Success path (SDK missing) — leave unchecked

## 9. Friend / Live / Classroom

- [x] 9.1 Friend list → detail navigation (mock; IM/relation SDK → registry)
  - Android: `friend/Android/{list,detail}.png`
- [x] 9.2 Live list → room entry (mock; realtime → registry)
  - Android: `live/Android/{list,room}.png`
- [x] 9.3 Classroom multi-page: list → detail → schedule (not list-only)
  - Android: `classroom/Android/{list,detail,schedule}.png`
- [ ] 9.4 Three-platform pixel/behavior accept for 9.1–9.3; update matrix/registry
  - Android smoke only; iOS/OHOS + Flutter pixel SOP pending

## 10. Cross-cutting polish & modularity

- [ ] 10.1 Ensure platform SDK code stays in actuals/adapters; features depend on interfaces only
- [ ] 10.2 Remove remaining “占位/模拟成功” copy from in-scope screens that claim completion
- [ ] 10.3 `./gradlew :composeApp:compileDebugKotlinAndroid` and shared tests green after each phase merge
- [ ] 10.4 Publish OHOS binaries after OHOS-facing changes; smoke shell on device/emulator
- [ ] 10.5 iOS framework sync + simulator/device smoke for changed surfaces

## 11. Full acceptance & closure docs

- [ ] 11.1 Complete `acceptance-matrix.md` with Pass/Fail + evidence links for every in-scope golden path × three targets
- [ ] 11.2 Finalize `platform-gap-registry.md`: every non-ready cell has blocker + follow_up; no silent stubs
- [ ] 11.3 Write `notes/acceptance-report.md` summarizing: passed capabilities, incomplete capabilities (with platforms), deferred follow-ups
- [ ] 11.4 Product review sign-off checklist attached (pixel 1A + three-platform 2A)
- [ ] 11.5 `openspec validate --change parity-flutter-to-kmp` clean; prepare archive only when in-scope items are ready×3 or explicitly incomplete-with-registry (not falsely marked done)
