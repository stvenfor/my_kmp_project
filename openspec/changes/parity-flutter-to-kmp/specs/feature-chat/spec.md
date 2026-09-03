## Purpose

Aligns Chat list/detail and messaging behavior with Flutter `module_chat` and its IM component, including pixel UI and send/receive flows available in source.

## MODIFIED Requirements

### Requirement: Chat list and detail
The system SHALL provide a chat conversation list and conversation detail screen after authentication with UI and states matching Flutter chat pages at pixel level.

#### Scenario: Open conversation
- **WHEN** an authenticated user opens a conversation from the list
- **THEN** the detail screen shows that conversation’s messages and allows returning to the list

### Requirement: Pluggable IM engine
The system SHALL access messaging through an IM engine interface; a mock engine MUST be available for development and CI. Behavior SHALL match Flutter’s current IM component (including mock if Flutter is mock). Replacing mock with a real vendor SDK later SHALL keep UI contracts stable and update the gap registry.

#### Scenario: Mock engine boot
- **WHEN** the app runs with the mock IM engine configured
- **THEN** chat list/detail remain usable without a third-party IM SDK

#### Scenario: Send message
- **WHEN** an authenticated user sends a text message in a conversation that Flutter supports
- **THEN** the message appears in the detail timeline consistent with Flutter engine behavior

### Requirement: Auth-gated Chat tab
The Chat tab root SHALL be unreachable as guest content (see app-shell soft gate).

#### Scenario: Guest blocked
- **WHEN** a guest attempts to enter Chat
- **THEN** login is required before chat list content is shown

## ADDED Requirements

### Requirement: Chat three-platform parity
Chat list/detail visuals and messaging flows SHALL pass acceptance on Android, iOS, and HarmonyOS; any target without a working engine adapter is incomplete.

#### Scenario: Engine missing on OHOS
- **WHEN** chat UI exists but the IM engine cannot run on HarmonyOS
- **THEN** chat is incomplete and HarmonyOS is listed in the gap registry
