## Purpose

Defines classroom multi-page POC parity with Flutter `module_classroom` (homework, gift, video-related surfaces present in source).

## ADDED Requirements

### Requirement: Classroom journey coverage
The system SHALL expose classroom destinations that cover the Flutter module’s primary pages and navigation graph, not a single placeholder list only.

#### Scenario: Navigate classroom subpages
- **WHEN** the user enters classroom from Home and opens a documented subpage
- **THEN** the subpage content and back stack match Flutter route behavior

### Requirement: Three-platform classroom parity
Classroom UI SHALL pass pixel and behavior acceptance on Android, iOS, and HarmonyOS for implemented pages; any page left stub MUST be incomplete in acceptance.

#### Scenario: Stub page rejected
- **WHEN** a classroom page still shows placeholder-only content while Flutter has real UI
- **THEN** that page fails acceptance
