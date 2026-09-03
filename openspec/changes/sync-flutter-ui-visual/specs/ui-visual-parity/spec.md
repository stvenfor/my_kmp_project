## Purpose

Defines the Flutter→KMP visual parity contract: resource sync from the Flutter SoT, pixel-level UI alignment for product surfaces, and evidence rules that reject skeleton/placeholder chrome as “done.”

## ADDED Requirements

### Requirement: Flutter asset inventory and sync
The system SHALL maintain an inventory mapping Flutter module assets (under `my_ai_project` feature/commons packages) to Compose Multiplatform resources, and MUST sync all in-scope product assets required by shipped screens into shared or platform resource trees before marking those screens visually complete.

#### Scenario: Missing product icon is a fail
- **WHEN** a Home or Mine entry that uses a Flutter asset is rendered in KMP
- **THEN** the corresponding synced drawable/image resource is shown (not a single-character or solid-color stand-in)

### Requirement: No skeleton UI as acceptance
The system MUST NOT treat Material-default cards, monochrome text lists, or letter-tile icons as visual Pass when the Flutter SoT uses branded imagery, multi-row cells, or structured chrome for the same route.

#### Scenario: Community feed visual bar
- **WHEN** Community feed is accepted under this change
- **THEN** row structure, spacing, typography, and media placeholders match Flutter `module_community` within the pixel SOP, not a generic card list

### Requirement: Pixel evidence pairs
Visual Pass for a surface SHALL include Flutter and KMP screenshots for the same route/state under the project pixel SOP, stored under this change’s evidence notes.

#### Scenario: Home root accept
- **WHEN** Home root is marked Pass for a target
- **THEN** evidence includes paired Flutter and KMP captures for that Home root state

### Requirement: Design token alignment
Shared design tokens (accent, background, surface, separator, text roles, key radii and type scales) SHALL match Flutter `commons/ui` AppTheme roles used by the product screens in scope.

#### Scenario: Accent drift rejected
- **WHEN** a screen uses a primary accent for interactive chrome
- **THEN** the color matches the Flutter AppTheme accent role for that surface (no demo-only palette drift)
