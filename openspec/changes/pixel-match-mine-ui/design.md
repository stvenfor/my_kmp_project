## Context

See `proposal.md` — Why. Gold: `notes/evidence/flutter/mine_root.png` (Flutter iOS Mine). Flutter SoT widgets: `mine_header_widget`, `mine_quick_services_widget`, `mine_function_section_widget`, `MineTheme`. KMP today: `feature/mine/MineHomeContent.kt` has IA but icon/letter/density drift. `sync-flutter-ui-visual` synced some assets and chevrons; this change is Mine-root pixel closeout only. Immersive shell + `ReportMainTabRoot` rules remain.

## Goals / Non-Goals

**Goals:**
- Rebuild Mine root Compose to match gold structure and MineTheme metrics.
- OHOS-safe icons (drawable / simple vectors — no material-icons-extended).
- Android Flutter↔KMP evidence under this change.

**Non-Goals:**
- Home/Chat/Community pixel; real SMS/calculator/e-card backends; long-press reorder implementation beyond visual affordance (hint may be shown; drag can be stubbed).

## Decisions

### D1 — Gold is the attached Flutter Mine screenshot
**Choice:** User wrote「首页」but attached Mine; scope = Mine tab root.  
**Alt:** Also rebuild Home — deferred to a separate change.

### D2 — Mirror Flutter widget sections 1:1 in Compose
**Choice:** Header / profile card / stats / quick services / function grid as separate composables matching Flutter file boundaries.  
**Why:** Easier pixel diff and ownership.  
**Alt:** One monolithic screen — rejected.

### D3 — Icons without material-icons-extended
**Choice:** Prefer Flutter-exported or home/settings PNGs; else lightweight custom `ImageVector` / tinted drawables.  
**Why:** Extended Material icons fail OHOS resolution in this fork.

### D4 — Calculator value and HOT badge are visual SoT
**Choice:** Show `5830.00` and HOT as in gold (mock data OK).  
**Why:** Pixel parity; not claiming live pricing.

### D5 — Long-press reorder
**Choice:** Show「长按拖动顺序」copy; actual drag reorder optional stub (toast) unless cheap to wire.  
**Why:** Spec requires hint; full reorder not needed for 1A Pass of first viewport.

## Risks / Trade-offs

- **[Risk] Guest vs logged-in copy** → Match gold「用户0000」demo when session has no profile; document mapping.
- **[Risk] Shadow/radius platform variance** → Use MineTheme radii (12/14) and light shadow; accept minor raster diff.
- **[Trade-off] Home still wrong if user meant Home tab** → Explicit in proposal; wait for follow-up change.

## Migration Plan

1. Extract/align Mine tokens.  
2. Rebuild sections against gold.  
3. Compile Android; capture KMP Mine; pair with flutter gold.  
4. Cross-link parity matrix Mine row.  

Rollback: revert `feature/mine` diffs.

## Open Questions

- Whether long-press reorder must work in this change — default **visual hint only**.
- Guest Mine name/phone when not logged in — default **keep gold demo strings** for pixel accept.
