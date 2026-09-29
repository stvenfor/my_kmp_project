## Purpose

Defines logic-first Flutter→KMP parity acceptance: Shared Presentation Logic and Flutter-equivalent behavior become the hard gate while Screenshot Diff Gate is deferred.

## ADDED Requirements

### Requirement: Logic Acceptance Packet is the hard gate
During the logic-first phase, an in-scope route SHALL be marked complete only when its Logic Acceptance Packet passes. The Android Screenshot Diff Gate MUST NOT be required for that completion mark.

#### Scenario: Logic pass without pixel gate
- **WHEN** a route’s shared presentation logic and main interactions match Flutter at the same mock-or-real level, three platforms can open the main path (including Soft Auth where applicable), and the gap registry is updated
- **THEN** the inventory MAY record `logic-pass` even if Screenshot Diff Gate was not re-run

#### Scenario: Pixel-only work does not count as done
- **WHEN** only screenshots, SoT body bitmaps, or chrome polish change without behavior/logic alignment
- **THEN** the route MUST NOT be marked `logic-pass`

### Requirement: Flutter behavior is the logic SoT
Shared Presentation Logic (UiState/UseCase/mock engines and main interactions) MUST match Flutter `my_ai_project` for the same RoutePath at the same depth Flutter ships (including toast-only entries that MUST remain toast-only). Platform Capability Gaps MUST stay registered and MUST NOT be faked as ready.

#### Scenario: Mock-depth parity
- **WHEN** Flutter implements a feature with seeded mock data and local interactions only
- **THEN** KMP MUST provide equivalent mock depth and interactions without claiming vendor SDK readiness

#### Scenario: Honest payment outcomes
- **WHEN** pay/membership flows run without a real payment SDK
- **THEN** the system MUST NOT report a fake Success; unavailable/sandbox honesty MUST match the FlaggedPayGateway contract

### Requirement: Screenshot Diff Gate is deferred not deleted
Screenshot Diff Gate tooling and prior evidence MUST remain in the repository. Agents MUST NOT treat mse≤2% as a blocking acceptance criterion until the logic-first phase is explicitly ended in inventory/CONTEXT.

#### Scenario: Optional regression only
- **WHEN** an agent implements logic-first work
- **THEN** they MAY skip Screenshot Diff Gate and MUST NOT open new SoT body bitmap work as the primary delivery

### Requirement: Inventory distinguishes logic-pass from packet-pass
The Parity Inventory MUST support `logic-pass` for logic-first completion and MUST retain historical `packet-pass` / pixel evidence without rewriting them as incomplete solely because the gate is deferred.

#### Scenario: Status coexistence
- **WHEN** a route previously had `packet-pass` under the pixel gate
- **THEN** that historical status MAY remain, and further work in this phase SHALL target `logic-pass` gaps instead of re-polishing pixels
