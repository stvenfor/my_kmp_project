## Purpose

Defines cross-platform media playback contracts for short/full video and music experiences migrated from Flutter video/music modules.

## ADDED Requirements

### Requirement: Video playback entry
The system SHALL provide video playback screens for feed and detail use cases with play/pause and exit controls.

#### Scenario: Play short video
- **WHEN** the user opens a short-video item
- **THEN** playback starts or a clear loading/error state is shown, and exiting returns to the previous screen

### Requirement: Music list and now playing
The system SHALL provide a music list and a now-playing experience, including a dismissible mini-player affordance when music is active on Home where applicable.

#### Scenario: Start music
- **WHEN** the user starts a track from the music list
- **THEN** now-playing state is visible and audio continues until paused/stopped

### Requirement: Platform media adapters
Media engines SHALL be provided per platform behind a shared interface so Android, iOS, and HarmonyOS can each use a supported player implementation.

#### Scenario: Target-specific engine
- **WHEN** the same video screen runs on Android, iOS, and HarmonyOS
- **THEN** controls and navigation behave the same even if the underlying player differs
