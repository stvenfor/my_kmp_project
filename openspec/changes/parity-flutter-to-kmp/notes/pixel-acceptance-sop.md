# Pixel acceptance SOP (1A)

## Reference
- Flutter app from `/Users/mac/Desktop/github/my_ai_project` on the same device class (phone portrait).
- KMP app from this repo on the same target OS build.

## Capture
1. Use the same logical viewport (prefer same physical device or matched emulator DPI).
2. Capture Flutter screen, then KMP screen for the same route/state.
3. Store under `openspec/changes/parity-flutter-to-kmp/notes/evidence/<path>/<target>/` as `flutter.png` / `kmp.png`.

## Pass criteria
- Layout structure (sections, order) matches.
- Colors, typography scale, spacing, corner radii, and assets match within zero intentional drift.
- Interactive chrome (tab bar, nav bar, insets) matches Flutter safe-area behavior.

## Fail
- Placeholder/stub copy where Flutter has real UI.
- Missing modules, wrong colors (e.g. Demo blue vs Flutter `#007AFF`), or unwired entries.

## Record
Update `acceptance-matrix.md` result to Pass/Fail and link evidence folder.
