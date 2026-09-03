## Purpose

Defines how Flutter→KMP product parity is verified: pixel-level UI, business-flow equivalence, three-platform completion rules, and how incomplete platform capabilities are recorded without counting as done.

## ADDED Requirements

### Requirement: Pixel-level UI acceptance
The system SHALL treat UI as complete for a screen only when side-by-side comparison with the Flutter reference on the same device class shows matching layout structure, colors, typography scale, spacing, corner radii, and assets within agreed pixel tolerance (zero intentional visual drift).

#### Scenario: Screen fails pixel gate
- **WHEN** a migrated screen differs from Flutter in spacing, color, or typography beyond the agreed tolerance
- **THEN** that screen MUST be marked incomplete and MUST NOT be claimed as UI-complete in acceptance evidence

### Requirement: Business-logic equivalence
The system SHALL match Flutter observable business behavior for the same user actions: navigation outcomes, auth gates, validation errors, success/failure states, and data displayed from the same backend contracts.

#### Scenario: Auth-gated tab
- **WHEN** a guest selects Chat or Community
- **THEN** login is required and protected content is not shown, matching Flutter MainPage gating

### Requirement: Three-platform completion rule
A capability SHALL be marked complete only when Android, iOS, and HarmonyOS all satisfy the capability’s acceptance scenarios with real (non-stub) platform adapters where the Flutter product uses real plugins; otherwise the capability remains incomplete.

#### Scenario: One platform missing SDK
- **WHEN** a native capability works on Android and iOS but has no real adapter on HarmonyOS
- **THEN** the capability is incomplete and MUST appear in the platform gap registry with the missing target listed

### Requirement: Golden-path evidence
Acceptance SHALL record golden paths (route sequence + expected screens) with evidence (screenshots or recordings) per target for shell, auth, home, chat, community, mine, media, commerce, and bridges in scope.

#### Scenario: Missing evidence
- **WHEN** a golden path lacks evidence for a required target
- **THEN** acceptance for that path MUST fail
