## Purpose

Defines Community tab feed, publish, and media preview behaviors migrated from Flutter `module_community` with three-platform consistency.

## ADDED Requirements

### Requirement: Community feed
The system SHALL show a community feed for authenticated users with post list browsing and pull-to-refresh or equivalent reload.

#### Scenario: Load feed
- **WHEN** an authenticated user opens the Community tab
- **THEN** the feed loads and displays posts or an empty state

### Requirement: Publish flow
The system SHALL allow authenticated users to open a publish flow to create a community post subject to backend validation.

#### Scenario: Open publish
- **WHEN** the user taps publish from Community
- **THEN** the publish UI opens and cancel returns to the feed

### Requirement: Media preview
The system SHALL support image preview and video playback entry points for community media items.

#### Scenario: Preview image
- **WHEN** the user opens an image attachment from a post
- **THEN** a full-screen or dedicated preview is shown and dismiss returns to the previous screen

### Requirement: Auth-gated Community tab
Community root content SHALL require authentication.

#### Scenario: Guest blocked
- **WHEN** a guest selects Community
- **THEN** login is required before feed content is shown
