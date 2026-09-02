## Purpose

Defines the Home tab product surface migrated from Flutter `module_home`, including root feed/entry hubs and key secondary journeys.

## ADDED Requirements

### Requirement: Home tab root
The system SHALL provide a Home tab root that exposes the primary service entries and content hubs equivalent to the Flutter home module’s main page.

#### Scenario: Open Home
- **WHEN** the user selects the Home tab while authenticated or as guest
- **THEN** the Home root content is visible without an auth gate

### Requirement: Core secondary destinations
The system SHALL support navigation from Home to documented secondary destinations including learning report, all services, search, and strategy/content hubs that exist in the source home module.

#### Scenario: Open all services
- **WHEN** the user opens the all-services entry from Home
- **THEN** the all-services screen is shown and back returns to Home root

### Requirement: Three-platform Home parity
Home root and listed secondary destinations SHALL present the same information architecture and primary actions on Android, iOS, and HarmonyOS.

#### Scenario: Parity check
- **WHEN** the same Home secondary destination is opened on each target
- **THEN** titles, primary actions, and navigation back behavior match
