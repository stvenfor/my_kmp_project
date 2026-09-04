# ADR 0001 — Production architecture foundation

- **Status:** Accepted (grill 2026-09-04)
- **Date:** 2026-09-04
- **Reference diagram (Flutter):** `/Users/mac/Desktop/work/tpj-flt/docs/architecture/tpj-flt-architecture.excalidraw`
- **This repo diagram:** `docs/architecture/my-kmp-architecture.excalidraw`
- **OpenSpec:** `openspec/changes/architect-production-foundation/`

## Context

KMP demo grew into four-tab product surface with Flutter parity pressure, but stayed a monomodule with local `when` navigation, dual session tracks (`AccountFacade` + `AuthSessionState`), and no vertical `component` layer. OHOS requires a single `libkn.so`.

## Decision

1. **Goal:** Production app base; OHOS first-class; team parallelization.
2. **Layers (semantic align tpj-flt):** `app` / `feature` / `component` / `core` / platform shells.
3. **Modules:** Multi-module on Android/iOS; OHOS fat aggregate → one `libkn.so`. Start with empty shells + dependency guards, then extract `:core:network` / `:core:account`.
4. **Navigation:** Sealed `AppRoute` + `Navigator` first; optional Navigation Compose later if OHOS artifacts exist.
5. **DI:** `AppContainer` with subcomponents → migrate to Koin; fat container is a bridge only.
6. **Session:** `AuthSessionState` Flow is SoT; AccountFacade demoted to repository.
7. **Components first set:** Push, WebView, Pay, Chat/IM port (tpj-flt-aligned).
8. **Must gate:** Shell, auth+restore, deep link, Home/Mine main paths, Chat/Community soft-auth, WebView, scan, real media, sandbox real pay, push token + click deep link. Missing Must = Fail.
9. **Execution:** Spike I (2 FTE × 2 weeks bet): Person1 nav/auth/container; Person2 modules/components/OHOS aggregate. Spike II: pay+push+extract cores. Flutter parity frozen until Spike II ends.
10. **Tokens:** Single DesignSystem target; Mine/Membership may alias until after Spike II start.

## Consequences

- Positive: Clear ownership, testable session, navigable deep links, OHOS honesty via Must matrix.
- Negative: Large Spike I scope; requires two people; risk of thrash if staffing slips (fallback: milestone mode, no 2-week claim).
- Follow-ups: Detekt/Gradle import guards; must-matrix.md; Koin modules; DesignSystem migration.
