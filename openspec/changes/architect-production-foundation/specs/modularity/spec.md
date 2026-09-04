## ADDED Requirements

### Requirement: Five-layer dependency direction
The system SHALL organize code into app → feature → component → core → platform shells, and SHALL forbid reverse dependencies (core/component MUST NOT import feature; features MUST NOT import sibling feature internals).

#### Scenario: Feature isolation
- **WHEN** a feature module/package needs shared capability
- **THEN** it depends only on `core` and/or `component` contracts, not another feature's UI or internals

### Requirement: OHOS aggregate modules
Android/iOS MAY use multiple Gradle modules; OHOS builds SHALL still produce a single `libkn.so` via an aggregate/fat link strategy without dropping first-class OHOS targets.

#### Scenario: OHOS link
- **WHEN** shared Kotlin changes are published for Harmony
- **THEN** a single native library continues to load `MainArkUIViewController`
