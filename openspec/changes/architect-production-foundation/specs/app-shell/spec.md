## ADDED Requirements

### Requirement: Single type-safe navigation graph
The system SHALL route secondary destinations through a shared type-safe route graph (sealed routes + navigator), not ad-hoc per-tab string `when` hubs that import sibling feature screens.

#### Scenario: Secondary open
- **WHEN** the user opens a secondary page from Home or Mine
- **THEN** navigation goes through the shared navigator and bottom-bar visibility follows shell rules

### Requirement: Deep link and push click entry
Cold-start deep links and notification clicks SHALL enter the same navigation graph for Must destinations (tabs and login at minimum).

#### Scenario: Notification click
- **WHEN** a push notification is opened with a known route payload
- **THEN** the app navigates to that route on Android, iOS, and OHOS
