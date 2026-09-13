## Purpose

Defines cross-platform HTTP client behavior for `:core:network`, including when HarmonyOS uses the shared client stack versus a platform-native transport, and the acceptance bar for retiring the OHOS cinterop path.

## ADDED Requirements

### Requirement: Shared ApiClient façade across targets
The system SHALL expose a single `ApiClient` contract from shared code such that feature and account layers do not branch on transport technology for ordinary business HTTP calls.

#### Scenario: Feature code issues business GET
- **WHEN** shared feature or account code performs a business-path GET through the network façade
- **THEN** the same `ApiClient` API is used on Android, iOS, and OHOS without calling platform-specific transport types

### Requirement: Platform engines remain allowed under one client API
The system SHALL allow each target to select its HTTP engine while still satisfying the shared `ApiClient` contract (including headers, envelope parsing hooks, and token-expiry handling already used by the façade).

#### Scenario: Android and iOS keep native engines
- **WHEN** the CPF-aligned Ktor client line is adopted
- **THEN** Android continues to use an OkHttp-backed client and iOS continues to use a Darwin-backed client unless a later change explicitly decides otherwise

### Requirement: OHOS uses CPF-adapted Ktor with ohosArm64 support
On OHOS arm64 device builds, the system SHALL resolve and link a CPF-adapted Ktor client artifact line that publishes `ohosArm64` variants (not an unsuffixed Central-only `3.3.x` build), and SHALL implement `ApiClient` through that client stack for business HTTP.

#### Scenario: ohosArm64 dependency resolution
- **WHEN** `:core:network` is compiled for `ohosArm64` with the adopted catalog versions
- **THEN** Gradle resolves Ktor client modules that include an `ohosArm64` variant and the module links successfully

#### Scenario: OHOS business call goes through shared client
- **WHEN** an OHOS arm64 app performs `getApi` / `postApi` / form POST against a configured business host
- **THEN** the request is executed by the Ktor-backed `ApiClient` implementation rather than the legacy `net_http` cinterop transport

### Requirement: HTTPS acceptance before removing cinterop
The system MUST NOT remove the OHOS `net_http` cinterop transport until an explicit HTTPS acceptance check passes on a real OHOS arm64 device (or equivalent CI device farm) for at least one production-like HTTPS business URL used by the app.

#### Scenario: HTTPS gate fails
- **WHEN** the chosen OHOS Ktor engine cannot complete a required HTTPS business request with a usable response body
- **THEN** the cinterop transport remains available as the OHOS implementation and Ktor-on-OHOS is not declared complete

#### Scenario: HTTPS gate passes
- **WHEN** HTTPS business requests succeed under the chosen OHOS Ktor engine and parity checks for auth headers / envelope handling pass
- **THEN** the project MAY delete or stop linking the `net_http` cinterop path

### Requirement: Concurrent request capability on OHOS after migration
After Ktor-on-OHOS is accepted, the OHOS client SHALL support overlapping in-flight HTTP requests without a process-wide single-flight mutex that serializes all network calls (unless a documented engine limitation forces a temporary serialize mode that is tracked as tech debt).

#### Scenario: Two overlapping GETs
- **WHEN** two OHOS coroutines issue independent GETs concurrently after migration acceptance
- **THEN** both requests can be in flight without one waiting on a global network mutex solely because of the transport layer

### Requirement: ohosX64 / emulator gap is explicit
If published CPF Ktor artifacts lack `ohosX64` variants, the system SHALL document that OHOS x64 emulator network via Ktor is unsupported (or provide a non-Ktor fallback for that target only) rather than silently breaking the `ohosX64` compile.

#### Scenario: ohosX64 without Ktor variant
- **WHEN** CPF Ktor packages do not publish `ohosX64` klibs
- **THEN** the build either excludes Ktor from `ohosX64` with a documented fallback, or the project drops `ohosX64` from the active matrix for network-dependent modules with an explicit note in module docs
