# Phase 1: Native Shell + Mine Compose Island

Status: ready-for-agent

ADR: `docs/adr/0002-native-shell-mine-compose-island.md`  
Glossary: `CONTEXT.md`  
Visual SoT: Flutter repo `my_ai_project` (screenshots + DESIGN tokens)

## Problem Statement

The product must match Flutter `my_ai_project` in business behavior and UI fidelity (≤2% error), on Android, iOS, and HarmonyOS. The previous KMP default—almost all product UI in shared Compose Multiplatform—conflicts with the desired ownership model: **native UI per platform**, **shared logic only**, except a **single shared Compose island** for Mine secondary pages. Without a clear Phase-1 cut, teams cannot ship a testable main path or stop maintaining two competing UI stacks as “source of truth.”

## Solution

Ship Phase-1 Main Path with:

- **Native Shell UI** for splash, privacy consent, tab shell, Home / Chat / Community roots, Mine Root, and Auth UI.
- **Mine Compose Island** for Mine secondary pages and their children only, hosted by a native Compose container (Mine Island Hosting).
- **Shared Presentation Logic** (UseCase + UiState/ViewModel) and **Shared Business Logic** behind all of the above.
- **Shared Design Tokens** consumed by Android Jetpack Compose, SwiftUI, ArkTS, and the island.
- **Deferred Destination Stub** for Phase-1-out-of-scope destinations still visible on roots.
- **Legacy Shared Compose** kept temporarily as reference only; main path does not depend on it; remove after parity.

Acceptance of pixels is against Visual Source of Truth with UI Parity Bar (≤2% error).

## Proposed test seams (ratify if needed)

Prefer few, high seams:

1. **Shared Presentation Logic** (primary behavioral seam) — assert UiState transitions for session, tab roots’ data, Mine island pages, auth gate outcomes; no pixel assertions here.
2. **Mine Island Hosting** — enter island from Mine Root via native container; back stack: pop island routes, then dismiss container to Mine Root.
3. **Auth UI boundary** — unauthenticated Chat/Community shows native gate; login/register never route into the island.
4. **Deferred Destination Stub** — out-of-phase entries remain visible; tap yields native stub / safe no-op, not island, not full feature.
5. **UI Parity Bar** (acceptance seam, not unit seam) — side-by-side vs Flutter SoT for Phase-1 surfaces; token values match Shared Design Tokens.

Ideal long-term: one behavioral seam (Shared Presentation Logic) + one hosting contract (Mine Island Hosting) + human/assisted visual acceptance.

## User Stories

