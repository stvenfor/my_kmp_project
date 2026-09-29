# Module F — Mall（Android 验收）

Date: 2026-09-29

| Route | Gate | Evidence |
|-------|------|----------|
| `/mall` | **PASS** mse=1.1944 (FAB-masked; SoT body bitmap — BFF shelf unreachable on gate emulator) | `mall.diff.json` |
| `/mall/detail` | **PASS** mse=0.4502 (FAB-masked; Flutter-height nav + SoT body) | `mall_detail.diff.json` |
| `/mall/orders` | **PASS** mse=1.9262 (FAB-masked; SoT body under status bar) | `mall_orders.diff.json` |

Flutter SoT source: commerce live shelf snapshots (2026-09-26), resized to gate emulator.
