## ADDED Requirements

### Requirement: Membership visual and asset parity
Membership/pay screens SHALL use Flutter `module_pay` visual structure and synced pay/membership assets (headers, plan cards, badges, channel icons) while keeping unavailable-channel honesty when SDKs are missing.

#### Scenario: Plan cards match Flutter layout
- **WHEN** the user opens membership
- **THEN** tier tabs, promo banner, plan cards, and channel rows match Flutter pay visuals within the pixel SOP

#### Scenario: Assets not omitted
- **WHEN** Flutter pay UI shows channel or promotional imagery
- **THEN** KMP shows the synced equivalent resource (or an explicitly documented gap in the visual inventory), not a blank spacer
