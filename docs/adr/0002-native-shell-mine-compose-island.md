# Native shell + Mine Compose island

- **Status:** Accepted (grill 2026-09-20)
- **Date:** 2026-09-20
- **Glossary:** `CONTEXT.md`

## Context

Product sync from Flutter `my_ai_project` required a hard choice between shared Compose Multiplatform product UI (prior KMP default) and per-platform native UI with shared logic only. A full native rewrite of every screen is the long-term shape; one shared Compose surface remains useful for Mine secondary flows.

## Decision

1. **Default UI ownership:** Native per platform — iOS SwiftUI, Android Jetpack Compose, Harmony ArkTS. KMP shares business logic plus observable UiState/ViewModel, not pixels (except the island below).
2. **Mine Compose Island only:** Shared CMP UI is limited to Mine **secondary pages and their children**. Mine **root** stays native.
3. **Auth UI** (login/register/soft gates) is native, outside the island.
4. **Hosting:** Native shell pushes a Compose container to enter the island; back pops island routes first, then dismisses the container to Mine root.
5. **Visual SoT:** Flutter `my_ai_project` (screenshots + design tokens). Parity bar: ≤2% error vs SoT.
6. **Phase 1:** Splash → privacy → tab shell + full Home/Chat/Community roots + native Mine root + Mine island; other features deferred with visible stubs on roots.
7. **Tokens:** One shared token source consumed by all four UI stacks.
8. **Legacy commonMain product Compose:** Keep temporarily as reference; main path does not depend on it; remove after native+island parity.

## Consequences

- Supersedes the assumption that all product UI lives in `commonMain` Compose (see also ADR 0001 / prior OpenSpec CMP parity tracks — those must be reoriented to this ownership model).
- Android carries two Compose stacks (Jetpack for shell/roots, CMP for the island).
- iOS/Harmony embed CMP only inside the Mine container, not as the app shell.
