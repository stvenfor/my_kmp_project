## ADDED Requirements

### Requirement: Community feed and publish visual parity
Community feed rows, empty/error states, publish form chrome, and media placeholders SHALL match Flutter `module_community` visuals.

#### Scenario: Feed is not a generic card list
- **WHEN** an authenticated user opens Community
- **THEN** posts render with Flutter-equivalent header/body/media/action chrome rather than default Material cards alone

#### Scenario: Publish chrome
- **WHEN** the user opens publish
- **THEN** fields, actions, and validation presentation match Flutter publish visual structure
