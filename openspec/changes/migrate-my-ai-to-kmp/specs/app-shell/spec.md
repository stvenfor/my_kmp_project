## Purpose

Defines the shared multiplatform app shell: cold start, privacy consent, four-tab main chrome, and immersive navigation consistency on Android, iOS, and HarmonyOS.

## ADDED Requirements

### Requirement: Cold start reaches main shell
The system SHALL present a splash/entry flow and then open the main shell without requiring login.

#### Scenario: First launch after privacy accepted
- **WHEN** the user completes or has previously accepted privacy consent
- **THEN** the app opens the main shell with four tabs available

### Requirement: Four primary tabs
The system SHALL expose exactly four root tabs: Home, Chat, Community, and Mine (我的), with stable labels and selection state.

#### Scenario: Tab selection
- **WHEN** the user selects a root tab
- **THEN** that tab’s root content is shown and the bottom/rail selection matches across phone layouts

### Requirement: Soft auth gate on protected tabs
The system SHALL require an authenticated session before entering Chat or Community root content; unauthenticated selection MUST route to login and MUST NOT silently show protected content.

#### Scenario: Guest selects Chat
- **WHEN** a guest user selects the Chat tab
- **THEN** the app navigates to the login flow and does not show chat list content

### Requirement: Immersive chrome consistency
The system SHALL use edge-to-edge immersive chrome on Android, iOS, and HarmonyOS so that root and secondary screens avoid overlapping system bars for interactive controls.

#### Scenario: Secondary page back navigation
- **WHEN** the user opens a secondary page from a tab root
- **THEN** the main tab bar is hidden and interactive controls remain clear of system insets on all three targets

### Requirement: Three-platform parity of shell routes
The system SHALL provide the same shell route graph (splash → main → tab roots → secondary stacks) on Android, iOS, and HarmonyOS.

#### Scenario: Same path on each target
- **WHEN** a user follows splash → Home → a documented secondary route
- **THEN** the sequence and resulting screens match on Android, iOS, and HarmonyOS
