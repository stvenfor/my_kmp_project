## Purpose

Upgrades WebView/JS bridge, deep link delivery, scan, and push entry from stubs to real three-platform bridges matching Flutter commons/components.

## MODIFIED Requirements

### Requirement: In-app WebView with bridge
The system SHALL open in-app web pages in a real platform WebView and support the documented JS bridge method set used by Flutter H5 entry points.

#### Scenario: Open web route
- **WHEN** the app navigates to an in-app web URL
- **THEN** the page loads in a shared WebView host and back dismisses to the previous screen

### Requirement: Deep link entry
The system SHALL accept configured deep links / URL schemes from platform delivery (intents, Universal Links, OHOS skills) and route into the shared navigation graph after splash when required.

#### Scenario: Cold start deep link
- **WHEN** the app is launched via a supported deep link on a target
- **THEN** after shell readiness the corresponding destination opens; targets without delivery adapters are incomplete

### Requirement: Scan capability
The system SHALL provide a scan/QR entry that uses the camera, returns a decoded payload or a clear permission/error state, matching Flutter scan UX at pixel level—simulation-only flows MUST NOT pass acceptance.

#### Scenario: Permission denied
- **WHEN** camera permission is denied
- **THEN** scanning fails gracefully with user-visible guidance matching Flutter

### Requirement: Three-platform bridge parity
Bridge method names and routing outcomes used by product H5 SHALL match across Android, iOS, and HarmonyOS for the supported method set.

#### Scenario: Shared bridge method
- **WHEN** H5 invokes a supported bridge method
- **THEN** the resulting native navigation or callback behavior matches on each target for that method

## ADDED Requirements

### Requirement: Push entry routing
Where Flutter uses push notifications to open destinations, KMP SHALL register for push on each target and route payloads into the shared graph, or list incomplete targets in the gap registry.

#### Scenario: Push opens chat
- **WHEN** a supported push payload arrives for a chat destination
- **THEN** after auth/shell rules the chat destination opens on targets with working push adapters
