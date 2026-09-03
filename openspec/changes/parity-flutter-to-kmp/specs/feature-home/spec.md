## Purpose

Brings Home root and secondary journeys to Flutter `module_home` business and pixel parity, replacing launcher-only placeholders.

## MODIFIED Requirements

### Requirement: Home tab root
The system SHALL provide a Home tab root that reproduces Flutter home main page information architecture, modules, and visuals at pixel level for guest and authenticated users without an auth gate.

#### Scenario: Open Home
- **WHEN** the user selects the Home tab while authenticated or as guest
- **THEN** the Home root content matches Flutter layout and primary entries

### Requirement: Core secondary destinations
The system SHALL support navigation from Home to Flutter-documented secondary destinations including learning report, all services, search, strategy/content hubs, media entry, and in-app web entry with real screens (not title-only placeholders).

#### Scenario: Open all services
- **WHEN** the user opens the all-services entry from Home
- **THEN** the all-services screen matches Flutter content structure and back returns to Home root

### Requirement: Three-platform Home parity
Home root and listed secondary destinations SHALL present the same information architecture, primary actions, and pixel-comparable UI on Android, iOS, and HarmonyOS.

#### Scenario: Parity check
- **WHEN** the same Home secondary destination is opened on each target
- **THEN** titles, primary actions, visuals, and navigation back behavior match Flutter

## ADDED Requirements

### Requirement: Wired media and web entries
Home entries for media and embedded web SHALL open the real media and WebView hosts, not generic secondary stubs.

#### Scenario: Open media from Home
- **WHEN** the user taps the media entry on Home
- **THEN** the media hub matching Flutter video/music entry is shown
