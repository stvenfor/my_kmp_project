# Spec: Full Parity Phase (Flutter → KMP)

**Status:** ready-for-agent (grill 2026-09-27; seams confirmed)  
**Glossary:** `CONTEXT.md` · **ADRs:** 0002 (ownership), 0003 (Full Parity Phase)  
**Tracker:** publish as GitHub issue with label `ready-for-agent`

## Problem Statement

I need the Flutter product `my_ai_project` fully migrated into this KMP repo with clear ownership: iOS SwiftUI, Harmony ArkUI, Android Jetpack for shell/tab roots, and Mine secondary pages only on the shared Mine Compose Island. Some surfaces already match; many still diverge, sit on Legacy SecondaryRouteIsland, or lack acceptance evidence. Without a single acceptance seam and inventory, work stalls as endless “继续” without a done definition.

## Solution

Enter **Full Parity Phase**: build a Flutter-route **Parity Inventory**, cut work into **Parity Domain Tickets**, deliver each via **Native-First Cutover** (non-Mine) or Mine island completion, and accept only through the **Route Acceptance Packet** (three-platform open path, Android **Screenshot Diff Gate** ≤ UI Parity Bar, Flutter-equivalent behavior/mock, gap registry update). **Platform Capability Gaps** (real SDKs) may remain mock-parity when Flutter is mock; they must stay registered and must not fake ready.

## User Stories

1. As a product owner, I want every in-scope Flutter route listed in a Parity Inventory, so that remaining work is visible and not guessed from chat history.
2. As a product owner, I want n/a-out-of-scope demos (DoKit/BLE/bfui debug, face verify, etc.) excluded from the inventory, so that agents do not burn cycles on non-goals.
3. As a product owner, I want true vendor SDK gaps listed separately from page sync, so that mock-parity pages can still pass acceptance.
4. As an end user on Android, I want splash → privacy → four tabs to match Flutter SoT within the UI Parity Bar, so that the shell feels identical.
5. As an end user on iOS, I want the same shell and tab roots in SwiftUI, so that navigation matches Android/Flutter intent.
6. As an end user on Harmony, I want the same shell and tab roots in ArkUI, so that the product is usable on OHOS.
7. As an end user, I want Mine root to be native on each platform, so that the tab feels platform-native.
8. As an end user, I want Mine secondary pages (settings, profile, addresses, mall under Mine entry, membership hosted as island, etc.) on the Mine Compose Island, so that those flows stay one shared UI.
9. As an end user, I want Auth UI (login, register, soft gates) to be native, so that gated tabs do not depend on the island.
10. As an end user opening a non-Mine secondary from Home, I want the main path to be Native Shell UI (not SecondaryRouteIsland), so that ownership matches ADR 0002/0003.
11. As a developer, I want SecondaryRouteIsland treated as Legacy Shared Compose, so that cutover has a deletion target.
12. As a developer, I want Native-First Cutover (wire native main path, pass acceptance, then delete island route), so that we do not polish CMP twice.
13. As a QA engineer, I want a Route Acceptance Packet per route, so that “done” is evidence-based.
14. As a QA engineer, I want Android Screenshot Diff Gate (≤2% vs Flutter Android SoT) as the hard pixel gate, so that parity is measurable.
15. As a QA engineer, I want iOS/Harmony open-path plus sampling (not the same hard gate), so that cost stays manageable.
16. As a QA engineer, I want Soft Auth behavior included in the open-path check, so that gated routes are not falsely marked open.
17. As a developer, I want Shared Presentation Logic aligned with Flutter (same mock/API level), so that UI is not a hollow skin.
18. As a developer, I want platform-gap-registry updated when a ticket finishes, so that SDK honesty survives the sprint.
19. As a product owner, I want Parity Domain Tickets (~2–4 related routes), so that AFK agents can finish a ticket in one session.
20. As a product owner, I want Mine-island track and native-cutover track in parallel against one inventory, so that work is not falsely serialized.
21. As a developer, I want inventory draft in-repo and GitHub child issues synced, so that agents and humans share one list.
22. As an end user, I want Home services (search, all services, learning report, check-in mall, strategy, hot rank, used car, ledger/收支, data analytics, life service, club/live commerce, after-sales, new-car follow, todos, dubbing feed) to open and match Flutter SoT under the acceptance packet.
23. As an end user, I want Chat list/detail mock-parity with Flutter MockIm, so that messaging UX works without claiming RongCloud ready.
24. As an end user, I want Community feed/publish/search/convention mock-parity, so that social tab matches Flutter mock depth.
25. As an end user, I want Friend directory mock-parity, so that contacts work without vendor IM SDK.
26. As an end user, I want Live list/room mock-parity, so that live entry is not a dead stub.
27. As an end user, I want Wallet recharge/bind/ledger UI mock-parity, so that wallet matches Flutter wallet page.
28. As an end user, I want Membership renew UI on the island with renew bar/agreement/pay channels (sandbox/unavailable honest), so that pay UX matches Flutter without fake Success.
29. As an end user, I want Mall list/detail/orders flows reachable and SoT-aligned, so that commerce demos work.
30. As an end user, I want Deal invoice demo/upload under Mine/settings ownership rules, so that 新车成交 matches Flutter.
31. As an end user, I want Classroom/homework/gift/video mock-parity, so that classroom feature is not Android-only.
32. As an end user, I want Short video list/play/publish/help mock-parity (player gaps registered), so that video hub matches Flutter chrome.
33. As an end user, I want Music list/now playing mock-parity (OHOS player gap registered), so that audio chrome matches.
34. As an end user, I want AI 小石头 stream chrome with mock chunking (SSE gap registered), so that AI entry matches Flutter.
35. As an end user, I want Web offline fixture path working where platforms support WebView, so that web bridge demos stay honest on gaps.
36. As an end user, I want Scan/camera paths with deny UX on supported platforms and OHOS gap registered, so that scan never silently lies.
37. As a developer, I want shared design tokens consumed by Jetpack, SwiftUI, ArkTS, and the island, so that colors/spacing do not drift per stack.
38. As a developer, I want legacy commonMain product Compose removable after cutover, so that dual main paths end.
39. As a release manager, I want CI to keep building Android unit tests while screenshot gate evidence is attached per ticket (local/emulator), so that merge health and pixel evidence both exist.
40. As an agent implementing a domain ticket, I want blocking edges declared (inventory row + gap updates + cutover delete), so that tickets can be grabbed when unblocked.
41. As a product owner, I want “验收成功” to mean inventory rows closed under Route Acceptance Packet, not “chat said done”.
42. As a developer, I want Flutter toast-only Mine entries (e.g. some fan/invite cases) preserved as toast-only when Flutter is toast-only, so that we do not invent full pages Flutter never shipped.
43. As a developer, I want deep links to resolve to the same ownership rules as in-app navigation, so that cold start does not reopen Legacy island incorrectly.
44. As a QA engineer, I want evidence paths (screenshots, mse/diff notes) stored under the change notes/evidence convention, so that Reality Checker / Evidence Collector can audit.
45. As a product owner, after Full Parity Phase, I want a short closeout that SecondaryRouteIsland no longer hosts in-scope non-Mine routes, so that ADR 0003 is enforceable in code.

