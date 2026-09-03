# User Auth Specification

## Purpose

Defines authentication and session behavior needed for gated tabs, API authorization, and forced logout across Android, iOS, and HarmonyOS.

## Requirements

### Requirement: Login and registration flows
The system SHALL provide password and OTP login plus registration entry points reachable from the auth gate and settings.

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
