# Phase-1 parity — honest status (ADR 0002)

**Do not confuse screenshot MSE with product completion.**

## What “done” means here

| Gate | Meaning | Status |
|------|---------|--------|
| Android first-viewport **looks like** Flutter SoT (MSE≤1%) | Visual shell only | Previously “passed” via SoT-baked assets + mock seeds — **not product parity** |
| Clickable paths match Flutter module behavior | Real UX | **In progress** |
| iOS / Harmony same behavior | Multi-platform | **Not started for pixel/product depth** |
| Full Flutter feature set (mall/pay/video/live/…) | Product | **Far from done (~20%)** |

## Android capability (updated this session)

| Path | Before | Now |
|------|--------|-----|
| Community media | SoT-baked dual PNG | Flutter-style **3×9 ImageGrid** + picsum seeds; video cover via Coil |
| Home「更多」 | Deferred stub | Opens real **`AllServicesScreen`** |
| Mine「商城」 | snackbar「开发中」 | Opens **`MembershipScreen`** |
| Chat row | Dead list | Opens **chat detail** + send bubble (local mock) |
| Home other features / Mine wallet·课程·订单 / IM / HTTP feed | Stub / missing | Still stub or missing |

## Remaining high-impact work (ordered)

1. Home feature taps → real secondary pages (not DeferredStub)
2. Community HTTP feed + publish (not 2 static cards)
3. Chat real IM / history (not local bubble list)
4. Mine wallet / course / order real modules
5. Home metrics/todos live API with Flutter-compatible fallback
6. iOS + Harmony depth parity
7. Mall / pay / video / live / classroom product paths

## Measurement policy going forward

- MSE is a **regression signal**, not a ship gate for “完成度”.
- Acceptance = **user can tap through** the same primary flows as Flutter for scoped Phase-1 tabs, with data contracts documented.
- SoT-baked screenshot assets for community/home are **technical debt**; prefer network/catalog assets + layout contract.
