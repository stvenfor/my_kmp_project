## ADDED Requirements

### Requirement: Home root visual parity
Home root layout, module order, spacing, and entry icons/images SHALL match Flutter `module_home` main page visuals for the same content state.

#### Scenario: Feature grid uses real assets
- **WHEN** the user views the Home service/feature grid
- **THEN** each entry shows the synced Flutter icon/image and label styling, not a letter tile

### Requirement: Home secondary visual parity
All-services, search, learning report, and strategy (and other documented Home secondaries in scope) SHALL match Flutter secondary page chrome and key visual blocks.

#### Scenario: All services icons
- **WHEN** the user opens all-services
- **THEN** service tiles use synced assets and layout density comparable to Flutter
