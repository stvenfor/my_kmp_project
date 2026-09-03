## Context

See `proposal.md` for motivation. Source of truth for product behavior: `/Users/mac/Desktop/github/my_ai_project` (Flutter modular monolith: `lib` + `features/*` + `commons/*` + `components/*`, GetX, four tabs). Target baseline: this repo’s single `:composeApp` Demo shell (Home/Mine), with existing `NetworkFacade` / `AccountFacade`, immersive helpers, and Android/iOS/OHOS hosts. Specs under `specs/*` define required behaviors.

Constraints: common-first Compose Multiplatform; OHOS HTTP remains non-Ktor; no FanGroup branding; hosts stay thin.

## Goals / Non-Goals

**Goals:**
- Modular Gradle graph mirroring Flutter’s layering (`app → features → cores`).
- One shared navigation + shell that matches Flutter IA on three targets.
- Phased capability delivery without rewriting module boundaries each phase.
- Clear platform adapter seams for WebView, pay, push, media, scan.

**Non-Goals:**
- Bit-for-bit Flutter widget parity or GetX API compatibility.
- Shipping DoKit / `module_bfui` showcase in the first architecture cut.
- Binding a specific commercial IM/face SDK in phase 1.

## Decisions

### 1) Gradle modules vs source-set packages
**Decision:** Introduce Gradle modules: `:core:network`, `:core:account`, `:core:design`, `:core:router`, `:core:platform`, plus `:feature:*` modules; `:composeApp` becomes the app assembler + platform entrypoints.  
**Why:** Enforces dependency direction at compile time (matches Flutter path-package discipline).  
**Alt:** Keep everything in `composeApp` packages — faster initially, weakens modularity requirement.

### 2) Navigation
**Decision:** Shared type-safe navigation (Compose Navigation multiplatform or equivalent) owned by `:core:router`, with feature-owned destination graphs registered at assemble time (ModuleRegistry pattern).  
**Why:** Replaces GetX `ModuleRegistry` + `RoutePath` without putting routes in hosts.  
**Alt:** Single mega-NavHost in app module — simpler early, fights feature isolation.

### 3) Network / account
**Decision:** Keep façade pattern; extract implementations into `:core:network` / `:core:account`; preserve `networkKtorMain` + OHOS actual split. Map Flutter Dio interceptors (token, force-logout) onto shared handlers.  
**Why:** Already proven on three targets; minimizes OHOS risk.  
**Alt:** Force Ktor on OHOS — blocked by current fork constraints.

### 4) State management
**Decision:** Prefer explicit ViewModel / `StateFlow` + Compose state (no GetX).  
**Why:** Idiomatic CMP; testable; avoids third-party DI lock-in.  
**Alt:** Port GetX-like patterns — higher cost, little benefit.

### 5) IM / pay / push / WebView
**Decision:** Interface-first adapters (`ImEngine`, `PayGateway`, `PushBridge`, `WebBridgeHost`) with mock defaults; real SDKs plugged per target behind expect/actual or android/ios/ohos source sets.  
**Why:** Unblocks UI migration before native SDK wiring; keeps three-platform UI identical.  
**Alt:** Block features until every SDK is ready — delays shell parity.

### 6) Three-platform consistency gate
**Decision:** Each feature slice must pass a “parity checklist”: same routes, same auth gates, same primary actions on A/iOS/OHOS before marking the slice done. Screenshots or scripted smoke optional but recommended for shell and auth.  
**Why:** Makes “三端一致” enforceable in tasks, not aspirational.

### 7) Migration sequencing
**Decision:** Phase A shell+modularity+auth → Phase B home+settings → Phase C community+chat(mock) → Phase D media → Phase E commerce+bridges/SDK → Phase F classroom/live/friend/music leftovers & polish.  
**Why:** Matches inventory risk order; early vertical slices prove architecture.

## Risks / Trade-offs

- **[Risk] Scope explosion from “完整迁移”** → Mitigation: phased tasks; DoKit/bfui explicitly deferred; each phase has exit criteria.
- **[Risk] OHOS media/WebView/pay gaps** → Mitigation: adapters + feature flags; ship mock/disabled channel UI rather than crash.
- **[Risk] Dual Flutter stacks (`module_*` vs `wys_*`)** → Mitigation: consolidate to one KMP core; prefer `module_*` product behavior, reuse `wys_*` only where it is the active runtime path.
- **[Risk] Large Gradle graph slows builds** → Mitigation: start with fewer modules (core + critical features), split further when compile boundaries hurt.
- **[Trade-off] Not pixel-perfect Flutter UI** → Accept Material3/CMP redesign while preserving IA and flows.

## Migration Plan

1. Land modular skeleton + shell/auth parity (Demo tabs replaced).
2. Migrate features by phase; keep Flutter project as reference only (no runtime hybrid).
3. Per phase: `compile` on Android + targeted iOS/OHOS publish smoke; update OpenSpec archive when a capability is done.
4. Rollback: feature flags / unfinished destinations stay behind stubs; modular boundaries allow reverting a feature module without ripping shell.

## Open Questions

- Final applicationId / bundleName: keep `com.example.my_kmp_project` vs adopt `com.sample.module_sample` (does not change specs; decide at branding/release task).
- Whether classroom/live ship in Phase F of this change or a follow-up change once media adapters stabilize.