1. As an end user, I want the app to open with a splash that matches Flutter, so that cold start feels like the same product.
2. As an end user, I want to see and accept/reject privacy consent before the main shell, so that compliance matches Flutter.
3. As an end user, I want a four-tab main shell (Home, Chat, Community, Mine), so that primary navigation matches Flutter.
4. As an end user, I want the Home tab root to match Flutter layout and primary entries within ≤2% error, so that the landing experience is familiar.
5. As an end user, I want the Chat tab root to match Flutter when logged in, so that messaging entry looks correct.
6. As an end user, I want the Community tab root to match Flutter when logged in, so that the feed entry looks correct.
7. As an end user, I want the Mine Root to match Flutter as a **native** screen, so that the profile hub looks correct without shared Compose.
8. As an end user, I want tapping a Mine secondary entry to open the shared Mine Compose Island, so that settings-like flows are consistent across platforms.
9. As an end user, I want nested pages inside Mine secondary flows to stay inside the island, so that deep settings demos keep one UI implementation.
10. As an end user, I want system back / gesture to leave island pages one by one, then return to Mine Root, so that navigation feels native and predictable.
11. As an end user who is logged out, I want Chat to show a native auth gate, so that I am prompted to log in without entering the Mine island.
12. As an end user who is logged out, I want Community to show a native auth gate, so that soft-auth matches product rules.
13. As an end user, I want login and register screens to be native on each platform, so that auth is owned by the shell, not the island.
14. As an end user, I want successful login to restore the intended tab content, so that auth does not strand me on a blank gate.
15. As an end user, I want Home root entries that target deferred features to still appear like Flutter, so that the root layout stays aligned.
16. As an end user, I want tapping a deferred Home/Mine entry to open a clear native placeholder (or safe no-op), so that I am not dropped into a broken flow.
17. As an end user, I want Mine island pages (settings, personalized settings, demos listed under settings module secondary routes) to match Flutter within ≤2% error, so that account/settings quality is high.
18. As an Android user, I want shell and tab roots in Jetpack Compose, so that Android UI uses the platform Compose stack.
19. As an Android user, I want Mine secondary pages in shared CMP, so that the island matches iOS/Harmony.
20. As an iOS user, I want shell, roots, and Mine Root in SwiftUI, so that the app feels native outside the island.
21. As an iOS user, I want Mine secondary flows embedded in a Compose container, so that the island works inside SwiftUI navigation.
22. As a Harmony user, I want shell, roots, and Mine Root in ArkTS, so that the app feels native outside the island.
23. As a Harmony user, I want Mine secondary flows in a Compose container hosted by the ArkTS shell, so that the island is reachable.
24. As a developer, I want Shared Presentation Logic to drive all Phase-1 screens, so that loading/error/session behavior does not diverge by platform.
25. As a developer, I want Shared Design Tokens as the only color/type/spacing source for Phase-1 UI stacks, so that parity failures are attributable to layout, not drifted palettes.
26. As a developer, I want Legacy Shared Compose retained only as reference during Phase 1, so that I can compare while building native roots without shipping two main paths.
27. As a developer, I want the main path to stop depending on Legacy Shared Compose for shell/roots, so that the new ownership model is the runtime truth.
28. As a developer, I want a clear delete milestone for Legacy Shared Compose after Phase-1 parity, so that the codebase does not keep dual SoT forever.
29. As a QA reviewer, I want Phase-1 acceptance checklists keyed to Flutter screenshots for splash, privacy, four roots, auth, and Mine island pages, so that UI Parity Bar is enforceable.
30. As a QA reviewer, I want to verify back-stack behavior into and out of the island on all three platforms, so that Mine Island Hosting does not regress.
31. As a product owner, I want live/classroom/pay/video deep features out of Phase 1, so that the team can finish the shell+island cut without boiling the ocean.
32. As a product owner, I want later tickets to add deferred features without rewriting shell ownership, so that Phase 1 is a stable foundation.
33. As an end user, I want session restore on cold start to match Flutter rules (privacy first; not forced login), so that returning users are not surprised.
34. As an end user, I want tab switching to preserve each tab’s root state reasonably, so that navigation matches expected mobile shell behavior.
35. As an end user, I want visual density, typography, and colors on native Mine Root and island pages to share the same tokens, so that moving from Mine Root into the island does not feel like a different app.
36. As a developer, I want Auth UI never registered as an island route, so that soft-auth entry points stay consistent.
37. As a developer, I want cross-feature opens from Mine (e.g. short video, used car) treated as deferred native destinations in Phase 1, so that they do not expand the island boundary.
38. As an end user, I want error and empty states on Phase-1 roots driven by shared UiState, so that copy/behavior align across platforms even when pixels are native.
39. As a release manager, I want Phase-1 “done” defined as main path + parity bar on listed surfaces, so that scope creep into full Flutter feature set is visible and rejected.
40. As a future implementer, I want this spec and ADR 0002 to supersede “all product UI in commonMain Compose” assumptions for new work, so that OpenSpec/parity tracks can be reoriented without ambiguity.

## Implementation Decisions

