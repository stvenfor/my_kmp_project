## Purpose

Defines modular development boundaries so features and cores can evolve independently while the app assembles a consistent three-platform product.

## ADDED Requirements

### Requirement: Layered dependency direction
The system SHALL enforce a dependency direction of `app assemble → feature modules → core modules`, and MUST NOT allow feature modules to depend on other features’ internals.

#### Scenario: Feature isolation
- **WHEN** a developer adds a new feature module
- **THEN** it may depend only on published core APIs and shared UI contracts, not sibling feature implementation packages

### Requirement: Shared UI and domain in common code
Feature business UI and domain logic SHALL live in shared multiplatform source sets so Android, iOS, and HarmonyOS consume the same implementation by default.

#### Scenario: Feature change propagates
- **WHEN** a shared feature screen is updated in common code
- **THEN** all three targets receive the same UI/behavior without duplicating the screen in native shells

### Requirement: Platform adapters are thin
Platform-specific code SHALL be limited to expect/actual adapters and thin host shells; hosts MUST NOT contain feature business screens.

#### Scenario: Host remains thin
- **WHEN** a new feature screen is added
- **THEN** `iosApp` and `harmonyApp` only host the shared entry and do not embed a parallel native feature UI
