## 1. Baseline & tokens

- [x] 1.1 Confirm gold `notes/evidence/flutter/mine_root.png` and map Flutter Mine widgets → KMP `MineHomeContent` sections
- [x] 1.2 Add/align Mine visual tokens (largeTitle 32, radius 12/14, page `#F2F2F7`, accent `#007AFF`, light card shadow) vs Flutter `MineTheme`
- [x] 1.3 Inventory OHOS-safe icons for 常用服务 / 个人功能 / header actions (PNG or custom ImageVector; no material-icons-extended)

## 2. Mine root rebuild

- [x] 2.1 Rebuild header: left large title「我的」+ four trailing action icons (info / calendar / settings / logout) matching gold spacing
- [x] 2.2 Rebuild profile card: avatar, name, role badge, store row, 电子名片 chip, masked phone
- [x] 2.3 Rebuild four-stat row (加入天数 / 员工数 / 店铺天数 / 累计客户) with Flutter stat typography
- [x] 2.4 Rebuild「常用服务」four-up grid with colored icon tiles + HOT on 商城
- [x] 2.5 Rebuild「个人功能」two-column cards (至少短信模板 + 购车计算器含 `5830.00`) +「长按拖动顺序」hint (reorder behavior stub OK)
- [x] 2.6 `./gradlew :composeApp:compileDebugKotlinAndroid`

## 3. Evidence & handoff

- [x] 3.1 Capture Android KMP Mine root screenshot beside Flutter gold under `notes/evidence/`
- [x] 3.2 Update this change acceptance note + cross-link `parity-flutter-to-kmp` Mine matrix row
- [x] 3.3 Record iOS/OHOS as Partial if not device-verified; optional OHOS publish if Mine resources added
