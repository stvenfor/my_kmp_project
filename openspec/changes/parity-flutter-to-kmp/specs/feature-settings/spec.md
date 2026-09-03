## Purpose

Aligns Mine/Settings product surfaces with Flutter `module_settings` product pages (excluding debug-only kits unless product includes them).

## MODIFIED Requirements

### Requirement: Mine tab root
The system SHALL provide a Mine tab root showing account/guest status and entry rows matching Flutter Mine page IA and pixel layout.

#### Scenario: Guest Mine
- **WHEN** a guest opens Mine
- **THEN** guest status is visible and login entry is available matching Flutter

### Requirement: Settings destinations
The system SHALL provide settings and personalized settings destinations reachable from Mine with Flutter-equivalent sections and controls.

#### Scenario: Open settings
- **WHEN** the user opens Settings from Mine
- **THEN** settings content is shown matching Flutter structure and back returns to Mine root

### Requirement: Environment visibility
The system SHALL expose the active API environment/host in a discoverable settings or Mine location for diagnostics without requiring native-only tooling.

#### Scenario: View API host
- **WHEN** the user opens the environment/host row
- **THEN** the current effective API base URL is displayed consistently on all three targets

## ADDED Requirements

### Requirement: Product vs debug separation
Debug-only Flutter demos (BLE playground, DoKit, invoice experiments) SHALL either be explicitly in-scope with parity tasks or listed out-of-scope; in-scope items MUST meet pixel/behavior gates, out-of-scope MUST NOT block product Mine acceptance.

#### Scenario: Out-of-scope debug
- **WHEN** a Flutter settings debug entry is declared out-of-scope
- **THEN** its absence does not fail Mine product acceptance but MUST be noted in the gap or non-goals registry
