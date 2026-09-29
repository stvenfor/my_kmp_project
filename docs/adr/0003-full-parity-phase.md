# Full parity phase supersedes Phase-1 deferral

- **Status:** Accepted (grill 2026-09-27)
- **Date:** 2026-09-27
- **Glossary:** `CONTEXT.md`（Full Parity Phase, Platform Capability Gap, Parity Inventory, Screenshot Diff Gate, Route Acceptance Packet, Native-First Cutover, Parity Domain Ticket）
- **Supersedes:** ADR 0002 §6（“other features deferred”）only — ownership in §1–5, 7–8 remains.

## Context

Phase-1 delivered shell, tab roots, and the Mine Compose Island baseline while deferring other features. Product now requires Flutter `my_ai_project` full-page migration with an explicit acceptance bar. Meanwhile iOS/Harmony still host many non-Mine secondaries on Legacy `SecondaryRouteIsland`, which contradicts ADR 0002 ownership.

## Decision

1. Enter **Full Parity Phase**: every in-scope Flutter product route enters the **Parity Inventory** and must pass a **Route Acceptance Packet**.
2. Keep ADR 0002 UI ownership: **Native Shell UI** everywhere except **Mine Compose Island** (Mine secondary + children only).
3. Burn down Legacy `SecondaryRouteIsland` / leftover commonMain product UI via **Native-First Cutover** (native main path first, then delete island routes).
4. **Platform Capability Gaps** (vendor SDKs, SSE, etc.) may ship Flutter-equivalent mock for page acceptance when Flutter itself is mock; register gaps honestly — never fake `ready`.
5. **Screenshot Diff Gate** is Android-hard (≤ UI Parity Bar vs Flutter Android SoT) when the pixel phase is active; iOS/Harmony require open main path plus sampling, not the same hard gate.
6. Implementation units are **Parity Domain Tickets** (small domain, ~2–4 routes); Mine island and native cutover tracks may run in parallel against one inventory. Inventory draft lives in-repo and syncs to GitHub issues.
7. **Logic-first phase (2026-09-29, change `logic-first-parity`)**: hard acceptance is the **Logic Acceptance Packet** (Shared Presentation Logic / Flutter mock depth + three-platform open path + gap honesty). Screenshot Diff Gate is **optional** until this phase is explicitly ended in `CONTEXT.md` / inventory. Do not ship new SoT body bitmaps as primary delivery.

## Consequences

- ADR 0002 §6 no longer caps scope; deferred stubs are technical debt to clear, not a product ceiling.
- SecondaryRouteIsland is Mine-only after #23: in-scope non-Mine routes open via Native Shell UI only; the island refuses non-Mine hosts.
- Acceptance is evidence-based: during logic-first, behavior/mock parity + open path + gap registry (inventory `logic-pass`); pixel mse is deferred, not deleted. Full Route Acceptance Packet (incl. Screenshot Diff Gate) resumes after the phase ends.
