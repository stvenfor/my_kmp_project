# Mine widget → KMP section map

Gold: `notes/evidence/flutter/mine_root.png` (Flutter iOS Mine tab root).

| Flutter SoT | KMP |
|-------------|-----|
| `MineHeaderWidget` large title + `_TopIcon`×4 | `MineTopChrome` + `TopIconButton` (`MineIcons`) |
| `_HeaderBody` profile `DecoratedBox` | `ProfileCard` |
| `MineStatsBarWidget` | `StatsBar` |
| `MineQuickServicesWidget` | `QuickServicesSection` |
| `MineFunctionSectionWidget` + `MineFunctionCardWidget` | `FunctionSection` + `FunctionCard` |
| `MineMenuListWidget` | `MenuSection` |
| `MineTheme` | `MineTheme.kt` |
| Icons (Cupertino / Material) | `MineIcons.kt` custom ImageVector (OHOS-safe) |

Pixel demo profile: `MineCatalog.goldDemoProfile` (用户0000 / 1028… / 购车计算器 `5830.00` / HOT).
