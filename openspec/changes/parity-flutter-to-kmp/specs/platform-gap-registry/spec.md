## Purpose

Maintains an explicit inventory of native and product capabilities that are missing, stubbed, or incomplete on Android, iOS, or HarmonyOS so incomplete work is visible and schedulable.

## ADDED Requirements

### Requirement: Gap registry is authoritative
The project SHALL maintain a platform gap registry listing each native or bridge capability with status per Android, iOS, and HarmonyOS (missing, stub, partial, ready) and a short note for follow-up.

#### Scenario: Lookup before claiming done
- **WHEN** a reviewer checks whether pay or scan is complete
- **THEN** the registry shows per-target status and incomplete targets are visible without reading source

### Requirement: Incomplete platforms enumerated
Any capability that is not ready on all three targets SHALL list every incomplete target and the blocking reason (for example missing SDK, unsigned key, or absent N-API binding).

#### Scenario: OHOS pay missing
- **WHEN** WeChat pay is ready on Android/iOS but not on HarmonyOS
- **THEN** the registry enumerates HarmonyOS as incomplete with a follow-up note, and overall pay acceptance remains failed

### Requirement: Registry updates with delivery
When a previously incomplete adapter ships, the registry SHALL be updated in the same change set as the adapter.

#### Scenario: Adapter lands
- **WHEN** a HarmonyOS scan adapter is merged
- **THEN** the registry row for scan on HarmonyOS moves from missing/stub to ready (or partial with remaining notes)
