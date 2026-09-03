## Purpose

Defines unified contracts for WebView/JS bridge, deep links/push entry, and scanning capabilities so platform differences stay behind shared APIs.

## ADDED Requirements

### Requirement: In-app WebView with bridge
The system SHALL open in-app web pages and support a documented JS bridge for navigation and capability calls used by migrated H5 entry points.

#### Scenario: Open web route
- **WHEN** the app navigates to an in-app web URL
- **THEN** the page loads in a shared WebView host and back dismisses to the previous screen

### Requirement: Deep link entry
The system SHALL accept configured deep links / URL schemes and route into the shared navigation graph after splash when required.

#### Scenario: Cold start deep link
- **WHEN** the app is launched via a supported deep link
- **THEN** after shell readiness the corresponding destination opens

### Requirement: Scan capability
The system SHALL provide a scan/QR entry that returns a decoded payload or a clear permission/error state.

#### Scenario: Permission denied
- **WHEN** camera permission is denied
- **THEN** scanning fails gracefully with user-visible guidance

### Requirement: Three-platform bridge parity
Bridge method names and routing outcomes used by product H5 SHALL match across Android, iOS, and HarmonyOS for the supported method set.

#### Scenario: Shared bridge method
- **WHEN** H5 invokes a supported bridge method
- **THEN** the resulting native navigation or callback behavior matches on each target for that method
