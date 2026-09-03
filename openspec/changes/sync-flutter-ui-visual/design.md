## Context

See `proposal.md` — Why. SoT is Flutter at `/Users/mac/Desktop/github/my_ai_project`. KMP today has wiring for many routes but almost no product assets (`composeResources/drawable` ≈ splash + two tab icons) and many screens use letter tiles / Material defaults. `parity-flutter-to-kmp` remains the 2A/SDK track; this change owns 1A visual/resource closure. Immersive shell rules in AGENTS.md stay mandatory.

## Goals / Non-Goals

**Goals:**
- Inventory + sync Flutter product assets into Compose resources with a maintainable map.
- Rebuild shared Compose UIs surface-by-surface to match Flutter layout/chrome/assets.
- Produce Flutter↔KMP evidence pairs; reject skeleton Pass.
- Keep three-platform shared UI (commonMain-first); shell assets verified per host where needed.

**Non-Goals:**
- Real pay/IM/push/live SDKs; bfui/DoKit demos; brand reintroduction.
- Redesigning Flutter UX — copy SoT, do not invent a new system.

## Decisions

### D1 — Assets-first, then pixels per surface
**Choice:** Sync asset packs (home, settings/mine, pay, chat/community shared icons, tab chrome) before deep layout rewrites of each screen.  
**Why:** Most “极大差距” is missing imagery; layout alone cannot look like Flutter without icons.  
**Alt:** Layout-first with placeholders — rejected (reproduces current gap).

### D2 — Compose resources as shared SoT; thin platform shells
**Choice:** Put feature assets under `composeApp/src/commonMain/composeResources/` (drawable/font as needed). Android mipmap / iOS AppIcon / Harmony media only for launcher/splash host contracts.  
**Why:** Matches KMP common-first; one sync feeds three UIs.  
**Alt:** Duplicate assets per platform — rejected (drift).

### D3 — Module-scoped resource folders + manifest map
**Choice:** Mirror Flutter module folders in a documented map file under this change (`notes/asset-sync-map.md`): Flutter path → Compose resource id → consuming screen. Scriptable copy step preferred over ad-hoc drops.  
**Why:** Auditability; prevents silent omissions.  
**Alt:** Dump all PNGs flat into drawable — rejected (collisions / untraceable).

### D4 — Surface wave order
**Choice:** (1) tokens + shell/tab/splash (2) Home + secondaries (3) Mine/Settings (4) Community + Chat (5) Commerce membership visuals (6) Media hub (7) friend/live/classroom polish if still skeleton after shared chrome.  
**Why:** Matches user focus (Community open) after fixing shared tokens/home which set the visual baseline.  
**Alt:** Community-only first — rejected (user asked 全部同步).

### D5 — Relationship to `parity-flutter-to-kmp`
**Choice:** Do not merge changes. On visual Pass, update parity matrix notes to point at this change’s evidence; leave 2A checkboxes to parity.  
**Why:** Clear ownership; avoids reopening SDK tasks in a UI change.

### D6 — Fonts
**Choice:** Prefer Flutter-bundled fonts if product-critical; otherwise system font with matching size/weight scale from AppTheme. Record gaps in asset map.  
**Why:** Avoid illegal/unlicensed font copies; match metrics first.

## Risks / Trade-offs

- **[Risk] Asset volume / binary size** → Sync only product-path assets; exclude bfui/DoKit; compress where Flutter already uses density variants (pick @2x/@3x or single mdpi carefully).
- **[Risk] SVG/Lottie not 1:1 in Compose** → Prefer PNG/WebP exports used by Flutter runtime; Lottie → static keyframe or skip with map note if rare.
- **[Risk] OHOS resource publish path** → After Compose resource adds, re-run Harmony publish so `rawfile/composeResources` stays current.
- **[Risk] Scope creep into 2A** → Hard gate: no SDK work in this change; unavailable pay stays honest.
- **[Trade-off] Android evidence first** → iOS/OHOS may lag as Partial in registry; still required in tasks.

## Migration Plan

1. Freeze inventory from Flutter packages in scope.
2. Copy/convert → composeResources; wire `painterResource` / tab icons.
3. Rebuild screens wave-by-wave; compile Android each wave.
4. Capture Flutter + KMP pairs; update evidence + matrix for this change.
5. Cross-link parity change notes; archive when Pass criteria met.

Rollback: revert resource adds + screen diffs per wave (git); no server migration.

## Open Questions

- Exact Flutter font files licensed for redistribution (if any custom TTF) — resolve during asset inventory without blocking PNG sync.
- Whether friend/live/classroom stay “demo mock” content but still require Flutter visual chrome — default **yes** for chrome/assets if Flutter modules ship UI; content can remain mock.
