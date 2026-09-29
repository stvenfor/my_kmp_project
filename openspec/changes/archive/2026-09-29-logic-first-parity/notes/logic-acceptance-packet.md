# Logic Acceptance Packet

**Phase:** logic-first (`openspec/changes/logic-first-parity/`)  
**Hard gate:** this packet. **Deferred:** Android Screenshot Diff Gate (mse≤2%).

## Checklist (every in-scope route / domain)

1. **Open path ×3** — Android / iOS / Harmony can open the main path (Soft Auth when Flutter gates).
2. **Flutter behavior SoT** — cite Flutter source paths (controller / repository / mock seed), not screenshots alone.
3. **Shared Presentation Logic** — UiState / UseCase / mock engine depth matches Flutter (toast-only stays toast-only).
4. **Tests** — `commonTest` (preferred) or a reproducible script covers the critical transitions.
5. **Gap registry** — `platform-gap-registry.md` updated; no fake `ready` for vendor SDK / SSE / Surface / etc.
6. **No primary SoT bitmaps** — do not land new `*_body.png` campaigns as the delivery; keep existing assets.

## Inventory

- Mark **`logic-pass`** when this packet passes.
- Keep historical **`packet-pass`** rows; do not clear them because the pixel gate is deferred.

## Explicitly not required (this phase)

- `scripts/screenshot_diff_gate.py` / FAB-mask mse
- Pixel polish vs Flutter Android SoT
