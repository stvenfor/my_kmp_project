## Purpose

Aligns Community feed, publish, and media preview with Flutter `module_community` at business and pixel parity.

## MODIFIED Requirements

### Requirement: Community feed
The system SHALL show a community feed for authenticated users with post list browsing and pull-to-refresh or equivalent reload, matching Flutter feed layout at pixel level and using real feed data contracts where Flutter does.

#### Scenario: Load feed
- **WHEN** an authenticated user opens the Community tab
- **THEN** the feed loads and displays posts or an empty state consistent with Flutter

### Requirement: Publish flow
The system SHALL allow authenticated users to complete a publish flow to create a community post subject to backend validation, matching Flutter publish UI and outcomes (not a labeled stub).

#### Scenario: Open publish
- **WHEN** the user taps publish from Community
- **THEN** the publish UI opens, supports required Flutter fields/actions, and cancel returns to the feed

### Requirement: Media preview
The system SHALL support image preview and video playback entry points for community media items with real preview hosts.

#### Scenario: Preview image
- **WHEN** the user opens an image attachment from a post
- **THEN** a full-screen or dedicated preview is shown and dismiss returns to the previous screen

### Requirement: Auth-gated Community tab
Community root content SHALL require authentication.

#### Scenario: Guest blocked
- **WHEN** a guest selects Community
- **THEN** login is required before feed content is shown
