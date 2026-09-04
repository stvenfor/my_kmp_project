# Spike II exit review (lead)

**Date:** 2026-09-04  
**Verdict:** **Conditional Pass** — all `tasks.md` items `[x]`; Android compile + layer guard green; several Must rows remain Partial (honest).

## Gate evidence

| Check | Result |
|-------|--------|
| `tasks.md` 1.x–5.x | All `[x]` |
| `scripts/check-layer-deps.sh` | OK (0 feature→feature) |
| `./gradlew :composeApp:compileDebugKotlinAndroid` | BUILD SUCCESSFUL |
| Gradle includes | `:composeApp`, `:core:network`, `:core:account`, `:ohosAggregate` (placeholder) |

## Spike II task rollup

| Task | Status | Note |
|------|--------|------|
| 4.1 Pay sandbox | Pass (sandbox) / Must **Partial** | Real OpenSDK unwired |
| 4.2 Push ports | Pass (skeleton) / Must **Partial** | Platform SDK unwired |
| 4.3 Core modules | **Partial** | Extracted + Android OK; `libkn.so` still `:composeApp`; `:ohosAggregate` placeholder |
| 4.4 DesignSystem | Pass (lightweight) | |
| 4.5 Parity unfreeze | Pass (policy) | Gate: `parity-unfreeze.md` + `boundary-gate.md` |

## Carry-over (not blocking this change close)

1. Promote `:ohosAggregate` to own fat `libkn.so` + publish path; OHOS link smoke.
2. Wire production Pay / Push SDKs; flip Must matrix when evidenced.
3. Resume Flutter parity only under boundary gate + `check-layer-deps.sh`.
4. Narrow over-broad `public` APIs introduced during module extract.

## Related notes

- `spike-i-exit-review.md` — Spike I Conditional Pass  
- `core-modules-extraction.md` — 4.3 details  
- `home-hub-migration.md` — Home hub → `AppRoute`  
- `spike-ii-remaining.md` — Must Partial inventory  
- `parity-unfreeze.md` — post-spike parity rules  
