## Purpose

Raises authentication from demo/local stubs to Flutter-equivalent login, OTP, register, and session behavior with real network-backed credentials on all targets.

## MODIFIED Requirements

### Requirement: Login and registration flows
The system SHALL provide password and OTP login plus registration entry points that call the same backend contracts as Flutter `module_auth` / account services, with UI matching Flutter auth screens at pixel level.

#### Scenario: Successful password login
- **WHEN** the user submits valid credentials
- **THEN** an authenticated session is established and the pending destination (if any) is resumed

### Requirement: Session persistence
The system SHALL persist session tokens securely enough for process restart on each target and restore them on next launch.

#### Scenario: Relaunch stays signed in
- **WHEN** a user with a valid session kills and relaunches the app
- **THEN** the session remains authenticated without requiring login again

### Requirement: Unauthorized and force-logout handling
The system SHALL clear the local session and return the user to a safe guest state when the backend signals unauthorized or forced logout.

#### Scenario: Token rejected
- **WHEN** an API response indicates the session is invalid or logged in elsewhere
- **THEN** the local session is cleared and protected tabs again require login

### Requirement: Auth header attachment
Authenticated requests SHALL include the current session credentials via the shared network façade on all targets.

#### Scenario: Authenticated request
- **WHEN** an authenticated user calls a protected API
- **THEN** the request carries the session token through the platform HTTP stack

## ADDED Requirements

### Requirement: No demo-only auth as complete
Demo or hard-coded credential shortcuts MUST NOT count as acceptance-complete; production-like remote auth MUST succeed on Android, iOS, and HarmonyOS.

#### Scenario: Stub login rejected
- **WHEN** login only flips local state without backend validation used by Flutter
- **THEN** auth capability remains incomplete

### Requirement: Third-party login channels
Where Flutter exposes WeChat (or other) login channels, KMP SHALL provide the same channels on each target or list missing targets in the platform gap registry as incomplete.

#### Scenario: WeChat login missing on a target
- **WHEN** WeChat login is unavailable on one target while Flutter offers it
- **THEN** that target is incomplete for auth social login and enumerated in the gap registry
