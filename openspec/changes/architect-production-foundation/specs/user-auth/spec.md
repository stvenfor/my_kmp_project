## ADDED Requirements

### Requirement: Single reactive session source of truth
Logged-in state observed by UI and gates SHALL come from a reactive `AuthSessionState` (Flow). Imperative facades MAY persist but MUST NOT be a parallel SoT requiring manual epoch sync.

#### Scenario: Login success
- **WHEN** login succeeds
- **THEN** subscribers of AuthSessionState observe logged-in without requiring a separate `sessionEpoch` bump as the primary mechanism
