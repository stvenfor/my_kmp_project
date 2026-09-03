## Purpose

Extends the shared app shell so cold start, privacy, and chrome meet Flutter pixel parity and persistent consent—stub shells no longer count as complete.

## MODIFIED Requirements

### Requirement: Cold start reaches main shell
The system SHALL present a splash/entry flow matching Flutter splash timing and visuals within pixel tolerance, then open the main shell without requiring login.

#### Scenario: First launch after privacy accepted
- **WHEN** the user completes or has previously accepted privacy consent
- **THEN** the app opens the main shell with four tabs available and splash visuals match Flutter reference

### Requirement: Four primary tabs
The system SHALL expose exactly four root tabs: Home, Chat, Community, and Mine (我的), with labels, icons, and selection chrome matching Flutter pixel-level tab bar treatment.

#### Scenario: Tab selection
- **WHEN** the user selects a root tab
- **THEN** that tab’s root content is shown and bottom bar selection visuals match Flutter on phone layouts

### Requirement: Soft auth gate on protected tabs
The system SHALL require an authenticated session before entering Chat or Community root content; unauthenticated selection MUST route to login and MUST NOT silently show protected content.

#### Scenario: Guest selects Chat
- **WHEN** a guest user selects the Chat tab
- **THEN** the app navigates to the login flow and does not show chat list content

### Requirement: Immersive chrome consistency
The system SHALL use edge-to-edge immersive chrome on Android, iOS, and HarmonyOS matching Flutter safe-area and tab-hide behavior on secondary pages.

#### Scenario: Secondary page back navigation
- **WHEN** the user opens a secondary page from a tab root
- **THEN** the main tab bar is hidden and interactive controls remain clear of system insets on all three targets

### Requirement: Three-platform parity of shell routes
The system SHALL provide the same shell route graph (splash → main → tab roots → secondary stacks) on Android, iOS, and HarmonyOS with pixel-comparable chrome.

#### Scenario: Same path on each target
- **WHEN** a user follows splash → Home → a documented secondary route
- **THEN** the sequence and resulting screens match on Android, iOS, and HarmonyOS against Flutter

## ADDED Requirements

### Requirement: Privacy consent persistence
The system SHALL persist privacy acceptance so relaunch skips the consent screen after acceptance, matching Flutter linking privacy behavior.

#### Scenario: Relaunch after accept
- **WHEN** the user accepted privacy and kills the app
- **THEN** the next cold start does not require accepting privacy again
