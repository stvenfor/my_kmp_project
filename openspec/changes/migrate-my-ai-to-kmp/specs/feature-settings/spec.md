## Purpose

Defines the Mine/Settings product surface migrated from Flutter `module_settings`, including profile entry, settings, and environment/capability entry points that are part of the product (not debug-only kits).

## ADDED Requirements

### Requirement: Mine tab root
The system SHALL provide a Mine tab root showing account/guest status and entry rows to settings and key personal destinations.

#### Scenario: Guest Mine
- **WHEN** a guest opens Mine
- **THEN** guest status is visible and login entry is available

### Requirement: Settings destinations
The system SHALL provide settings and personalized settings destinations reachable from Mine.

#### Scenario: Open settings
- **WHEN** the user opens Settings from Mine
- **THEN** settings content is shown and back returns to Mine root

### Requirement: Environment visibility
The system SHALL expose the active API environment/host in a discoverable settings or Mine location for diagnostics without requiring native-only tooling.

#### Scenario: View API host
- **WHEN** the user opens the environment/host row
- **THEN** the current effective API base URL is displayed consistently on all three targets
