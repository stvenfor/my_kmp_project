## Purpose

Defines pixel and structural parity for the Mine (我的) tab root against the Flutter `module_settings` Mine screenshot SoT, so KMP cannot pass with skeleton chrome.

## ADDED Requirements

### Requirement: Mine root matches Flutter screenshot structure
The Mine tab root SHALL present, in order: large title「我的」with trailing action icons; profile card (avatar, display name, role badge, store/org row, electronic card affordance, masked phone); four-stat row;「常用服务」four-up icon grid with HOT badge where Flutter shows it;「个人功能」section with reorder hint and two-column function cards including calculator highlight value when present in SoT.

#### Scenario: Open Mine while logged in or guest-demo
- **WHEN** the user selects the Mine tab
- **THEN** the sections above are visible in Flutter order and are not replaced by text-only letter-tile placeholders for icons that Flutter renders as colored icon tiles

### Requirement: Flutter screenshot is the acceptance gold
Visual Pass for Mine root on a target SHALL include evidence pairing the Flutter gold (`notes/evidence/flutter/mine_root.png` or equivalent capture) with a KMP capture of the same route/state under the project pixel SOP.

#### Scenario: Android accept
- **WHEN** Mine root is marked Pass for Android
- **THEN** evidence links Flutter and KMP screenshots for Mine root and layout/spacing/icon treatment match within zero intentional drift for the sections in the gold image

### Requirement: Theme tokens for Mine chrome
Mine root typography, page background, card surface, accent, separators, and card corner radii SHALL match Flutter MineTheme roles used by the gold screenshot (large title ~32sp bold; page `#F2F2F7`; surface white; accent `#007AFF`).

#### Scenario: Large title and page background
- **WHEN** Mine root is shown
- **THEN** the title「我的」uses large-title weight/size and the scaffold uses the Flutter Mine page background role
