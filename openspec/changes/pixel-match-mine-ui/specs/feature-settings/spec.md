## ADDED Requirements

### Requirement: Mine root visual parity with Flutter module_settings
The Mine tab root SHALL match Flutter `module_settings` Mine page visuals for header actions, profile card, stats, common services, and personal function cards as shown in the product SoT screenshot attached to this change.

#### Scenario: Common services icons
- **WHEN** the user views「常用服务」
- **THEN** each entry shows a colored icon tile and label consistent with Flutter (including HOT on 商城 when SoT includes it)

#### Scenario: Personal function cards
- **WHEN** the user views「个人功能」
- **THEN** cards appear in a two-column grid with icon, title, subtitle, and any highlighted metric Flutter shows (e.g. calculator value), plus the「长按拖动顺序」hint