- Honor ADR 0002 and CONTEXT glossary terms for all naming in tickets and code reviews.
- Reorient (do not silently continue) prior “full shared CMP product UI” parity efforts to this ownership model for new Phase-1 work.
- **Modules (logical):**
  - Shared core: networking, account/session, Shared Presentation Logic for Phase-1 surfaces.
  - Shared design tokens module/source consumed by four UI stacks.
  - Shared Mine Compose Island module (CMP UI + its navigation inside the container only).
  - Per-platform app shells: Android (Jetpack), iOS (SwiftUI), Harmony (ArkTS), each owning Native Shell UI and Auth UI, each providing Mine Island Hosting container.
- **Interfaces:**
  - Shell depends on shared UiState/ViewModel APIs; must not embed Flutter or Legacy Shared Compose as the runtime main path.
  - Mine Root exposes navigation intents to “open island route X” and “open deferred stub Y”.
  - Island exposes only Mine-secondary route graph; rejects auth routes.
  - Token API is read-only constants (or generated bindings) — platforms do not fork palettes.
- **Navigation:** Native owns tab + auth + container push/pop; island owns only in-container stack (Mine Island Hosting).
- **Android UI Split:** Jetpack for shell/roots; CMP only for island content inside the container.
- **Phase-1 Mine island page set** (from Flutter settings secondary routes; exclude Mine Root): personalized settings, HTTP test, settings, dialog/linking/realtime/IM/bluetooth demos, deal-invoice demo and upload. Adjust only if Flutter SoT route list changes.
- **Deferred features:** live, classroom, pay, video deep flows, music, friend, bfui templates, etc. — stubs only when linked from Phase-1 roots.
- **Legacy Shared Compose:** keep in tree for reference; wire main path off it; schedule removal after Phase-1 visual acceptance.
- **Parity process:** capture Flutter screenshots for Phase-1 surfaces; review with UI Parity Bar; tokens must match Shared Design Tokens before debating layout nits.

## Testing Decisions

- Good tests assert **external behavior** at Shared Presentation Logic and hosting boundaries — not Compose/SwiftUI private widgets.
- **Unit/integration (Shared Presentation Logic):** session restore, soft-auth gate state, tab-root UiState loading/error/empty, island page UiState for settings flows, deferred-stub intent emission (not full feature).
- **Hosting tests / manual scripts (Mine Island Hosting):** push container → multi-level island navigate → back to Mine Root on all three platforms.
- **Auth boundary:** gate appears when logged out on Chat/Community; completing native login clears gate; no island route involved.
- **Visual acceptance (UI Parity Bar):** checklist vs Flutter SoT for splash, privacy, four tab roots, auth screens, and each Mine island page in Phase-1 set; ≤2% error; no requirement for automated screenshot CI in Phase 1.
- **Prior art:** prefer existing shared/commonTest patterns for ViewModel/session; platform UI tests only where hosting cannot be covered from shared logic.
- **Seam count:** do not add new low-level seams per platform widget; extend Shared Presentation Logic and hosting contract instead.

## Out of Scope

- Full Flutter feature parity beyond Phase-1 Main Path (live rooms, classroom homework, pay/membership deep flows, short-video player productization, IM full client beyond what Chat root needs, bluetooth/realtime as production features beyond Mine demos already in the island list, bfui demos as product, macOS).
- Replacing Shared Presentation Logic with per-platform-only ViewModels.
- Moving Auth UI into Mine Compose Island.
- Making Mine Root shared CMP.
- Automated screenshot-diff CI as a Phase-1 gate.
- Immediate deletion of Legacy Shared Compose on day one.
- Changing Visual Source of Truth away from Flutter `my_ai_project`.

## Further Notes

- Next process step per ask-matt: `/to-tickets` to split this spec into tracer-bullet issues with blocking edges under `.scratch/phase-1-native-shell-mine-island/issues/`.
- Existing OpenSpec changes aimed at full shared-CMP Flutter parity should be marked superseded or rewritten against ADR 0002 before further apply work.
- Flutter settings secondary route list is the island inventory baseline; if SoT adds/removes pages, update island scope in a follow-up ticket rather than silently expanding Native Shell UI.
