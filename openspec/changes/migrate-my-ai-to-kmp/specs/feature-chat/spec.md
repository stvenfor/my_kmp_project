## Purpose

Defines Chat tab behavior and an IM engine abstraction so the product can run with a mock engine first and swap real SDKs later without rewriting UI.

## ADDED Requirements

### Requirement: Chat list and detail
The system SHALL provide a chat conversation list and a conversation detail screen after authentication.

#### Scenario: Open conversation
- **WHEN** an authenticated user opens a conversation from the list
- **THEN** the detail screen shows that conversation’s messages and allows returning to the list

### Requirement: Pluggable IM engine
The system SHALL access messaging through an IM engine interface; a mock engine MUST be available for development and CI.

#### Scenario: Mock engine boot
- **WHEN** the app runs with the mock IM engine configured
- **THEN** chat list/detail remain usable without a third-party IM SDK

### Requirement: Auth-gated Chat tab
The Chat tab root SHALL be unreachable as guest content (see app-shell soft gate).

#### Scenario: Guest blocked
- **WHEN** a guest attempts to enter Chat
- **THEN** login is required before chat list content is shown
