## Purpose

Defines live list/room entry parity with Flutter `module_live`.

## ADDED Requirements

### Requirement: Live list and room entry
The system SHALL provide live list and room entry destinations aligned with Flutter routes and visible states.

#### Scenario: Open live list
- **WHEN** the user opens live from a documented Home or route entry
- **THEN** the live list appears and selecting a room follows Flutter navigation outcomes

### Requirement: Three-platform live parity
Live surfaces SHALL pass pixel and behavior acceptance on Android, iOS, and HarmonyOS; missing realtime SDK on a target marks the capability incomplete in the gap registry.

#### Scenario: Missing realtime on one target
- **WHEN** live room playback/realtime cannot run on a target
- **THEN** that target is listed incomplete and overall live acceptance fails until resolved or explicitly deferred in the registry with product approval
