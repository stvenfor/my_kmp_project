## Purpose

Defines a shared UUID generation and string round-trip API for Android, iOS, and OHOS arm64 so features use one cross-platform contract instead of platform-specific UUID APIs.

## ADDED Requirements

### Requirement: Shared UUID API for feature and core code
Feature and core code that needs a new UUID or to parse a UUID string SHALL use only the shared UUID facade in the common core layer. Call sites MUST NOT import the underlying UUID library types directly (except inside the facade implementation).

#### Scenario: Generate via facade
- **WHEN** shared code needs a new random UUID string
- **THEN** it calls the shared facade API and receives a non-blank canonical UUID string

#### Scenario: Parse via facade
- **WHEN** shared code needs to validate or normalize a UUID string
- **THEN** it calls the shared facade parse/round-trip API rather than a platform-specific UUID type

### Requirement: Canonical string form
Generated UUID strings SHALL be lowercase hexadecimal in the standard 8-4-4-4-12 form (36 characters including hyphens). Parsing a string produced by the facade MUST succeed and round-trip to an equal string.

#### Scenario: Round-trip
- **WHEN** a UUID is generated and then parsed from its string form
- **THEN** the parsed value's string form equals the original string

#### Scenario: Two random values differ
- **WHEN** two UUIDs are generated in succession
- **THEN** their string forms are not equal

### Requirement: Android, iOS, and OHOS arm64 resolve CPF UUID
On Android, iOS, and OHOS arm64 builds, the project SHALL resolve and link the CPF-adapted `com.benasher44:uuid` artifact line that publishes those platform variants, at the catalog-pinned version.

#### Scenario: Dependency resolution on primary targets
- **WHEN** `:composeApp` is compiled for Android, an iOS target, or `ohosArm64` with the adopted catalog version
- **THEN** Gradle resolves `com.benasher44:uuid` variants for that target and the module compiles successfully

### Requirement: Unsupported OHOS x64 does not block primary targets
If the adopted UUID line does not publish a usable `ohosX64` variant, `ohosX64` builds SHALL either keep compiling with a documented stub/fallback behind the same facade, or remain out of the UUID dependency graph without breaking Android / iOS / `ohosArm64` acceptance.

#### Scenario: Primary targets unaffected by missing ohosX64 klib
- **WHEN** Maven has no `uuid-ohosx64` artifact for the pinned version
- **THEN** Android, iOS, and `ohosArm64` builds and facade smoke tests still succeed

### Requirement: Smoke tests cover facade behavior
The project SHALL include shared unit tests that exercise generate, format shape, round-trip, and uniqueness of successive random UUIDs through the shared facade.

#### Scenario: commonTest smoke
- **WHEN** shared unit tests for the UUID facade are run on the Android unit-test task
- **THEN** generate/format/round-trip/uniqueness assertions pass
