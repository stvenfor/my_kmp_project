## Purpose

Defines friend-list product surface parity with Flutter `module_friend`.

## ADDED Requirements

### Requirement: Friend list surface
The system SHALL provide a friend list destination reachable from the same product entries as Flutter, showing the same empty/loading/error/content states available in the Flutter module.

#### Scenario: Open friends from Home
- **WHEN** the user opens the friends entry from Home
- **THEN** the friend list screen matching Flutter IA is shown and back returns to the prior screen

### Requirement: Three-platform friend parity
Friend list UI and navigation SHALL pass pixel and behavior acceptance on Android, iOS, and HarmonyOS.

#### Scenario: Parity check
- **WHEN** the same friend route is opened on each target
- **THEN** layout and primary actions match Flutter within pixel tolerance
