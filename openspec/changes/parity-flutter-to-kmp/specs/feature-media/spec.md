## Purpose

Replaces stub media players with real platform playback so video and music flows match Flutter modules at behavior and pixel level.

## MODIFIED Requirements

### Requirement: Video playback entry
The system SHALL provide video playback screens for feed and detail use cases with play/pause and exit controls matching Flutter video module visuals and using real decoders on each target.

#### Scenario: Play short video
- **WHEN** the user opens a short-video item
- **THEN** playback starts or a clear loading/error state is shown, and exiting returns to the previous screen

### Requirement: Music list and now playing
The system SHALL provide a music list and a now-playing experience, including a dismissible mini-player affordance when music is active on Home where Flutter provides it.

#### Scenario: Start music
- **WHEN** the user starts a track from the music list
- **THEN** now-playing state is visible and audio continues until paused/stopped using a real audio engine on that target

### Requirement: Platform media adapters
Media engines SHALL be provided per platform behind a shared interface so Android, iOS, and HarmonyOS each use a supported real player implementation; stub-only adapters MUST NOT pass acceptance.

#### Scenario: Target-specific engine
- **WHEN** the same video screen runs on Android, iOS, and HarmonyOS
- **THEN** controls and navigation behave the same and each target uses a non-stub player, or incomplete targets are listed in the gap registry and acceptance fails