## Implementation Decisions

1. **Scope driver:** ADR 0003 Full Parity Phase; ADR 0002 ownership remains except §6 deferral cap.
2. **Inventory SoT:** Flutter `RoutePath` (+ tab-reachable labels) minus n/a-out-of-scope and true Platform Capability Gap SDK-only rows; draft at `openspec/changes/parity-flutter-to-kmp/notes/parity-inventory.md`; sync to GitHub epic/children.
3. **UI stacks:** Android Jetpack shell/roots; iOS SwiftUI; Harmony ArkTS; Mine secondary+children = Mine Compose Island only.
4. **Legacy:** SecondaryRouteIsland + leftover non-island commonMain product UI = Legacy Shared Compose; Native-First Cutover then delete.
5. **Auth:** Native Soft Auth / login-register; not island.
6. **Domain tickets:** ~2–4 routes per Parity Domain Ticket; parallel tracks (native cutover vs Mine island).
7. **Logic:** Shared Presentation Logic / bridges at Flutter’s mock-or-real level; FlaggedPayGateway stays honest (no fake Success).
8. **Gaps:** Maintain `platform-gap-registry.md` (or successor); mock-parity allowed when Flutter is mock.
9. **Tokens:** Shared token source for all four UI stacks.
10. **Hosting:** Mine island container push/pop rules unchanged (ADR 0002).
11. **Deeplink:** Resolve to native or island per ownership; must not permanently re-home non-Mine onto SecondaryRouteIsland after cutover.
12. **Evidence:** Per-route Android SoT + KMP shots + diff metric under change notes/evidence; ticket comments link evidence.

## Testing Decisions

1. **Good tests** assert external behavior of the Route Acceptance Packet, not private Composable structure.
2. **Primary seam (sole acceptance seam):** Route Acceptance Packet — (a) open path on Android/iOS/Harmony including Soft Auth, (b) Android Screenshot Diff Gate ≤ UI Parity Bar vs Flutter Android SoT, (c) main interactions Flutter-equivalent (mock allowed), (d) gap registry updated.
3. **Secondary seams (non-substitutes):** existing `commonTest` Soft Auth / session tests; Android assemble + unit test CI.
4. **Prior art:** `.scratch/phase-1-native-shell-mine-island/screenshots`, `notes/evidence/**`, `notes/pixel-acceptance-sop.md`, `scripts/parity_task_audit.py`.
5. **Not required as hard gate:** iOS/OHOS same-device MSE; pixel-perfect vendor SDK integration.

## Out of Scope

- Integrating real RongCloud, WeChat login/pay, JPush/FCM, OHOS camera/MediaKit, or AI SSE as a precondition of page acceptance (tracked as Platform Capability Gap).
- bfui showcase / bluetooth / face verify / DoKit-style demos marked n/a-out-of-scope.
- Replacing Mine Compose Island with native Mine secondaries.
- Making SecondaryRouteIsland the long-term host for non-Mine pages.
- Requiring automated screenshot CI on hosted GitHub runners for iOS/OHOS in this phase.
- Rewriting Flutter SoT itself.

## Further Notes

- Grill decisions live in `CONTEXT.md` and ADR 0003; use glossary terms in tickets.
- Next skill: `/to-tickets` to split Parity Inventory into GitHub children with blocking edges.
- Phase-1 is baseline complete for shell/roots/island start — not a ceiling.
- When Flutter is toast-only for an entry, KMP must remain toast-only (do not invent pages).
