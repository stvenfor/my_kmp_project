# Pixel acceptance SOP (1A) + Android Screenshot Diff Gate

## Reference
- Flutter app from `/Users/mac/Desktop/github/my_ai_project` on the same device class (phone portrait).
- KMP app from this repo on the same target OS build.

## Capture
1. Use the same logical viewport (prefer same physical device or matched emulator DPI).
2. Capture Flutter screen, then KMP screen for the same route/state.
3. Store under `openspec/changes/parity-flutter-to-kmp/notes/evidence/<path>/<target>/` as `{route}.flutter.png` / `{route}.kmp.png`.

## Android Screenshot Diff Gate (hard · ADR 0003 / issue #2)

```bash
python3 scripts/screenshot_diff_gate.py \
  --flutter path/to/flutter.png \
  --kmp path/to/kmp.png \
  --out-dir openspec/changes/parity-flutter-to-kmp/notes/evidence/<module>/Android \
  --route <slug> \
  --bar 2.0
```

- **Pass:** `mse_pct <= 2.0` (UI Parity Bar). Exit code 0.
- **Outputs:** `{route}.flutter.png`, `{route}.kmp.png`, `{route}.heat.png`, `{route}.diff.json`.
- **ROI default:** top=0.04, bot=0.90, resize=540×960 (status/nav chrome cropped).
- **Sample evidence:** `notes/evidence/harness/Android/home.diff.json` (home mse_pct≈0.33).

### iOS / Harmony (auxiliary, not hard gate)
Open-path smoke + sampling screenshots only. Do not claim pixel-ready from iOS/OHOS alone.

## Pass criteria (manual + gate)
- Gate PASS for Android SoT vs KMP.
- Layout structure (sections, order) matches.
- Colors, typography scale, spacing, corner radii, and assets match within zero intentional drift.
- Interactive chrome (tab bar, nav bar, insets) matches Flutter safe-area behavior.

## Fail
- Placeholder/stub copy where Flutter has real UI.
- Missing modules, wrong colors, or unwired entries.
- Gate FAIL (`mse_pct > bar`) without an approved exception note.

## Record
Update `acceptance-matrix.md` + `parity-inventory.md`; link evidence folder; keep `platform-gap-registry.md` honest (no fake pixel ready).
